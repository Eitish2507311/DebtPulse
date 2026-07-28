package com.debtpulse.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enforces a strong password policy: at least {@link #minLength()} characters (default 8) and
 * containing at least one uppercase letter, one lowercase letter, one digit and one special
 * (non-alphanumeric, non-whitespace) character.
 *
 * <p>A {@code null} value is treated as valid so the required-ness stays the responsibility of
 * {@code @NotBlank}; this keeps the two concerns (presence vs. strength) independent.</p>
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Password must be at least 8 characters and include an uppercase letter, "
            + "a lowercase letter, a digit and a special character";

    int minLength() default 8;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
