package com.debtpulse.common.id;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;

import java.lang.reflect.Member;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Year;
import java.util.EnumSet;

/**
 * Hibernate identifier generator behind {@link BusinessId}. Produces {@code PREFIX-YEAR-NNNNNN}
 * ids (6-digit, zero-padded, sequence reset per calendar year) using a single atomic MySQL
 * statement so concurrent inserts across instances never collide:
 *
 * <pre>
 *   INSERT INTO id_sequence (seq_key, seq_year, next_val) VALUES (?, ?, LAST_INSERT_ID(1))
 *   ON DUPLICATE KEY UPDATE next_val = LAST_INSERT_ID(next_val + 1);
 *   SELECT LAST_INSERT_ID();
 * </pre>
 *
 * <p>The upsert takes a row lock on the {@code (seq_key, seq_year)} row, so parallel transactions
 * serialize on it and each gets a distinct value. The work runs on the current session's JDBC
 * connection (same transaction as the insert), so a rolled-back insert also rolls back the
 * sequence bump — gaps are possible but never duplicates.</p>
 */
public class BusinessIdGenerator implements BeforeExecutionGenerator {

    private static final String UPSERT =
            "INSERT INTO id_sequence (seq_key, seq_year, next_val) VALUES (?, ?, LAST_INSERT_ID(1)) "
                    + "ON DUPLICATE KEY UPDATE next_val = LAST_INSERT_ID(next_val + 1)";
    private static final String SELECT_LAST = "SELECT LAST_INSERT_ID()";

    private final String prefix;

    // Constructor signature required by @IdGeneratorType.
    public BusinessIdGenerator(BusinessId config, Member member, CustomIdGeneratorCreationContext context) {
        this.prefix = config.prefix();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EnumSet.of(EventType.INSERT);
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner,
                           Object currentValue, EventType eventType) {
        // Preserve an explicitly assigned id (seed data, imports, tests).
        if (currentValue instanceof String s && !s.isBlank()) {
            return s;
        }
        int year = Year.now().getValue();
        long seq = session.doReturningWork(connection -> {
            try (PreparedStatement upsert = connection.prepareStatement(UPSERT)) {
                upsert.setString(1, prefix);
                upsert.setInt(2, year);
                upsert.executeUpdate();
            }
            try (PreparedStatement select = connection.prepareStatement(SELECT_LAST);
                 ResultSet rs = select.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        });
        return String.format("%s-%d-%06d", prefix, year, seq);
    }
}
