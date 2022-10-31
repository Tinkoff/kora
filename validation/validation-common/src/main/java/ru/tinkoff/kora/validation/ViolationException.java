package ru.tinkoff.kora.validation;

import java.util.List;

/**
 * Please add Description Here.
 */
public class ViolationException extends RuntimeException {

    public ViolationException(List<Violation> violations) {
        super(buildViolationMessage(violations));
    }

    private static String buildViolationMessage(List<Violation> violations) {
        final StringBuilder builder = new StringBuilder("Validation failed with violations:\n");
        for (int i = 1; i <= violations.size(); i++) {
            final Violation violation = violations.get(i);
            builder.append(i)
                .append(") Path '")
                .append(violation.path().full())
                .append("' violation: ")
                .append(violation.message())
                .append(';');

            if (i != violations.size()) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }
}
