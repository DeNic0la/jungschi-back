package ch.denic0la.controller;

import ch.denic0la.model.AppUser;
import ch.denic0la.model.Camp;
import ch.denic0la.model.CampParticipant;
import ch.denic0la.model.CampParticipantMedication;
import ch.denic0la.model.Gender;
import ch.denic0la.model.Household;
import ch.denic0la.model.HouseholdGuardian;
import ch.denic0la.model.Participant;
import ch.denic0la.model.Room;
import ch.denic0la.model.SignUp;
import io.quarkus.narayana.jta.QuarkusTransaction;
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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;

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
        Room.deleteAll();
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

    @Test
    @TestSecurity(user = "signup-guardian", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "signup-guardian"),
            @Claim(key = "preferred_username", value = "signup-guardian"),
            @Claim(key = "email", value = "signup@example.com")
    })
    public void guardianMustReopenCompletedSignupBeforeEditing() {
        Long signupId = createSignup(SignUp.State.COMPLETED, null);

        given()
                .contentType("application/json")
                .body("""
                        {
                          "campId": "signup-camp",
                          "photoConsent": true,
                          "infoEmail": true,
                          "campParticipants": [
                            {
                              "participantId": 1,
                              "drugConsent": true,
                              "medications": []
                            }
                          ]
                        }
                        """)
                .when().post("/api/signups")
                .then()
                .statusCode(400);

        given()
                .when().put("/api/signups/" + signupId + "/reopen")
                .then()
                .statusCode(200)
                .body("state", is("IN_PROGRESS"));
    }

    @Test
    @TestSecurity(user = "team-reviewer", roles = {"Jungschiteam"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "team-reviewer"),
            @Claim(key = "preferred_username", value = "team-reviewer"),
            @Claim(key = "email", value = "team@example.com")
    })
    public void teamCanRejectAndApproveSignup() {
        Long signupId = createSignup(SignUp.State.COMPLETED, null);

        given()
                .contentType("application/json")
                .body("{\"feedback\": \"Bitte Kontaktoption ergänzen\"}")
                .when().put("/api/signups/" + signupId + "/reject")
                .then()
                .statusCode(200)
                .body("state", is("IN_PROGRESS"))
                .body("feedback", is("Bitte Kontaktoption ergänzen"));

        setSignupState(signupId, SignUp.State.COMPLETED);

        given()
                .when().put("/api/signups/" + signupId + "/approve")
                .then()
                .statusCode(200)
                .body("state", is("APPROVED"));
    }

    @Test
    @TestSecurity(user = "team-reviewer", roles = {"Jungschiteam"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "team-reviewer"),
            @Claim(key = "preferred_username", value = "team-reviewer"),
            @Claim(key = "email", value = "team@example.com")
    })
    public void teamReviewSortsCompletedFirstAndFiltersSensitiveCampParticipantFields() {
        createSignupFor("progress@example.com", SignUp.State.IN_PROGRESS, null, Gender.FEMALE, null);
        createSignupFor("approved@example.com", SignUp.State.APPROVED, null, Gender.FEMALE, null);
        createSignupFor("completed@example.com", SignUp.State.COMPLETED, null, Gender.FEMALE, null);

        given()
                .when().get("/api/signups/camp/signup-camp/review")
                .then()
                .statusCode(200)
                .body("[0].state", is("COMPLETED"))
                .body("[1].state", is("IN_PROGRESS"))
                .body("[2].state", is("APPROVED"))
                .body("[0].campParticipants[0].infosZimmerleitung", nullValue())
                .body("[0].campParticipants[0].bemerkungen", nullValue())
                .body("[0].campParticipants[0].drugConsent", nullValue())
                .body("[0].campParticipants[0].medications", hasSize(0))
                .body("[0].campParticipants[0].fullAccess", is(false));
    }

    @Test
    @TestSecurity(user = "room-leader", roles = {"Jungschiteam"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "room-leader"),
            @Claim(key = "preferred_username", value = "room-leader"),
            @Claim(key = "email", value = "leader@example.com")
    })
    public void assignedRoomLeaderCanSeeZimmerleitungInfoOnly() {
        Long campParticipantId = createSignupFor("leader-child@example.com", SignUp.State.COMPLETED, null, Gender.FEMALE, "leader@example.com");

        given()
                .when().get("/api/signups/camp/signup-camp/review")
                .then()
                .statusCode(200)
                .body("[0].campParticipants[0].id", is(campParticipantId.intValue()))
                .body("[0].campParticipants[0].infosZimmerleitung", is("Sleeps lightly"))
                .body("[0].campParticipants[0].roomLeaderInfoVisible", is(true))
                .body("[0].campParticipants[0].bemerkungen", nullValue())
                .body("[0].campParticipants[0].fullAccess", is(false));
    }

    @Test
    @TestSecurity(user = "signup-guardian", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "signup-guardian"),
            @Claim(key = "preferred_username", value = "signup-guardian"),
            @Claim(key = "email", value = "signup@example.com")
    })
    public void campParticipantDetailRequiresHouseholdOrPrivilegedRole() {
        Long campParticipantId = createSignupFor("signup@example.com", SignUp.State.COMPLETED, null, Gender.FEMALE, null);

        given()
                .when().get("/api/signups/camp-participants/" + campParticipantId)
                .then()
                .statusCode(200)
                .body("fullAccess", is(true))
                .body("dateOfBirth", is("2015-01-01"));
    }

    @Test
    @TestSecurity(user = "admin-reviewer", roles = {"ADMIN", "Sanitaet"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "admin-reviewer"),
            @Claim(key = "preferred_username", value = "admin-reviewer"),
            @Claim(key = "email", value = "admin@example.com")
    })
    public void privilegedCampParticipantDetailDoesNotExposeRoomLeaderNotes() {
        Long campParticipantId = createSignupFor("privileged@example.com", SignUp.State.COMPLETED, null, Gender.FEMALE, null);

        given()
                .when().get("/api/signups/camp-participants/" + campParticipantId)
                .then()
                .statusCode(200)
                .body("fullAccess", is(true))
                .body("infosZimmerleitung", nullValue())
                .body("roomLeaderInfoVisible", is(false))
                .body("bemerkungen", is("Bring rain jacket"))
                .body("medications", hasSize(1));
    }

    @Test
    @TestSecurity(user = "team-reviewer", roles = {"Jungschiteam"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "team-reviewer"),
            @Claim(key = "preferred_username", value = "team-reviewer"),
            @Claim(key = "email", value = "team@example.com")
    })
    public void teamCanOpenFilteredCampParticipantDetail() {
        Long campParticipantId = createSignupFor("detail-team@example.com", SignUp.State.COMPLETED, null, Gender.FEMALE, null);

        given()
                .when().get("/api/signups/camp-participants/" + campParticipantId)
                .then()
                .statusCode(200)
                .body("fullAccess", is(false))
                .body("dateOfBirth", nullValue())
                .body("bemerkungen", nullValue())
                .body("drugConsent", nullValue())
                .body("medications", hasSize(0));
    }

    @Test
    @TestSecurity(user = "outsider-guardian", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "outsider-guardian"),
            @Claim(key = "preferred_username", value = "outsider-guardian"),
            @Claim(key = "email", value = "outsider@example.com")
    })
    public void guardianCannotViewOtherHouseholdsCampParticipantDetail() {
        Long campParticipantId = createSignupFor("signup@example.com", SignUp.State.COMPLETED, null, Gender.FEMALE, null);

        given()
                .when().get("/api/signups/camp-participants/" + campParticipantId)
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "team-reviewer", roles = {"Jungschiteam"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "team-reviewer"),
            @Claim(key = "preferred_username", value = "team-reviewer"),
            @Claim(key = "email", value = "team@example.com")
    })
    public void roomAssignmentRejectsFullRoomsAndGenderMismatch() {
        Long campParticipantId = createSignupFor("room-target@example.com", SignUp.State.COMPLETED, null, Gender.MALE, null);
        Long femaleRoomId = createRoom("female-room", Gender.FEMALE, 4);
        Long fullRoomId = createRoom("full-room", Gender.MALE, 1);
        createAssignedCampParticipant("occupant@example.com", fullRoomId, Gender.MALE);

        given()
                .contentType("application/json")
                .body("{\"roomId\": %d}".formatted(femaleRoomId))
                .when().put("/api/signups/camp-participants/" + campParticipantId + "/room")
                .then()
                .statusCode(400);

        given()
                .contentType("application/json")
                .body("{\"roomId\": %d}".formatted(fullRoomId))
                .when().put("/api/signups/camp-participants/" + campParticipantId + "/room")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "signup-guardian", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "signup-guardian"),
            @Claim(key = "preferred_username", value = "signup-guardian"),
            @Claim(key = "email", value = "signup@example.com")
    })
    public void guardianSeesFeedbackOnlyWhenSignupIsReturned() {
        Long signupId = createSignup(SignUp.State.COMPLETED, "Internal note");

        given()
                .when().get("/api/signups/camp/signup-camp")
                .then()
                .statusCode(200)
                .body("id", is(signupId.intValue()))
                .body("feedback", is((String) null));

        setSignupState(signupId, SignUp.State.IN_PROGRESS);

        given()
                .when().get("/api/signups/camp/signup-camp")
                .then()
                .statusCode(200)
                .body("feedback", is("Internal note"));
    }

    Long createSignupFor(
            String guardianEmail,
            SignUp.State state,
            String feedback,
            Gender gender,
            String roomLeaderEmail) {
        return QuarkusTransaction.requiringNew().call(() -> {
            AppUser guardian = user(guardianEmail, "guardian");

            Household household = new Household();
            household.primaryContact = guardian;
            household.persist();

            HouseholdGuardian membership = new HouseholdGuardian();
            membership.household = household;
            membership.user = guardian;
            membership.email = guardian.email;
            membership.contactType = HouseholdGuardian.ContactType.PRIMARY;
            membership.persist();

            Participant participant = new Participant();
            participant.firstname = "Anna";
            participant.lastname = "Muster";
            participant.dateOfBirth = LocalDate.of(2015, 1, 1);
            participant.gender = gender;
            participant.household = household;
            participant.persist();

            SignUp signUp = new SignUp();
            signUp.household = household;
            signUp.camp = Camp.findById("signup-camp");
            signUp.state = state;
            signUp.feedback = feedback;
            signUp.persist();

            Room room = null;
            if (roomLeaderEmail != null) {
                AppUser leader = user(roomLeaderEmail, "Jungschiteam");
                room = new Room();
                room.camp = signUp.camp;
                room.name = "Zimmer A";
                room.maxCapacity = 8;
                room.gender = gender;
                room.leaders.add(leader);
                room.persist();
            }

            CampParticipant campParticipant = new CampParticipant();
            campParticipant.participant = participant;
            campParticipant.signUp = signUp;
            campParticipant.camp = signUp.camp;
            campParticipant.room = room;
            campParticipant.schoolClass = "5a";
            campParticipant.infosZimmerleitung = "Sleeps lightly";
            campParticipant.bemerkungen = "Bring rain jacket";
            campParticipant.drugConsent = true;
            campParticipant.persist();

            CampParticipantMedication medication = new CampParticipantMedication();
            medication.campParticipant = campParticipant;
            medication.medicationName = "Ventolin";
            medication.persist();

            return campParticipant.id;
        });
    }

    Long createRoom(String name, Gender gender, int maxCapacity) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Room room = new Room();
            room.camp = Camp.findById("signup-camp");
            room.name = name;
            room.gender = gender;
            room.maxCapacity = maxCapacity;
            room.persist();
            return room.id;
        });
    }

    void createAssignedCampParticipant(String guardianEmail, Long roomId, Gender gender) {
        QuarkusTransaction.requiringNew().run(() -> {
            AppUser guardian = user(guardianEmail, "guardian");
            Household household = new Household();
            household.primaryContact = guardian;
            household.persist();

            Participant participant = new Participant();
            participant.firstname = "Other";
            participant.lastname = "Child";
            participant.dateOfBirth = LocalDate.of(2014, 1, 1);
            participant.gender = gender;
            participant.household = household;
            participant.persist();

            SignUp signUp = new SignUp();
            signUp.household = household;
            signUp.camp = Camp.findById("signup-camp");
            signUp.state = SignUp.State.COMPLETED;
            signUp.persist();

            CampParticipant campParticipant = new CampParticipant();
            campParticipant.participant = participant;
            campParticipant.signUp = signUp;
            campParticipant.camp = signUp.camp;
            campParticipant.room = Room.findById(roomId);
            campParticipant.persist();
        });
    }

    AppUser user(String email, String roles) {
        AppUser user = AppUser.findById(email);
        if (user != null) {
            return user;
        }
        user = new AppUser();
        user.email = email;
        user.username = email;
        user.roles = roles;
        user.createdAt = java.time.Instant.now();
        user.lastSeenAt = java.time.Instant.now();
        user.persist();
        return user;
    }

    Long createSignup(SignUp.State state, String feedback) {
        return QuarkusTransaction.requiringNew().call(() -> {
            AppUser guardian = new AppUser();
            guardian.email = "signup@example.com";
            guardian.username = "signup-guardian";
            guardian.roles = "guardian";
            guardian.createdAt = java.time.Instant.now();
            guardian.lastSeenAt = java.time.Instant.now();
            guardian.persist();

            Household household = new Household();
            household.primaryContact = guardian;
            household.persist();

            HouseholdGuardian membership = new HouseholdGuardian();
            membership.household = household;
            membership.user = guardian;
            membership.email = guardian.email;
            membership.contactType = HouseholdGuardian.ContactType.PRIMARY;
            membership.persist();

            Participant participant = new Participant();
            participant.firstname = "Anna";
            participant.lastname = "Muster";
            participant.dateOfBirth = LocalDate.of(2015, 1, 1);
            participant.gender = ch.denic0la.model.Gender.FEMALE;
            participant.household = household;
            participant.persist();

            SignUp signUp = new SignUp();
            signUp.household = household;
            signUp.camp = Camp.findById("signup-camp");
            signUp.state = state;
            signUp.feedback = feedback;
            signUp.persist();

            CampParticipant campParticipant = new CampParticipant();
            campParticipant.participant = participant;
            campParticipant.signUp = signUp;
            campParticipant.camp = signUp.camp;
            campParticipant.drugConsent = true;
            campParticipant.persist();

            return signUp.id;
        });
    }

    void setSignupState(Long signupId, SignUp.State state) {
        QuarkusTransaction.requiringNew().run(() -> {
            SignUp signUp = SignUp.findById(signupId);
            signUp.state = state;
        });
    }
}
