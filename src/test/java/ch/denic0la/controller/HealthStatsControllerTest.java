package ch.denic0la.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class HealthStatsControllerTest {

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "health-test-sub"),
            @Claim(key = "preferred_username", value = "testuser")
    })
    public void testHealthStatsCrudOperation() {
        // 1. Create a participant
        Long participantId = ((Number) given()
                .contentType("application/json")
                .body("{\"firstname\": \"John\", \"lastname\": \"Doe\", \"dateOfBirth\": \"2000-01-01\"}")
                .when().post("/api/participants")
                .then()
                .statusCode(200)
                .extract().path("id")).longValue();

        // 2. Get (should be empty/null initially)
        given()
                .when().get("/api/participants/" + participantId + "/health-stats")
                .then()
                .statusCode(204); // JAX-RS returns 204 for null return

        // 3. Create/Update health stats
        given()
                .contentType("application/json")
                .body("{\"isHealthy\": true, \"healthyReason\": \"Feeling great\", \"excludedActivities\": \"None\"}")
                .when().put("/api/participants/" + participantId + "/health-stats")
                .then()
                .statusCode(200)
                .body("isHealthy", is(true))
                .body("healthyReason", is("Feeling great"))
                .body("excludedActivities", is("None"));

        // 4. Get again
        given()
                .when().get("/api/participants/" + participantId + "/health-stats")
                .then()
                .statusCode(200)
                .body("isHealthy", is(true))
                .body("healthyReason", is("Feeling great"));

        // 5. Update again
        given()
                .contentType("application/json")
                .body("{\"isHealthy\": false, \"healthyReason\": \"Broken leg\", \"excludedActivities\": \"Hiking\"}")
                .when().put("/api/participants/" + participantId + "/health-stats")
                .then()
                .statusCode(200)
                .body("isHealthy", is(false))
                .body("healthyReason", is("Broken leg"))
                .body("excludedActivities", is("Hiking"));
    }

    @Test
    @TestSecurity(user = "other-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "user2"),
            @Claim(key = "preferred_username", value = "user2")
    })
    public void testHealthStatsIsolation() {
        given()
                .when().get("/api/participants/999/health-stats")
                .then()
                .statusCode(404);
    }
}
