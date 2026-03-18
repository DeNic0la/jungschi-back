package ch.denic0la;

import ch.denic0la.model.AppUser;
import io.quarkus.oidc.UserInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;

@ApplicationScoped
public class CurrentUserProvisioningService {

    @Inject
    JsonWebToken jwt;

    @Inject
    Instance<UserInfo> userInfoInstance;

    @Transactional
    public AppUser ensureCurrentUser() {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new IllegalStateException("Missing OIDC subject claim");
        }

        AppUser user = AppUser.find("oidcSubject", sub).firstResult();
        boolean isNew = false;

        if (user == null) {
            user = new AppUser();
            user.oidcSubject = sub;
            user.createdAt = Instant.now();
            isNew = true;
        }

        user.username = stringClaim("preferred_username");
        user.email = firstNonBlank(
                stringClaim("email"),
                userInfoString("email")
        );
        user.firstName = firstNonBlank(
                stringClaim("given_name"),
                userInfoString("given_name")
        );
        user.lastName = firstNonBlank(
                stringClaim("family_name"),
                userInfoString("family_name")
        );
        user.phoneNumber = firstNonBlank(
                stringClaim("phone_number"),
                userInfoString("phone_number")
        );
        user.lastSeenAt = Instant.now();

        if (isNew) {
            user.persist();
        }

        return user;
    }

    private String stringClaim(String name) {
        Object value = jwt.getClaim(name);
        return value != null ? value.toString() : null;
    }

    private String userInfoString(String name) {
        if (userInfoInstance.isResolvable()) {
            UserInfo ui = userInfoInstance.get();
            if (ui.contains(name)) {
                Object value = ui.get(name);
                return value != null ? value.toString() : null;
            }
        }
        return null;
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}