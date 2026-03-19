package ch.denic0la.team;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class TeamParticipantsControllerTest {

    @Test
    @TestSecurity(user = "team-user", roles = {"Jungschiteam"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "team-user-sub"),
            @Claim(key = "preferred_username", value = "teamuser")
    })
    public void testGetParticipantByIdDetailed() {
        // First create a participant as a normal user
        Integer idInt = given()
                .auth().preemptive().oauth2("anything") // Placeholder, @TestSecurity handles it
                .contentType("application/json")
                .body("{\"firstname\": \"John\", \"lastname\": \"Doe\", \"dateOfBirth\": \"2000-01-01\"}")
                .when().post("/api/participants")
                .then()
                .statusCode(200)
                .extract().path("id");
        Long id = idInt.longValue();

        // Get as team member
        given()
                .when().get("/api/team/participants/" + id)
                .then()
                .statusCode(200)
                .body("id", is(idInt))
                .body("firstname", is("John"))
                .body("lastname", is("Doe"))
                .body("user", notNullValue())
                .body("healthStats", anyOf(nullValue(), notNullValue()))
                .body("campStats", anyOf(nullValue(), notNullValue()))
                .body("intoleranceSelections", notNullValue());
    }

    @Test
    @TestSecurity(user = "normal-user", roles = {"user"})
    public void testGetParticipantByIdUnauthorized() {
        given()
                .when().get("/api/team/participants/1")
                .then()
                .statusCode(403);
    }
}
