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
        final StringBuilder builder = new StringBuilder("Validation failed due to violations:\n");
        for (int i = 0; i < violations.size(); i++) {
            builder.append(i + 1).append(") ").append(violations.get(i)).append(';');
            if (i + 1 != violations.size()) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }
}
