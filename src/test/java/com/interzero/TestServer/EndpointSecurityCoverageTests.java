package com.interzero.TestServer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Modifier;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against endpoints that are accidentally left without role checks.
 * <p>
 * Since authorization is declared with annotations, a controller method without one would be open to any
 * authenticated user. This test fails if any of the application's endpoints is missing a {@link PreAuthorize}
 * (directly or via {@code @CanRead} / {@code @CanWrite}) on the method or its class, or is not public
 * (method security does not apply to non-public methods).
 */
@SpringBootTest
class EndpointSecurityCoverageTests {

    private static final String APP_PACKAGE = TestServerApplication.class.getPackageName();

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void everyEndpointDeclaresAuthorization() {
        List<HandlerMethod> appHandlers = handlerMapping.getHandlerMethods().values().stream()
                .filter(handler -> handler.getBeanType().getPackageName().startsWith(APP_PACKAGE))
                .toList();

        assertThat(appHandlers).isNotEmpty();

        List<String> unprotected = appHandlers.stream()
                .filter(handler -> !AnnotatedElementUtils.hasAnnotation(handler.getMethod(), PreAuthorize.class)
                        && !AnnotatedElementUtils.hasAnnotation(handler.getBeanType(), PreAuthorize.class))
                .map(HandlerMethod::toString)
                .toList();
        assertThat(unprotected)
                .as("Endpoints without @PreAuthorize, @CanRead or @CanWrite")
                .isEmpty();

        List<String> nonPublic = appHandlers.stream()
                .filter(handler -> !Modifier.isPublic(handler.getMethod().getModifiers()))
                .map(HandlerMethod::toString)
                .toList();
        assertThat(nonPublic)
                .as("Non-public endpoints (method security annotations are ignored on them)")
                .isEmpty();
    }
}
