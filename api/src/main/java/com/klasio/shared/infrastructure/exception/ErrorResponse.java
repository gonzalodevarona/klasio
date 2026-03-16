package com.klasio.shared.infrastructure.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(ErrorDetail error) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorDetail(
            String code,
            String message,
            List<FieldError> details,
            String suggestedSlug
    ) {
        public ErrorDetail(String code, String message) {
            this(code, message, null, null);
        }

        public ErrorDetail(String code, String message, List<FieldError> details) {
            this(code, message, details, null);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FieldError(String field, String message) {
    }
}
