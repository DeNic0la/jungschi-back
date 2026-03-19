package ch.denic0la;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class HealthCheckTest {

    @Test
    public void testLiveness() {
        given()
                .when().get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks.name", org.hamcrest.Matchers.hasItem("alive"));
    }

    @Test
    public void testReadiness() {
        given()
                .when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks.name", org.hamcrest.Matchers.hasItem("database-migration"));
    }

    @Test
    public void testStartup() {
        given()
                .when().get("/q/health/started")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks.name", org.hamcrest.Matchers.hasItem("application-started"));
    }
}
