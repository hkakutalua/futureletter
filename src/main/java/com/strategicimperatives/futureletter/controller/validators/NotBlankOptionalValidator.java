package com.strategicimperatives.futureletter.controller.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotBlankOptionalValidator implements ConstraintValidator<NotBlankOptional, String> {
    @Override
    public void initialize(NotBlankOptional constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        } else {
            return !value.isBlank();
        }
    }
}
