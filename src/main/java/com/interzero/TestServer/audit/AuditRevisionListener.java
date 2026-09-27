package com.interzero.TestServer.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Fills in the user of each new {@link AuditRevision} from the Spring Security context of the current request.
 */
public class AuditRevisionListener implements RevisionListener {

    /**
     * The username recorded when no user is logged in, e.g. for the data loaded at startup.
     */
    public static final String SYSTEM_USER = "system";

    @Override
    public void newRevision(Object revisionEntity) {
        ((AuditRevision) revisionEntity).setUsername(currentUsername());
    }

    private static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SYSTEM_USER;
        }
        return authentication.getName();
    }
}
