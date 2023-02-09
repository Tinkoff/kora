package ru.tinkoff.kora.validation.annotation.processor;

import org.junit.jupiter.api.Assertions;
import ru.tinkoff.kora.annotation.processor.common.TestUtils;
import ru.tinkoff.kora.aop.annotation.processor.AopAnnotationProcessor;
import ru.tinkoff.kora.application.graph.TypeRef;
import ru.tinkoff.kora.validation.annotation.processor.testdata.*;
import ru.tinkoff.kora.validation.common.Validator;
import ru.tinkoff.kora.validation.common.constraint.ValidationModule;

import java.util.List;

public abstract class ValidateRunner extends Assertions implements ValidationModule {

    private static ClassLoader classLoader = null;

    protected ValidateSync getValidateSync() {
        try {
            final ClassLoader classLoader = getClassLoader();
            final Class<?> clazz = classLoader.loadClass("ru.tinkoff.kora.validation.annotation.processor.testdata.$Foo_Validator");
            return (ValidateSync) clazz.getConstructors()[0].newInstance(notEmptyStringConstraintFactory(),
                patternStringConstraintFactory(),
                getTazValidator());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected Validator<ValidBar> getMonoValid() {
        try {
            final ClassLoader classLoader = getClassLoader();
            final Class<?> clazz = classLoader.loadClass("ru.tinkoff.kora.validation.annotation.processor.testdata.$Bar_Validator");
            return (Validator<ValidBar>) clazz.getConstructors()[0].newInstance(
                sizeListConstraintFactory(TypeRef.of(Integer.class)),
                getTazValidator());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected Validator<ValidBar> getFluxValid() {
        try {
            final ClassLoader classLoader = getClassLoader();
            final Class<?> clazz = classLoader.loadClass("ru.tinkoff.kora.validation.annotation.processor.testdata.$Bar_Validator");
            return (Validator<ValidBar>) clazz.getConstructors()[0].newInstance(
                sizeListConstraintFactory(TypeRef.of(Integer.class)),
                getTazValidator());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected Validator<ValidTaz> getTazValidator() {
        try {
            final ClassLoader classLoader = getClassLoader();
            final Class<?> clazz = classLoader.loadClass("ru.tinkoff.kora.validation.annotation.processor.testdata.$ValidatedTaz_Validator");
            return (Validator<ValidTaz>) clazz.getConstructors()[0].newInstance(patternStringConstraintFactory());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ClassLoader getClassLoader() {
        try {
            if (classLoader == null) {
                final List<Class<?>> classes = List.of(ValidTaz.class, ValidateFlux.class, ValidateMono.class, ValidateSync.class);
                classLoader = TestUtils.annotationProcess(classes, new ValidAnnotationProcessor(), new AopAnnotationProcessor());
            }

            return classLoader;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
