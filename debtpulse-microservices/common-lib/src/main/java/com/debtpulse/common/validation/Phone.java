package com.debtpulse.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a phone number is exactly 10 numeric digits (no spaces, symbols or country code).
 *
 * <p>A {@code null} value is considered valid so the annotation is safe on optional fields;
 * combine it with {@code @NotBlank} where the phone is mandatory. Implemented as a composed
 * constraint over {@link Pattern} so it needs no validator class.</p>
 */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = "^\\d{10}$")
@ReportAsSingleViolation
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Phone {

    String message() default "Phone number must contain exactly 10 digits";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
