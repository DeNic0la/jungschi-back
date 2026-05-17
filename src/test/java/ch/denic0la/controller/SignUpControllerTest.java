package ch.denic0la.controller;

import ch.denic0la.model.AppUser;
import ch.denic0la.model.Camp;
import ch.denic0la.model.CampParticipant;
import ch.denic0la.model.Household;
import ch.denic0la.model.Participant;
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

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class SignUpControllerTest {

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
        Participant.deleteAll();
        Camp.deleteAll();
        Household.deleteAll();
        AppUser.deleteAll();

        Camp camp = new Camp();
        camp.id = "signup-camp";
        camp.title = "Signup Camp";
        camp.startDate = LocalDate.of(2027, 7, 10);
        camp.endDate = LocalDate.of(2027, 7, 17);
        camp.signupEndDate = LocalDate.of(2027, 6, 1);
        camp.persist();
    }

    @Test
    @TestSecurity(user = "signup-guardian", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "signup-guardian"),
            @Claim(key = "preferred_username", value = "signup-guardian"),
            @Claim(key = "email", value = "signup@example.com")
    })
    public void guardianCanSaveReloadAndCompleteSignup() {
        Integer participantId = given()
                .contentType("application/json")
                .body("{\"firstname\": \"Anna\", \"lastname\": \"Muster\", \"dateOfBirth\": \"2015-01-01\", \"gender\": \"female\"}")
                .when().post("/api/participants")
                .then()
                .statusCode(200)
                .extract().path("id");

        Integer signupId = given()
                .contentType("application/json")
                .body("""
                        {
                          "campId": "signup-camp",
                          "photoConsent": true,
                          "infoEmail": true,
                          "additionalContactOptionsDuringCamp": "Call after 20:00",
                          "campParticipants": [
                            {
                              "participantId": %d,
                              "schoolClass": "5a",
                              "infosZimmerleitung": "Sleeps lightly",
                              "bemerkungen": "Bring rain jacket",
                              "drugConsent": false,
                              "medications": [
                                {
                                  "medicationName": "Ventolin",
                                  "dose": "1 puff",
                                  "frequency": "as needed",
                                  "purpose": "Asthma",
                                  "needsHelp": true,
                                  "confidential": false
                                }
                              ]
                            }
                          ]
                        }
                        """.formatted(participantId))
                .when().post("/api/signups")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("state", is("IN_PROGRESS"))
                .body("photoConsent", is(true))
                .body("campParticipants[0].participantId", is(participantId))
                .body("campParticipants[0].drugConsent", is(false))
                .body("campParticipants[0].medications[0].medicationName", is("Ventolin"))
                .extract().path("id");

        given()
                .when().get("/api/signups/camp/signup-camp")
                .then()
                .statusCode(200)
                .body("id", is(signupId))
                .body("additionalContactOptionsDuringCamp", is("Call after 20:00"))
                .body("campParticipants[0].schoolClass", is("5a"));

        given()
                .when().put("/api/signups/" + signupId + "/complete")
                .then()
                .statusCode(200)
                .body("state", is("COMPLETED"));
    }

    @Test
    @TestSecurity(user = "signup-guardian", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "signup-guardian"),
            @Claim(key = "preferred_username", value = "signup-guardian"),
            @Claim(key = "email", value = "signup@example.com")
    })
    public void cannotSignupParticipantFromOtherHousehold() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "campId": "signup-camp",
                          "photoConsent": true,
                          "infoEmail": true,
                          "campParticipants": [
                            {
                              "participantId": 999,
                              "drugConsent": true,
                              "medications": []
                            }
                          ]
                        }
                        """)
                .when().post("/api/signups")
                .then()
                .statusCode(400);
    }
}
