package org.rsinitsyn.quiz.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PhotoUrlValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhotoUrlValid {
    String message() default "Photo URL must contain extension .png, .jpg";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
