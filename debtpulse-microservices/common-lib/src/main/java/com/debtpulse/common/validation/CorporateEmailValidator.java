package com.debtpulse.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Backing validator for {@link CorporateEmail}. Checks basic RFC-style email shape first, then
 * confirms the domain is one of the approved corporate domains, emitting a specific message for
 * each failure so a frontend can surface a precise reason.
 */
public class CorporateEmailValidator implements ConstraintValidator<CorporateEmail, String> {

    // Deliberately pragmatic (not full RFC 5322): local-part @ domain . tld
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private String[] domains;

    @Override
    public void initialize(CorporateEmail annotation) {
        this.domains = Arrays.stream(annotation.domains())
                .map(d -> d.toLowerCase(Locale.ROOT))
                .toArray(String[]::new);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // presence is enforced separately by @NotBlank
        }
        if (!EMAIL.matcher(value).matches()) {
            return fail(context, "Invalid email format");
        }
        String domain = value.substring(value.indexOf('@') + 1).toLowerCase(Locale.ROOT);
        for (String allowed : domains) {
            if (domain.equals(allowed)) {
                return true;
            }
        }
        return fail(context, "Email domain must be one of: " + String.join(", ", domains));
    }

    private boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
