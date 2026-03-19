package ch.denic0la.controller;

import ch.denic0la.model.GlobalDefinitions;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class GlobalDefinitionsControllerTest {

    @BeforeEach
    @Transactional
    public void setup() {
        GlobalDefinitions.deleteAll();

        createDefinition("Z-Label", "z-val", GlobalDefinitions.Category.FoodIntolerance);
        createDefinition("A-Label", "a-val", GlobalDefinitions.Category.FoodIntolerance);
        createDefinition("Allergy-Label", "allergy-val", GlobalDefinitions.Category.AllergyDefinition);
    }

    private void createDefinition(String label, String value, GlobalDefinitions.Category category) {
        GlobalDefinitions def = new GlobalDefinitions();
        def.label = label;
        def.definitionValue = value;
        def.category = category;
        def.persist();
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-sub"),
            @Claim(key = "preferred_username", value = "testuser")
    })
    public void testGetFoodIntolerances() {
        given()
                .when().get("/api/global-definitions/food-intolerances")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].label", is("A-Label"))
                .body("[1].label", is("Z-Label"));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-sub"),
            @Claim(key = "preferred_username", value = "testuser")
    })
    public void testGetAllergies() {
        given()
                .when().get("/api/global-definitions/allergies")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].label", is("Allergy-Label"));
    }
}
