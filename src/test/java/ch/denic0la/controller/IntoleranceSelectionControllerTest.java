package ch.denic0la.controller;

import ch.denic0la.model.GlobalIntoleranceDefinitions;
import ch.denic0la.model.IntoleranceSelection;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

@QuarkusTest
public class IntoleranceSelectionControllerTest {

    private Long lactoseId;
    private Long glutenId;

    @BeforeEach
    @Transactional
    public void setup() {
        IntoleranceSelection.deleteAll();
        GlobalIntoleranceDefinitions.deleteAll();
        
        lactoseId = createDefinition("Lactose", "LAC", GlobalIntoleranceDefinitions.Category.FoodIntolerance);
        glutenId = createDefinition("Gluten", "GLU", GlobalIntoleranceDefinitions.Category.FoodIntolerance);
    }

    private Long createDefinition(String label, String value, GlobalIntoleranceDefinitions.Category category) {
        GlobalIntoleranceDefinitions def = new GlobalIntoleranceDefinitions();
        def.label = label;
        def.definitionValue = value;
        def.category = category;
        def.persistAndFlush();
        return def.id;
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "intolerance-test-sub"),
            @Claim(key = "preferred_username", value = "testuser"),
            @Claim(key = "email", value = "intolerance-test@example.com")
    })
    public void testIntoleranceSelectionCrud() {
        // 1. Create a participant
        Long participantId = ((Number) given()
                .contentType("application/json")
                .body("{\"firstname\": \"John\", \"lastname\": \"Doe\", \"dateOfBirth\": \"2000-01-01\"}")
                .when().post("/api/participants")
                .then()
                .statusCode(200)
                .extract().path("id")).longValue();

        // 2. GET (should be empty)
        given()
                .when().get("/api/participants/" + participantId + "/intolerance-selections")
                .then()
                .statusCode(200)
                .body("size()", is(0));

        // 3. PUT a global definition intolerance
        given()
                .contentType("application/json")
                .body(String.format("{\"intoleranceId\": %d, \"severity\": \"STRONG\", \"customText\": \"\"}", lactoseId))
                .when().put("/api/participants/" + participantId + "/intolerance-selections")
                .then()
                .statusCode(200)
                .body("intoleranceId", is(lactoseId.intValue()))
                .body("severity", is("STRONG"));

        // 4. PUT a custom intolerance
        given()
                .contentType("application/json")
                .body("{\"intoleranceId\": null, \"severity\": \"AFFECTED\", \"customText\": \"Mushrooms\"}")
                .when().put("/api/participants/" + participantId + "/intolerance-selections")
                .then()
                .statusCode(200)
                .body("intoleranceId", is(nullValue()))
                .body("customText", is("Mushrooms"))
                .body("severity", is("AFFECTED"));

        // 5. GET again (should have 2)
        given()
                .when().get("/api/participants/" + participantId + "/intolerance-selections")
                .then()
                .statusCode(200)
                .body("size()", is(2));

        // 6. Update existing global intolerance
        given()
                .contentType("application/json")
                .body(String.format("{\"intoleranceId\": %d, \"severity\": \"LIFE_THREATENING\", \"customText\": \"Be careful\"}", lactoseId))
                .when().put("/api/participants/" + participantId + "/intolerance-selections")
                .then()
                .statusCode(200)
                .body("intoleranceId", is(lactoseId.intValue()))
                .body("severity", is("LIFE_THREATENING"))
                .body("customText", is("Be careful"));

        // 7. GET again (still 2)
        given()
                .when().get("/api/participants/" + participantId + "/intolerance-selections")
                .then()
                .statusCode(200)
                .body("size()", is(2));

        // 8. DELETE global intolerance
        given()
                .queryParam("intoleranceId", lactoseId)
                .when().delete("/api/participants/" + participantId + "/intolerance-selections")
                .then()
                .statusCode(204);

        // 9. DELETE custom intolerance
        given()
                .when().delete("/api/participants/" + participantId + "/intolerance-selections")
                .then()
                .statusCode(204);

        // 10. GET again (should be empty)
        given()
                .when().get("/api/participants/" + participantId + "/intolerance-selections")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }
}
