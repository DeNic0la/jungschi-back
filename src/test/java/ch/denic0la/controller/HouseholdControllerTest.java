package ch.denic0la.controller;

import ch.denic0la.model.AppUser;
import ch.denic0la.model.Camp;
import ch.denic0la.model.CampParticipant;
import ch.denic0la.model.HealthStats;
import ch.denic0la.model.Household;
import ch.denic0la.model.HouseholdGuardian;
import ch.denic0la.model.IntoleranceSelection;
import ch.denic0la.model.Participant;
import ch.denic0la.model.ParticipantGeneralData;
import ch.denic0la.model.Room;
import ch.denic0la.model.SignUp;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@QuarkusTest
public class HouseholdControllerTest {

    @Inject
    EntityManager entityManager;

    @BeforeEach
    @Transactional
    public void setup() {
        entityManager.createNativeQuery("DELETE FROM room_leader_assignment").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM camp_participant_medication").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM household_guardian").executeUpdate();
        CampParticipant.deleteAll();
        SignUp.deleteAll();
        IntoleranceSelection.deleteAll();
        ParticipantGeneralData.deleteAll();
        HealthStats.deleteAll();
        Participant.deleteAll();
        Room.deleteAll();
        Camp.deleteAll();
        Household.deleteAll();
        AppUser.deleteAll();

        AppUser current = user("household-current", "current@example.com", "Current", "Guardian", "guardian");
        user("household-extra", "extra@example.com", "Extra", "Guardian", "guardian");
        AppUser other = user("household-other", "other@example.com", "Other", "Guardian", "guardian");

        Household household = new Household();
        household.primaryContact = current;
        household.persist();

        HouseholdGuardian pending = new HouseholdGuardian();
        pending.household = household;
        pending.email = "claim-pending@example.com";
        pending.contactType = HouseholdGuardian.ContactType.PENDING;
        pending.persist();

        Household otherHousehold = new Household();
        otherHousehold.primaryContact = other;
        otherHousehold.persist();
    }

    @Test
    @TestSecurity(user = "household-current", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "household-current"),
            @Claim(key = "preferred_username", value = "household-current"),
            @Claim(key = "email", value = "current@example.com")
    })
    public void guardianCanManageOwnHouseholdAndPendingGuardian() {
        given()
                .contentType("application/json")
                .body("{\"streetAndNumber\": \"Main 1\", \"plz\": \"6000\", \"place\": \"Luzern\"}")
                .when().put("/api/household/me")
                .then()
                .statusCode(200)
                .body("streetAndNumber", is("Main 1"))
                .body("plz", is("6000"))
                .body("place", is("Luzern"))
                .body("guardians.email", hasItems("current@example.com"))
                .body("guardians.find { it.email == 'current@example.com' }.contactType", is("PRIMARY"));

        given()
                .contentType("application/json")
                .body("{\"email\": \"extra@example.com\"}")
                .when().post("/api/household/me/guardians")
                .then()
                .statusCode(200)
                .body("guardians.email", hasItems("current@example.com", "extra@example.com"))
                .body("guardians.find { it.email == 'extra@example.com' }.contactType", is("SECONDARY"));

        given()
                .contentType("application/json")
                .body("{\"email\": \"pending@example.com\"}")
                .when().post("/api/household/me/guardians")
                .then()
                .statusCode(200)
                .body("guardians.email", hasItems("pending@example.com"))
                .body("guardians.find { it.email == 'pending@example.com' }.pending", is(true));

        given()
                .when().delete("/api/household/me/guardians/pending@example.com")
                .then()
                .statusCode(200)
                .body("guardians.email", not(hasItems("pending@example.com")));
    }

    @Test
    @TestSecurity(user = "household-current", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "household-current"),
            @Claim(key = "preferred_username", value = "household-current"),
            @Claim(key = "email", value = "current@example.com")
    })
    public void guardianCanSetPrimaryAndSecondaryContacts() {
        given()
                .contentType("application/json")
                .body("{\"email\": \"extra@example.com\"}")
                .when().post("/api/household/me/guardians")
                .then()
                .statusCode(200)
                .body("guardians.find { it.email == 'extra@example.com' }.contactType", is("SECONDARY"));

        given()
                .contentType("application/json")
                .body("{\"contactType\": \"SECONDARY\"}")
                .when().put("/api/household/me/guardians/extra@example.com/contact-type")
                .then()
                .statusCode(200)
                .body("guardians.find { it.email == 'extra@example.com' }.contactType", is("SECONDARY"));

        given()
                .contentType("application/json")
                .body("{\"contactType\": \"PRIMARY\"}")
                .when().put("/api/household/me/guardians/extra@example.com/contact-type")
                .then()
                .statusCode(200)
                .body("guardians.find { it.email == 'extra@example.com' }.contactType", is("PRIMARY"))
                .body("guardians.find { it.email == 'current@example.com' }.contactType", is("ADDITIONAL"));
    }

    @Test
    @TestSecurity(user = "household-current", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "household-current"),
            @Claim(key = "preferred_username", value = "household-current"),
            @Claim(key = "email", value = "current@example.com")
    })
    public void pendingGuardianCannotBePrimaryOrSecondaryContact() {
        given()
                .contentType("application/json")
                .body("{\"email\": \"pending@example.com\"}")
                .when().post("/api/household/me/guardians")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .body("{\"contactType\": \"SECONDARY\"}")
                .when().put("/api/household/me/guardians/pending@example.com/contact-type")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "household-current", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "household-current"),
            @Claim(key = "preferred_username", value = "household-current"),
            @Claim(key = "email", value = "current@example.com")
    })
    public void guardianCannotAddOtherHouseholdGuardian() {
        given()
                .contentType("application/json")
                .body("{\"email\": \"other@example.com\"}")
                .when().post("/api/household/me/guardians")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "pending-guardian", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "pending-guardian"),
            @Claim(key = "preferred_username", value = "pending-guardian"),
            @Claim(key = "email", value = "claim-pending@example.com")
    })
    public void pendingGuardianEmailJoinsReferencedHouseholdOnLogin() {
        given()
                .when().get("/api/household/me")
                .then()
                .statusCode(200)
                .body("guardians.find { it.email == 'claim-pending@example.com' }.pending", is(false))
                .body("guardians.find { it.email == 'claim-pending@example.com' }.username", is("pending-guardian"))
                .body("guardians.find { it.email == 'claim-pending@example.com' }.contactType", is("SECONDARY"));
    }

    @Test
    @TestSecurity(user = "new-household-creator", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "new-household-creator"),
            @Claim(key = "preferred_username", value = "new-household-creator"),
            @Claim(key = "email", value = "new-household-creator@example.com")
    })
    public void creatorBecomesPrimaryContactForNewHousehold() {
        given()
                .when().get("/api/household/me")
                .then()
                .statusCode(200)
                .body("guardians.find { it.email == 'new-household-creator@example.com' }.contactType", is("PRIMARY"))
                .body("guardians.find { it.email == 'new-household-creator@example.com' }.currentUser", is(true));
    }

    private AppUser user(String sub, String email, String firstName, String lastName, String roles) {
        AppUser user = new AppUser();
        user.username = sub;
        user.email = email;
        user.firstName = firstName;
        user.lastName = lastName;
        user.roles = roles;
        user.createdAt = Instant.now();
        user.lastSeenAt = Instant.now();
        user.persist();
        return user;
    }
}
