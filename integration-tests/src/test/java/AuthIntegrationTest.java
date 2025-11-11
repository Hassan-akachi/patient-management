import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;


public class AuthIntegrationTest {

    // Set base URL for all test requests
    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost:4004";
    }

    @Test
    public void shouldReturnOKWithValidToken() {
        // 1. Arrange - Setup test data
        String loginPayload = """
                {
                "email": "testuser@test.com",
                "password": "password123"
                }
                """;

        // 2. Act - Make API call to login endpoint
        Response response = given()
                .contentType("application/json")
                .body(loginPayload)
                .when()
                .post("/auth/login")
                .then()
                // 3. Assert - Verify response status and token
                .statusCode(200)
                .body("token", notNullValue())
                .extract()
                .response();

        // Print token for debugging purposes
        System.out.println("Generated Token: " + response.jsonPath().getString("token"));
    }

    @Test
    public void shouldReturnUnauthorizedInvalidLogin() {
        // 1. Arrange - Setup test data
        String loginPayload = """
                {
                "email": "hassan@test.com",
                "password": "wrongpassword123"
                }
                """;

        // 2. Act - Make API call to login endpoint
                given()
                .contentType("application/json")
                .body(loginPayload)
                .when()
                .post("/auth/login")
                .then()
                // 3. Assert - Verify response status and token
                .statusCode(401);

    }
}

