package com.strategicimperatives.futureletter.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/** @noinspection unchecked, ConstantConditions, ConstantConditions, ConstantConditions */
class GlobalExceptionHandlerTest {
    public static final Path PROPERTY_PATH = new Path() {
        @NotNull
        @Override
        public Iterator<Node> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public String toString() {
            return "field";
        }
    };
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    public void testHandleMethodArgumentNotValid() {
        MethodParameter methodParameter = Mockito.mock(MethodParameter.class);
        DirectFieldBindingResult bindingResult = new DirectFieldBindingResult(null, "obj");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);
        ex.getBindingResult().addError(new FieldError("object", "field", "error message"));

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(ex, null);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("error"), equalTo("Validation Failed"));

        List<Map<String, String>> fields = (List<Map<String, String>>) body.get("fields");
        assertThat(fields.get(0).get("name"), equalTo("field"));
        assertThat(fields.get(0).get("message"), equalTo("error message"));
    }

    @Test
    public void testHandleBindException() {
        BindException ex = new BindException(new Object(), "object");
        ex.addError(new FieldError("object", "field", "error message"));

        ResponseEntity<Object> response = handler.handleBindException(ex, null);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("error"), equalTo("Validation Failed"));

        List<Map<String, String>> fields = (List<Map<String, String>>) body.get("fields");
        assertThat(fields.get(0).get("name"), equalTo("field"));
        assertThat(fields.get(0).get("message"), equalTo("error message"));
    }

    @Test
    public void testHandleConstraintViolationException() {
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolation<Object> violation = Mockito.mock(ConstraintViolation.class);
        Mockito.when(violation.getPropertyPath()).thenReturn(PROPERTY_PATH);
        Mockito.when(violation.getMessage()).thenReturn("error message");
        violations.add(violation);

        ConstraintViolationException ex = new ConstraintViolationException(violations);

        ResponseEntity<Object> response = handler.handleConstraintViolationException(ex, null);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("error"), equalTo("Validation Failed"));

        List<Map<String, String>> fields = (List<Map<String, String>>) body.get("fields");
        assertThat(fields.get(0).get("name"), equalTo("field"));
        assertThat(fields.get(0).get("message"), equalTo("error message"));
    }
}