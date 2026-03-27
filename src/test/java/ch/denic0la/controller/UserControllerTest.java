package ch.denic0la.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.UserInfo;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class UserControllerTest {

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-oidc-sub"),
            @Claim(key = "preferred_username", value = "testuser"),
            @Claim(key = "email", value = "test@example.com")
    })
    public void testMeEndpoint() {
        given()
                .when().get("/api/users/me")
                .then()
                .statusCode(200)
                .body("username", is("testuser"))
                .body("email", is("test@example.com"))
                .body("oidcSubject", is("test-oidc-sub"))
                .body("id", is("test-oidc-sub"));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-oidc-sub"),
            @Claim(key = "preferred_username", value = "testuser"),
            @Claim(key = "email", value = "test@example.com"),
            @Claim(key = "given_name", value = "First"),
            @Claim(key = "family_name", value = "Last"),
            @Claim(key = "phone_number", value = "123456789")
    })
    public void testUpdateMe() {
        given()
                .contentType("application/json")
                .body("{\"firstName\": \"NewFirst\", \"lastName\": \"NewLast\", \"phoneNumber\": \"987654321\", \"address\": \"NewAddress\"}")
                .when().put("/api/users/me")
                .then()
                .statusCode(200)
                .body("firstName", is("NewFirst"))
                .body("lastName", is("NewLast"))
                .body("phoneNumber", is("987654321"))
                .body("address", is("NewAddress"));
    }
}
