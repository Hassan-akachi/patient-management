import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class PatientIntegrationTest {

    @BeforeAll
     static void setUp() {
        RestAssured.baseURI = "http://localhost:4004";
    }

    @Test
// Marks this method as a test case to be executed by the testing framework (e.g., JUnit).
    public void shouldReturnPatientsWithValidToken() {

        // 1. Arrange - Setup test data
        String loginPayload = """
            {
            "email": "testuser@test.com",
            "password": "password123"
            }
            """;
        // loginPayload holds the JSON body needed to authenticate.

        // 2. Act (Login) - Make API call to the login endpoint to get a token
        String token = given()
                .contentType("application/json")
                .body(loginPayload)
                .when()
                .post("/auth/login")
                .then()
                // 3. Assert (Login) - Verify response status and extract the token
                .statusCode(200)
                .extract()
                .jsonPath()
                .get("token");

        // 4. Act (Resource Access) - Use the extracted token to access a protected resource

        given()
                // Set the Authorization header with the 'Bearer' scheme and the retrieved token.
                .header("Authorization", "Bearer " + token)
                .when()
                .get("api/patients")
                .then()
                // 5. Assert (Resource Access) - Verify the request succeeded and the body contains data.
                .statusCode(200)
                // Verify that the JSON field "patients" is present and not null.
                .body("patients", notNullValue());
    }
}
