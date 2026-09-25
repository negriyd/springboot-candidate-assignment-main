package com.interzero.TestServer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.Repository;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the layering rule: controllers talk to services, never directly to repositories.
 * <p>
 * Fails if any of the application's controllers has a field or constructor parameter whose type is a Spring Data
 * {@link Repository}.
 */
@SpringBootTest
class ControllerLayeringTests {

    private static final String APP_PACKAGE = TestServerApplication.class.getPackageName();

    @Autowired
    private ApplicationContext context;

    @Test
    void controllersDoNotDependOnRepositories() {
        List<Class<?>> controllers = context.getBeansWithAnnotation(RestController.class).values().stream()
                .<Class<?>>map(bean -> ClassUtils.getUserClass(bean))
                .filter(type -> type.getPackageName().startsWith(APP_PACKAGE))
                .toList();

        assertThat(controllers).isNotEmpty();

        List<String> violations = new ArrayList<>();
        for (Class<?> controller : controllers) {
            for (Field field : controller.getDeclaredFields()) {
                if (Repository.class.isAssignableFrom(field.getType())) {
                    violations.add(controller.getSimpleName() + " field " + field.getName());
                }
            }
            for (Constructor<?> constructor : controller.getDeclaredConstructors()) {
                Arrays.stream(constructor.getParameterTypes())
                        .filter(Repository.class::isAssignableFrom)
                        .forEach(type -> violations.add(
                                controller.getSimpleName() + " constructor parameter " + type.getSimpleName()));
            }
        }
        assertThat(violations)
                .as("Controllers that depend on repositories directly (use a service instead)")
                .isEmpty();
    }
}
