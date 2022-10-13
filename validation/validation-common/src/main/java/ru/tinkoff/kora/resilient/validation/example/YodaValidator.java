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
public class YodaValidator implements FieldValidator<Yoda> {

    // generated constraint declaration
    private NotNullConstraint constraint1 = null;
    private NotEmptyConstraint constraint2 = null;
    private NotEmptyConstraint constraint3 = null;

    // generated validator declaration
    private FieldValidator<Baby> validator1 = null;

    @NotNull
    @Override
    public List<Violation> validate(Field<Yoda> field) {
        // generated field declaration
        var f1 = Field.of(field.value().id(), "id", field.name());
        var f2 = Field.of(field.value().codes(), "codes", field.name());
        var f3 = Field.of(field.value().babies(), "babies", field.name());

        // generated constraint validation declaration
        final List<Violation> violations = new ArrayList<>();
        checkConstraint(constraint1, f1, violations);
        checkConstraint(constraint2, f1, violations);
        checkConstraint(constraint3, f2, violations);

        // generated inner field validation declaration
        if(f3.value() != null) {
            for (int i = 0; i < f3.value().size(); i++) {
                var v = Field.of(f3.value().get(i), "[0]", f3.name());
                violations.addAll(validator1.validate(v));
            }
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
