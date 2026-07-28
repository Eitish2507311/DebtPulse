package com.debtpulse.notification.repository;

import com.debtpulse.common.enums.NotifStatus;
import com.debtpulse.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * Notification persistence. Uses {@link PagingAndSortingRepository} + {@link CrudRepository}
 * for CRUD, paging and derived finders, plus {@link JpaSpecificationExecutor} so the
 * current-user list endpoint can apply dynamic, optional Specification-based filters.
 */
public interface NotificationRepository extends PagingAndSortingRepository<Notification, String>,
        CrudRepository<Notification, String>, JpaSpecificationExecutor<Notification> {

    Page<Notification> findByUserId(String userId, Pageable pageable);

    long countByUserIdAndStatus(String userId, NotifStatus status);

    List<Notification> findByUserIdAndStatus(String userId, NotifStatus status);
}
