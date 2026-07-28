package com.debtpulse.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a value is a well-formed email address whose domain is one of the approved
 * corporate domains (default: {@code dp.com}).
 *
 * <p>A {@code null} value is treated as valid so presence stays the responsibility of
 * {@code @NotBlank}. The accepted domain list is configurable per usage via {@link #domains()},
 * so the rule is reusable rather than hard-wired to a single environment.</p>
 */
@Documented
@Constraint(validatedBy = CorporateEmailValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CorporateEmail {

    String message() default "Email must be a valid address on an approved company domain";

    /** Allowed domains (case-insensitive), without the leading '@'. */
    String[] domains() default {"dp.com"};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
