package pt.psoft.g1.psoftg1.testutils;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.web.reactive.function.BodyInserters.fromFormData;

/**
 * Steps shared by the system tests, done only through the public HTTP API.
 */
public final class ApiTestClient {
    public static final String LIBRARIAN = "maria@gmail.com";
    public static final String LIBRARIAN_PASSWORD = "Mariaroberta!123";
    public static final String READER = "manuel@gmail.com";
    public static final String READER_PASSWORD = "Manuelino123!";
    public static final String NEW_READER_PASSWORD = "Leitor!2024";

    private ApiTestClient() {
    }

    /** Logs in and returns the JWT to use as bearer token. */
    public static String login(WebTestClient client, String username, String password) {
        final String token = client.post().uri("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", username, "password", password))
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertNotNull(token);
        return token;
    }

    /** A username that no other test uses. */
    public static String uniqueEmail(String prefix) {
        return prefix + "." + System.nanoTime() + "@mail.pt";
    }

    /** Registers a new reader (public endpoint) and returns its reader number. */
    public static String registerReader(WebTestClient client, String username, String fullName) {
        final String body = client.post().uri("/api/readers")
                .body(fromFormData("username", username)
                        .with("password", NEW_READER_PASSWORD)
                        .with("fullName", fullName)
                        .with("birthDate", "2000-01-01")
                        .with("phoneNumber", "912345678")
                        .with("gdpr", "true"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult().getResponseBody();
        return JsonPath.read(body, "$.readerNumber");
    }
}
