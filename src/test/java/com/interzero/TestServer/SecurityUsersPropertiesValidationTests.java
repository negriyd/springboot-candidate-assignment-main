package com.interzero.TestServer;

import com.interzero.TestServer.configuration.Role;
import com.interzero.TestServer.configuration.SecurityUsersProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that invalid user properties stop the application at startup with a clear error, instead of starting
 * with a user nobody can log in as. Binds only {@link SecurityUsersProperties}, without the rest of the application.
 */
class SecurityUsersPropertiesValidationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfig.class);

    @EnableConfigurationProperties(SecurityUsersProperties.class)
    static class PropertiesConfig {
    }

    @Test
    void validUsersAreBound() {
        runner.withPropertyValues(
                        "app.security.users.auditor.password=secret",
                        "app.security.users.auditor.roles=Reader, WRITER")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SecurityUsersProperties.UserProperties auditor =
                            context.getBean(SecurityUsersProperties.class).users().get("auditor");
                    assertThat(auditor.password()).isEqualTo("secret");
                    assertThat(auditor.roles()).containsExactlyInAnyOrder(Role.READER, Role.WRITER);
                });
    }

    @Test
    void noUsersFailsStartup() {
        runner.run(context -> assertStartupFails(context, "users"));
    }

    @Test
    void userWithoutPasswordFailsStartup() {
        runner.withPropertyValues("app.security.users.auditor.roles=READER")
                .run(context -> assertStartupFails(context, "app.security.users.auditor.password"));
    }

    @Test
    void userWithoutRolesFailsStartup() {
        runner.withPropertyValues("app.security.users.auditor.password=secret")
                .run(context -> assertStartupFails(context, "app.security.users.auditor.roles"));
    }

    @Test
    void unknownRoleFailsStartup() {
        runner.withPropertyValues(
                        "app.security.users.auditor.password=secret",
                        "app.security.users.auditor.roles=AUDITOR")
                .run(context -> assertStartupFails(context, "AUDITOR"));
    }

    private static void assertStartupFails(
            org.springframework.boot.test.context.assertj.AssertableApplicationContext context, String expected) {
        assertThat(context).hasFailed();
        StringBuilder messages = new StringBuilder();
        for (Throwable t = context.getStartupFailure(); t != null; t = t.getCause()) {
            messages.append(t.getMessage()).append('\n');
        }
        assertThat(messages.toString()).contains(expected);
    }
}
