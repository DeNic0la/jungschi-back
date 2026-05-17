package ch.denic0la;

import ch.denic0la.model.AppUser;
import ch.denic0la.model.CampParticipant;
import ch.denic0la.model.Household;
import ch.denic0la.model.HouseholdGuardian;
import ch.denic0la.model.Participant;
import ch.denic0la.model.Room;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

@ApplicationScoped
public class CurrentUserProvisioningService {

    private static final Logger LOG = Logger.getLogger(CurrentUserProvisioningService.class);

    @Inject
    JsonWebToken jwt;

    @Inject
    Instance<UserInfo> userInfoInstance;

    @Inject
    SecurityIdentity identity;

    @Inject
    ObjectMapper objectMapper;

    public AppUser getCurrentUser() {
        String email = currentEmail();

        AppUser user = AppUser.findById(email);
        if (user == null){
            throw new IllegalStateException("No User found for email: " + email);
        }
        return user;
    }

    @Transactional
    public AppUser ensureCurrentUser() {
        String email = currentEmail();

        AppUser user = AppUser.findById(email);
        boolean isNew = false;

        if (user == null) {
            user = new AppUser();
            user.email = email;
            user.createdAt = Instant.now();
            loadOpenIdConnectData(user);
            LOG.debugf("Provisioned new user from OpenID Connect data: email=%s openIdConnectData=%s",
                    user.email,
                    user.openidConnectData);
            isNew = true;
        }
        user.roles = identity.getRoles().isEmpty()
                ? null
                : String.join(",", new TreeSet<>(identity.getRoles()));
        user.lastSeenAt = Instant.now();

        if (isNew) {
            user.persist();
        }
        claimPendingGuardianMembership(user);

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
            ensureGuardianMembership(household, user, HouseholdGuardian.ContactType.PRIMARY);
        }
        return household;
    }

    public Household findHouseholdForContact(AppUser user) {
        if (user == null) {
            return null;
        }
        HouseholdGuardian membership = HouseholdGuardian.find("user = ?1", user).firstResult();
        if (membership == null && user.email != null) {
            membership = HouseholdGuardian.find("email", normalizeEmail(user.email)).firstResult();
        }
        if (membership != null) {
            return membership.household;
        }
        Household primary = Household.find("primaryContact", user).firstResult();
        if (primary != null) {
            return primary;
        }
        return Household.find("secondaryContact", user).firstResult();
    }

    public boolean isHouseholdContact(Household household, AppUser user) {
        Household userHousehold = findHouseholdForContact(user);
        return household != null
                && user != null
                && ((userHousehold != null && Objects.equals(userHousehold.id, household.id))
                || (household.primaryContact != null && household.primaryContact.email.equals(user.email))
                || (household.secondaryContact != null && household.secondaryContact.email.equals(user.email)));
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
        if (isAdmin()) {
            return true;
        }
        if (!identity.hasRole("guardian")) {
            return false;
        }

        Household currentHousehold = findHouseholdForContact(currentUser);
        Household candidateHousehold = findHouseholdForContact(candidate);
        return currentHousehold != null
                && candidateHousehold != null
                && currentHousehold.id.equals(candidateHousehold.id);
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

    @Transactional
    public HouseholdGuardian ensureGuardianMembership(
            Household household,
            AppUser user,
            HouseholdGuardian.ContactType contactType) {
        if (household == null || user == null || user.email == null) {
            return null;
        }
        String email = normalizeEmail(user.email);
        HouseholdGuardian membership = HouseholdGuardian.find("email", email).firstResult();
        if (membership == null) {
            membership = new HouseholdGuardian();
            membership.household = household;
            membership.email = email;
        }
        membership.user = user;
        membership.contactType = contactType != null ? contactType : HouseholdGuardian.ContactType.ADDITIONAL;
        membership.persist();
        return membership;
    }

    private void claimPendingGuardianMembership(AppUser user) {
        if (user == null || user.email == null) {
            return;
        }
        if (!identity.hasRole("guardian")) {
            return;
        }
        String email = normalizeEmail(user.email);
        HouseholdGuardian membership = HouseholdGuardian.find("email", email).firstResult();
        if (membership != null && membership.user == null) {
            membership.user = user;
            if (membership.household.secondaryContact == null) {
                membership.contactType = HouseholdGuardian.ContactType.SECONDARY;
                membership.household.secondaryContact = user;
            } else {
                membership.contactType = HouseholdGuardian.ContactType.ADDITIONAL;
            }
        }
    }

    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private void loadOpenIdConnectData(AppUser user) {
        String principalName = principalName();
        user.username = firstNonBlank(
                tokenString("preferred_username"),
                userInfoString("preferred_username"),
                principalName
        );
        user.firstName = firstNonBlank(
                tokenString("given_name"),
                userInfoString("given_name")
        );
        user.lastName = firstNonBlank(
                tokenString("family_name"),
                userInfoString("family_name")
        );
        user.pictureUrl = firstNonBlank(
                tokenString("picture"),
                userInfoString("picture"),
                tokenString("profile_image"),
                userInfoString("profile_image")
        );
        user.phoneNumber = firstNonBlank(
                tokenString("phone_number"),
                userInfoString("phone_number")
        );
        user.openidConnectData = openIdConnectDataJson();
    }

    private String tokenString(String name) {
        Object value = jwt.getClaim(name);
        return value != null ? value.toString() : null;
    }

    private String currentEmail() {
        String email = firstNonBlank(
                tokenString("email"),
                userInfoString("email")
        );
        if (email == null) {
            throw new IllegalStateException("Missing OIDC email claim; email is required as the application user id");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String principalName() {
        return identity.getPrincipal() != null ? identity.getPrincipal().getName() : null;
    }

    private String openIdConnectDataJson() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tokenClaims", tokenClaims());
        data.put("userInfo", userInfoClaims());
        data.put("principalName", principalName());
        data.put("roles", new TreeSet<>(identity.getRoles()));
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            LOG.debug("Could not serialize OpenID Connect data for new user", e);
            return "{}";
        }
    }

    private Map<String, Object> tokenClaims() {
        Map<String, Object> claims = new LinkedHashMap<>();
        for (String name : new TreeSet<>(jwt.getClaimNames())) {
            claims.put(name, jwt.getClaim(name));
        }
        return claims;
    }

    private Object userInfoClaims() {
        UserInfo ui = currentUserInfo();
        if (ui == null) {
            return null;
        }
        try {
            return objectMapper.readValue(ui.getJsonObject().toString(), Object.class);
        } catch (JsonProcessingException e) {
            LOG.debug("Could not parse OpenID Connect UserInfo data for new user", e);
            return ui.getUserInfoString();
        }
    }

    private String userInfoString(String name) {
        UserInfo ui = currentUserInfo();
        if (ui != null && ui.contains(name)) {
            return ui.getString(name);
        }
        return null;
    }

    private UserInfo currentUserInfo() {
        return userInfoInstance.isResolvable() ? userInfoInstance.get() : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
