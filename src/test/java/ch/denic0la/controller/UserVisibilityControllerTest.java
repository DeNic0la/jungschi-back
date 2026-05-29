package ch.denic0la.controller;

import ch.denic0la.model.AppUser;
import ch.denic0la.model.Camp;
import ch.denic0la.model.CampParticipant;
import ch.denic0la.model.HealthStats;
import ch.denic0la.model.Household;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@QuarkusTest
public class UserVisibilityControllerTest {

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

        AppUser currentGuardian = user("guardian-current", "current@example.com", "Current", "Guardian", "guardian");
        AppUser sameHouseholdGuardian = user("guardian-same", "same@example.com", "Same", "Guardian", "guardian");
        AppUser otherHouseholdGuardian = user("guardian-other", "other@example.com", "Other", "Guardian", "guardian");
        user("guardian-free", "free@example.com", "Free", "Guardian", "guardian");
        user("plain-user", "plain@example.com", "Plain", "User", "user");

        Household sameHousehold = new Household();
        sameHousehold.primaryContact = currentGuardian;
        sameHousehold.secondaryContact = sameHouseholdGuardian;
        sameHousehold.persist();

        Household otherHousehold = new Household();
        otherHousehold.primaryContact = otherHouseholdGuardian;
        otherHousehold.persist();
    }

    @Test
    @TestSecurity(user = "guardian-current", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "guardian-current"),
            @Claim(key = "preferred_username", value = "guardian-current"),
            @Claim(key = "email", value = "current@example.com")
    })
    public void guardianSeesOnlySameHouseholdGuardians() {
        given()
                .when().get("/api/users/guardians")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("id", hasSize(1))
                .body("id", org.hamcrest.Matchers.hasItems("same@example.com"))
                .body("id", not(org.hamcrest.Matchers.hasItems("other@example.com", "free@example.com", "plain@example.com")));
    }

    @Test
    @TestSecurity(user = "admin-current", roles = {"ADMIN"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "admin-current"),
            @Claim(key = "preferred_username", value = "admin-current"),
            @Claim(key = "email", value = "admin@example.com")
    })
    public void adminSeesAllGuardians() {
        given()
                .when().get("/api/users/guardians")
                .then()
                .statusCode(200)
                .body("size()", is(4))
                .body("id", org.hamcrest.Matchers.hasItems("current@example.com", "same@example.com", "other@example.com", "free@example.com"));
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
