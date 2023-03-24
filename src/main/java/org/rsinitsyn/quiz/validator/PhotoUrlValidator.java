package org.rsinitsyn.quiz.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

public class PhotoUrlValidator implements ConstraintValidator<PhotoUrlValid, String> {

    @Override
    public void initialize(PhotoUrlValid constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return Boolean.TRUE;
        }
        return !FilenameUtils.getExtension(value).isEmpty();
    }
}
