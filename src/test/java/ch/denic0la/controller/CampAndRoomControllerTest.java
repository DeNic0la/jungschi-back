package ch.denic0la.controller;

import ch.denic0la.model.AppUser;
import ch.denic0la.model.Camp;
import ch.denic0la.model.CampParticipant;
import ch.denic0la.model.Gender;
import ch.denic0la.model.Household;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class CampAndRoomControllerTest {

    @Inject
    EntityManager entityManager;

    private Long visibleRoomId;
    private Long hiddenRoomId;

    @BeforeEach
    @Transactional
    public void setup() {
        entityManager.createNativeQuery("DELETE FROM room_leader_assignment").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM camp_participant_medication").executeUpdate();
        CampParticipant.deleteAll();
        SignUp.deleteAll();
        Participant.deleteAll();
        Room.deleteAll();
        Camp.deleteAll();
        Household.deleteAll();
        AppUser.deleteAll();

        AppUser guardian = user("room-guardian", "guardian@example.com", "Guard", "Ian", "guardian");
        AppUser leader = user("room-leader", "leader@example.com", "Lea", "Der", "Jungschiteam");
        leader.pictureUrl = "https://example.com/leader.png";

        Camp visibleCamp = camp("camp-visible", "Visible Camp", LocalDate.of(2026, 7, 10));
        Camp hiddenCamp = camp("camp-hidden", "Hidden Camp", LocalDate.of(2026, 8, 10));

        Household household = new Household();
        household.primaryContact = guardian;
        household.persist();

        Participant participant = participant(household, "Room", "Kid", Gender.MALE);

        SignUp signUp = new SignUp();
        signUp.household = household;
        signUp.camp = visibleCamp;
        signUp.persist();

        Room visibleRoom = new Room();
        visibleRoom.camp = visibleCamp;
        visibleRoom.name = "Zelt Adler";
        visibleRoom.maxCapacity = 8;
        visibleRoom.gender = Gender.MALE;
        visibleRoom.leaders.add(leader);
        visibleRoom.persist();
        visibleRoomId = visibleRoom.id;

        Room hiddenRoom = new Room();
        hiddenRoom.camp = hiddenCamp;
        hiddenRoom.name = "Zelt Fuchs";
        hiddenRoom.maxCapacity = 6;
        hiddenRoom.gender = Gender.FEMALE;
        hiddenRoom.persist();
        hiddenRoomId = hiddenRoom.id;

        CampParticipant campParticipant = new CampParticipant();
        campParticipant.participant = participant;
        campParticipant.signUp = signUp;
        campParticipant.camp = visibleCamp;
        campParticipant.room = visibleRoom;
        campParticipant.persist();
    }

    @Test
    public void campsAreVisibleWithoutAuthentication() {
        given()
                .when().get("/api/camps")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].id", is("camp-visible"))
                .body("[1].id", is("camp-hidden"));
    }

    @Test
    @TestSecurity(user = "room-guardian", roles = {"guardian"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "room-guardian"),
            @Claim(key = "preferred_username", value = "room-guardian"),
            @Claim(key = "email", value = "guardian@example.com")
    })
    public void guardianCanViewAssignedRoomAndLeaderPicture() {
        given()
                .when().get("/api/rooms/" + visibleRoomId)
                .then()
                .statusCode(200)
                .body("name", is("Zelt Adler"))
                .body("leaders", hasSize(1))
                .body("leaders[0].firstName", is("Lea"))
                .body("leaders[0].pictureUrl", is("https://example.com/leader.png"));

        given()
                .when().get("/api/rooms/" + hiddenRoomId)
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "medic-viewer", roles = {"Sanitaet"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "medic-viewer"),
            @Claim(key = "preferred_username", value = "medic-viewer"),
            @Claim(key = "email", value = "medic@example.com")
    })
    public void sanitaetCanViewAnyRoom() {
        given()
                .when().get("/api/rooms/" + hiddenRoomId)
                .then()
                .statusCode(200)
                .body("name", is("Zelt Fuchs"));
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

    private Camp camp(String id, String title, LocalDate startDate) {
        Camp camp = new Camp();
        camp.id = id;
        camp.title = title;
        camp.startDate = startDate;
        camp.persist();
        return camp;
    }

    private Participant participant(Household household, String firstName, String lastName, Gender gender) {
        Participant participant = new Participant();
        participant.firstname = firstName;
        participant.lastname = lastName;
        participant.dateOfBirth = LocalDate.of(2012, 3, 2);
        participant.gender = gender;
        participant.household = household;
        participant.persist();
        return participant;
    }
}
