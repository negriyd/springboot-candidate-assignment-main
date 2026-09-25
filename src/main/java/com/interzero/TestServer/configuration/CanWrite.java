package com.interzero.TestServer.configuration;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows access to users who may write data (create, update, delete): {@link Role#WRITER} and {@link Role#ADMIN}.
 * <p>
 * Only works on public methods of Spring beans (method security is proxy based).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('WRITER', 'ADMIN')")
public @interface CanWrite {
}
