package ru.tinkoff.kora.resilient.validation;

import java.util.List;
import java.util.stream.Collectors;

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
            if(i + 1 != violations.size()) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }
}
