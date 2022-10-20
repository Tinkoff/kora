package ru.tinkoff.kora.validation.example;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.*;
import ru.tinkoff.kora.validation.constraint.NotNullConstraint;
import ru.tinkoff.kora.validation.constraint.factory.NotEmptyConstraintFactory;
import ru.tinkoff.kora.validation.constraint.factory.NotNullConstraintFactory;
import ru.tinkoff.kora.validation.constraint.factory.RangeConstraintFactory;

import javax.annotation.processing.Generated;
import java.util.ArrayList;
import java.util.List;

/**
 * Please add Description Here.
 */
@Generated("blabla")
public class BabyValidator implements FieldValidator<Baby> {

    // generated constraint declaration
    private final Constraint<String> constraint1;
    private final Constraint<String> constraint2;
    private final Constraint<Long> constraint3;

    // generated validator declaration
    private FieldValidator<Yoda> validator1;

    public BabyValidator(NotNullConstraintFactory<String> constraint1,
                         NotEmptyConstraintFactory<String> constraint2,
                         RangeConstraintFactory<Long> constraint3) {
        this.constraint1 = constraint1.create();
        this.constraint2 = constraint2.create();
        this.constraint3 = constraint3.create(1L, 5L);
    }

    public void setValidator1(FieldValidator<Yoda> validator1) {
        this.validator1 = validator1;
    }

    @NotNull
    @Override
    public List<Violation> validate(Field<Baby> field, Options options) {
        // generated field declaration
        var f1 = Field.of(field.value().number(), "number", field.name());
        var f2 = Field.of(field.value().code(), "code", field.name());
        var f3 = Field.of(field.value().yoda(), "yoda", field.name());

        // generated constraint validation declaration
        final List<Violation> violations = new ArrayList<>();
        checkConstraint(constraint1, f1, f1.value(), violations);
        checkConstraint(constraint2, f1, f1.value(), violations);
        checkConstraint(constraint3, f2, f2.value(), violations);

        // generated inner field validation declaration
        if(f3.value() != null) {
            violations.addAll(validator1.validate(f3, options));
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
