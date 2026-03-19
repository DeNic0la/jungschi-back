package ch.denic0la.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class ParticipantControllerTest {

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-oidc-sub"),
            @Claim(key = "preferred_username", value = "testuser")
    })
    public void testParticipantCrud() {
        // Create
        Object id = given()
                .contentType("application/json")
                .body("{\"firstname\": \"John\", \"lastname\": \"Doe\", \"dateOfBirth\": \"2000-01-01\"}")
                .when().post("/api/participants")
                .then()
                .statusCode(200)
                .body("firstname", is("John"))
                .body("lastname", is("Doe"))
                .body("dateOfBirth", is("2000-01-01"))
                .body("id", notNullValue())
                .extract().path("id");

        // Get All
        given()
                .when().get("/api/participants")
                .then()
                .statusCode(200)
                .body("$.size()", is(1))
                .body("[0].firstname", is("John"))
                .body("[0].id", is(id));

        // Update
        given()
                .contentType("application/json")
                .body("{\"firstname\": \"Jane\", \"lastname\": \"Doe\", \"dateOfBirth\": \"2000-01-01\"}")
                .when().put("/api/participants/" + id)
                .then()
                .statusCode(200)
                .body("firstname", is("Jane"))
                .body("id", is(id));

        // Delete
        given()
                .when().delete("/api/participants/" + id)
                .then()
                .statusCode(204);

        // Get All (empty)
        given()
                .when().get("/api/participants")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    @Test
    @TestSecurity(user = "other-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "user2"),
            @Claim(key = "preferred_username", value = "user2")
    })
    public void testIsolation() {
        given()
                .when().get("/api/participants")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }
}
