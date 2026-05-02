package ch.denic0la;

import ch.denic0la.model.AppUser;
import ch.denic0la.model.CampParticipant;
import ch.denic0la.model.Household;
import ch.denic0la.model.Participant;
import ch.denic0la.model.Room;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;
import java.util.List;
import java.util.TreeSet;

@ApplicationScoped
public class CurrentUserProvisioningService {

    @Inject
    JsonWebToken jwt;

    @Inject
    Instance<UserInfo> userInfoInstance;

    @Inject
    SecurityIdentity identity;

    public AppUser getCurrentUser() {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new IllegalStateException("Missing OIDC subject claim");
        }

        AppUser user = AppUser.findById(sub);
        if (user == null){
            throw new IllegalStateException("No User found for OIDC subject: " + sub );
        }
        return user;
    }

    @Transactional
    public AppUser ensureCurrentUser() {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new IllegalStateException("Missing OIDC subject claim");
        }

        AppUser user = AppUser.findById(sub);
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
        user.pictureUrl = firstNonBlank(
                stringClaim("picture"),
                userInfoString("picture"),
                user.pictureUrl
        );
        if (user.phoneNumber == null || user.phoneNumber.isBlank()){
            user.phoneNumber = firstNonBlank(
                    stringClaim("phone_number"),
                    userInfoString("phone_number")
            );
        }
        user.roles = identity.getRoles().isEmpty()
                ? null
                : String.join(",", new TreeSet<>(identity.getRoles()));
        user.lastSeenAt = Instant.now();

        if (isNew) {
            user.persist();
        }

        return user;
    }

    @Transactional
    public Household ensureCurrentUserHousehold() {
        AppUser user = ensureCurrentUser();
        Household household = findHouseholdForContact(user);
        if (household == null) {
            household = new Household();
            household.primaryContact = user;
            household.persist();
        }
        return household;
    }

    public Household findHouseholdForContact(AppUser user) {
        if (user == null) {
            return null;
        }
        Household primary = Household.find("primaryContact", user).firstResult();
        if (primary != null) {
            return primary;
        }
        return Household.find("secondaryContact", user).firstResult();
    }

    public boolean isHouseholdContact(Household household, AppUser user) {
        return household != null
                && user != null
                && ((household.primaryContact != null && household.primaryContact.oidcSubject.equals(user.oidcSubject))
                || (household.secondaryContact != null && household.secondaryContact.oidcSubject.equals(user.oidcSubject)));
    }

    public boolean canReadParticipant(Participant participant, AppUser user) {
        return participant != null && (canViewAnything() || isHouseholdContact(participant.household, user));
    }

    public boolean canWriteParticipant(Participant participant, AppUser user) {
        return participant != null && (isAdmin() || isHouseholdContact(participant.household, user));
    }

    public boolean canViewGuardian(AppUser currentUser, AppUser candidate) {
        if (currentUser == null || candidate == null || !candidate.hasRole("guardian")) {
            return false;
        }
        if (canViewAnything()) {
            return true;
        }
        if (!identity.hasRole("guardian")) {
            return false;
        }

        Household currentHousehold = findHouseholdForContact(currentUser);
        Household candidateHousehold = findHouseholdForContact(candidate);
        return candidateHousehold == null
                || (currentHousehold != null && currentHousehold.id.equals(candidateHousehold.id));
    }

    public boolean canViewRoom(Room room, AppUser user) {
        if (room == null || user == null) {
            return false;
        }
        if (canViewAnything()) {
            return true;
        }
        Household household = findHouseholdForContact(user);
        if (household == null || !identity.hasRole("guardian")) {
            return false;
        }
        return CampParticipant.count("room = ?1 and participant.household = ?2", room, household) > 0;
    }

    public boolean canViewAnything() {
        return isAdmin() || identity.hasRole("Sanitaet") || identity.hasRole("Jungschiteam");
    }

    public boolean isAdmin() {
        return identity.hasRole("ADMIN");
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

    private String firstNonBlank(String a, String b, String c) {
        return firstNonBlank(firstNonBlank(a, b), c);
    }
}
