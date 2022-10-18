package ru.tinkoff.kora.resilient.validation;

/**
 * Please add Description Here.
 */
public interface LengthConstraintFactory<T> extends ConstraintFactory<T, LengthConstraint<T>> {
    LengthConstraint<T> create(Integer from, Integer to);
}
