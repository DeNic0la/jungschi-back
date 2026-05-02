package ch.denic0la.controller;

import ch.denic0la.model.AppUser;
import ch.denic0la.model.Camp;
import ch.denic0la.model.CampParticipant;
import ch.denic0la.model.CampStats;
import ch.denic0la.model.Gender;
import ch.denic0la.model.HealthStats;
import ch.denic0la.model.Household;
import ch.denic0la.model.IntoleranceSelection;
import ch.denic0la.model.Participant;
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
import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class ParticipantHouseholdAccessControllerTest {

    @Inject
    EntityManager entityManager;

    private Long participantId;
    private Long outsiderParticipantId;

    @BeforeEach
    @Transactional
    public void setup() {
        entityManager.createNativeQuery("DELETE FROM room_leader_assignment").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM camp_participant_medication").executeUpdate();
        CampParticipant.deleteAll();
        SignUp.deleteAll();
        IntoleranceSelection.deleteAll();
        CampStats.deleteAll();
        HealthStats.deleteAll();
        Participant.deleteAll();
        Room.deleteAll();
        Camp.deleteAll();
        Household.deleteAll();
        AppUser.deleteAll();

        AppUser primary = user("household-primary", "primary@example.com", "Primary", "Guardian", "guardian");
        AppUser secondary = user("household-secondary", "secondary@example.com", "Secondary", "Guardian", "guardian");
        AppUser outsider = user("household-outsider", "outsider@example.com", "Out", "Sider", "guardian");

        Household household = new Household();
        household.primaryContact = primary;
        household.secondaryContact = secondary;
        household.persist();

        Household outsiderHousehold = new Household();
        outsiderHousehold.primaryContact = outsider;
        outsiderHousehold.persist();

        Participant participant = new Participant();
        participant.firstname = "Max";
        participant.lastname = "Muster";
        participant.dateOfBirth = LocalDate.of(2011, 5, 4);
        participant.gender = Gender.MALE;
        participant.household = household;
        participant.persist();
        participantId = participant.id;

        Participant outsiderParticipant = new Participant();
        outsiderParticipant.firstname = "Else";
        outsiderParticipant.lastname = "Where";
        outsiderParticipant.dateOfBirth = LocalDate.of(2010, 4, 3);
        outsiderParticipant.gender = Gender.FEMALE;
        outsiderParticipant.household = outsiderHousehold;
        outsiderParticipant.persist();
        outsiderParticipantId = outsiderParticipant.id;
    }

    @Test
    @TestSecurity(user = "household-secondary", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "household-secondary"),
            @Claim(key = "preferred_username", value = "household-secondary"),
            @Claim(key = "email", value = "secondary@example.com")
    })
    public void secondaryContactCanReadAndWriteHouseholdParticipant() {
        given()
                .when().get("/api/participants/" + participantId)
                .then()
                .statusCode(200)
                .body("firstname", is("Max"))
                .body("gender", is("male"));

        given()
                .contentType("application/json")
                .body("{\"firstname\": \"Maxim\", \"lastname\": \"Muster\", \"dateOfBirth\": \"2011-05-04\", \"gender\": \"male\"}")
                .when().put("/api/participants/" + participantId)
                .then()
                .statusCode(200)
                .body("firstname", is("Maxim"));

        given()
                .when().get("/api/participants/" + outsiderParticipantId)
                .then()
                .statusCode(404);
    }

    private AppUser user(String sub, String email, String firstName, String lastName, String roles) {
        AppUser user = new AppUser();
        user.oidcSubject = sub;
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
