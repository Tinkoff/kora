package ru.tinkoff.kora.resilient.validation.example;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.resilient.validation.*;
import ru.tinkoff.kora.resilient.validation.constraint.NotNullConstraint;

import javax.annotation.processing.Generated;
import java.util.ArrayList;
import java.util.List;

/**
 * Please add Description Here.
 */
@Generated("blabla")
public class BabyValidator implements FieldValidator<Baby> {

    // generated constraint declaration
    private final NotNullConstraint constraint1;
    private final NotEmptyConstraint<String> constraint2;

    // generated validator declaration
    private FieldValidator<Yoda> validator1;

    public BabyValidator(NotNullConstraint constraint1, NotEmptyConstraint<String> constraint2) {
        this.constraint1 = constraint1;
        this.constraint2 = constraint2;
    }

    public void setValidator1(FieldValidator<Yoda> validator1) {
        this.validator1 = validator1;
    }

    @NotNull
    @Override
    public List<Violation> validate(Field<Baby> field, Options options) {
        // generated field declaration
        var f1 = Field.of(field.value().number(), "number", field.name());
        var f2 = Field.of(field.value().yoda(), "yoda", field.name());

        // generated constraint validation declaration
        final List<Violation> violations = new ArrayList<>();
        checkConstraint(constraint1, f1, f1.value(), violations);
        checkConstraint(constraint2, f1, f1.value(), violations);

        // generated inner field validation declaration
        if(f2.value() != null) {
            violations.addAll(validator1.validate(f2, options));
        }

        return violations;
    }

    // generated
    private static <T> void checkConstraint(Constraint<T> constraint, Field<?> field, T t, List<Violation> violations) {
        try {
            Violation violation = constraint.validate(t);
            if(violation != null) {
                violations.add(Violation.of(violation.message(), field.name()));
            }
        } catch (Exception e) {
            violations.add(Violation.of(e.getMessage(), field.name()));
        }
    }
}
