package com.interzero.TestServer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.Repository;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the layering rules:
 * <ul>
 *     <li>controllers talk to services, never directly to repositories;</li>
 *     <li>endpoints return and accept DTOs, never JPA entities, so the API is independent of the database model.</li>
 * </ul>
 */
@SpringBootTest
class ControllerLayeringTests {

    private static final String APP_PACKAGE = TestServerApplication.class.getPackageName();

    private static final String ENTITY_PACKAGE = APP_PACKAGE + ".entity";

    @Autowired
    private ApplicationContext context;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

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

    @Test
    void endpointsDoNotExposeEntities() {
        List<String> violations = new ArrayList<>();
        handlerMapping.getHandlerMethods().values().stream()
                .filter(handler -> handler.getBeanType().getPackageName().startsWith(APP_PACKAGE))
                .forEach(handler -> {
                    Method method = handler.getMethod();
                    // Generic type names include type arguments, e.g. ResponseEntity<...entity.Pet>.
                    if (method.getGenericReturnType().getTypeName().contains(ENTITY_PACKAGE + ".")) {
                        violations.add(handler + " returns " + method.getGenericReturnType().getTypeName());
                    }
                    for (Type parameter : method.getGenericParameterTypes()) {
                        if (parameter.getTypeName().contains(ENTITY_PACKAGE + ".")) {
                            violations.add(handler + " accepts " + parameter.getTypeName());
                        }
                    }
                });
        assertThat(violations)
                .as("Endpoints that return or accept JPA entities (use a DTO instead)")
                .isEmpty();
    }
}
