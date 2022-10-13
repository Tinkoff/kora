package ru.tinkoff.kora.resilient.validation.example;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.resilient.validation.*;
import ru.tinkoff.kora.resilient.validation.constraint.NotEmptyConstraint;
import ru.tinkoff.kora.resilient.validation.constraint.NotNullConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * Please add Description Here.
 */
public class BabyValidator implements FieldValidator<Baby> {

    // generated constraint declaration
    private NotNullConstraint constraint1 = null;
    private NotEmptyConstraint constraint2 = null;

    // generated validator declaration
    private FieldValidator<Yoda> validator1 = null;

    @NotNull
    @Override
    public List<Violation> validate(Field<Baby> field) {
        // generated field declaration
        var f1 = Field.of(field.value().number(), "number", field.name());
        var f2 = Field.of(field.value().yoda(), "yoda", field.name());

        // generated constraint validation declaration
        final List<Violation> violations = new ArrayList<>();
        checkConstraint(constraint1, f1, violations);
        checkConstraint(constraint2, f1, violations);

        // generated inner field validation declaration
        if(f2.value() != null) {
            violations.addAll(validator1.validate(f2));
        }

        return violations;
    }

    // generated
    private static void checkConstraint(Constraint constraint, Field<?> field, List<Violation> violations) {
        try {
            Violation violation = constraint.validate(field.value());
            if(violation != null) {
                violations.add(violation);
            }
        } catch (Exception e) {
            violations.add(Violation.of(e.getMessage(), field.name()));
        }
    }
}
