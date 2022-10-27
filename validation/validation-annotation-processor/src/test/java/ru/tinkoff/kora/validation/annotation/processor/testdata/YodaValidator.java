package ru.tinkoff.kora.validation.annotation.processor.testdata;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.*;
import ru.tinkoff.kora.validation.constraint.factory.NotEmptyConstraintFactory;
import ru.tinkoff.kora.validation.constraint.factory.NotNullConstraintFactory;

import javax.annotation.processing.Generated;
import java.util.ArrayList;
import java.util.List;

/**
 * Please add Description Here.
 */
@Generated("blabla")
public final class YodaValidator implements FieldValidator<Yoda> {

    // generated constraint declaration
    private final Constraint<String> constraint1;
    private final Constraint<String> constraint2;
    private final Constraint<List<Baby>> constraint3;

    // generated validator declaration
    private FieldValidator<List<Baby>> validator1;
//    private ListFieldValidator<Baby> validator1;

    public YodaValidator(NotNullConstraintFactory<String> constraint1,
                         NotEmptyConstraintFactory<String> constraint2,
                         NotEmptyConstraintFactory<List<Baby>> constraint3) {
        this.constraint1 = constraint1.create();
        this.constraint2 = constraint2.create();
        this.constraint3 = constraint3.create();
    }

    @NotNull
    @Override
    public List<Violation> validate(@NotNull Field<Yoda> field, @NotNull ValidationOptions options) {
        // generated field declaration
        var f1 = Field.of(field.value().id(), "id", field.name());
        var f2 = Field.of(field.value().codes(), "codes", field.name());
        var f3 = Field.of(field.value().babies(), "babies", field.name());

        // generated constraint validation declaration
        final List<Violation> violations = new ArrayList<>();
        checkConstraint(constraint1, f1, f1.value(), violations);
        checkConstraint(constraint2, f1, f1.value(), violations);
        checkConstraint(constraint3, f3, f3.value(), violations);

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
                violations.add(violation);
            }
        } catch (Exception e) {
            violations.add(Violation.of(e.getMessage(), field.name()));
        }
    }
}
