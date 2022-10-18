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
public class YodaValidator implements FieldValidator<Yoda> {

    // generated constraint declaration
    private final NotNullConstraint constraint1;
    private final NotEmptyConstraint<String> constraint2;
    private final NotEmptyConstraint<Iterable> constraint3;

    // generated validator declaration
    private FieldValidator<Baby> validator1;

    public YodaValidator(NotNullConstraint constraint1, NotEmptyConstraint<String> constraint2, NotEmptyConstraint<Iterable> constraint3) {
        this.constraint1 = constraint1;
        this.constraint2 = constraint2;
        this.constraint3 = constraint3;
    }

    public void setValidator1(FieldValidator<Baby> validator1) {
        this.validator1 = validator1;
    }

    @NotNull
    @Override
    public List<Violation> validate(Field<Yoda> field, Options options) {
        // generated field declaration
        var f1 = Field.of(field.value().id(), "id", field.name());
        var f2 = Field.of(field.value().codes(), "codes", field.name());
        var f3 = Field.of(field.value().babies(), "babies", field.name());

        // generated constraint validation declaration
        final List<Violation> violations = new ArrayList<>();
        checkConstraint(constraint1, f1, f1.value(), violations);
        checkConstraint(constraint2, f1, f1.value(), violations);
        checkConstraint(constraint3, f2, f2.value(), violations);

        // generated inner field validation declaration
        if(f3.value() != null) {
            for (int i = 0; i < f3.value().size(); i++) {
                var v = Field.of(f3.value().get(i), "[" + i + "]", f3.name());
                violations.addAll(validator1.validate(v, options));
            }
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
