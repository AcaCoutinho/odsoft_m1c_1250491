package pt.psoft.g1.psoftg1.readermanagement.system;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.web.reactive.function.BodyInserters.fromFormData;
import static pt.psoft.g1.psoftg1.testutils.ApiTestClient.*;

/**
 * System (end-to-end) test of reader self-registration and profile management, through the public
 * HTTP API only. No test doubles.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:system-test")
@ActiveProfiles("bootstrap")
class ReaderApiSystemTest {
    @Autowired
    private WebTestClient client;

    private static BodyInserters.FormInserter<String> registration(String username, String fullName, boolean gdpr) {
        return fromFormData("username", username)
                .with("password", NEW_READER_PASSWORD)
                .with("fullName", fullName)
                .with("birthDate", "2000-01-01")
                .with("phoneNumber", "912345678")
                .with("gdpr", String.valueOf(gdpr));
    }

    @Test
    void visitorRegistersLogsInAndUpdatesTheProfile() {
        final String username = uniqueEmail("leitor");
        final String readerNumber = registerReader(client, username, "Leitor Teste");
        final String token = login(client, username, NEW_READER_PASSWORD);

        client.get().uri("/api/readers")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ETAG, "\"0\"")
                .expectBody()
                .jsonPath("$.readerNumber").isEqualTo(readerNumber)
                .jsonPath("$.email").isEqualTo(username)
                .jsonPath("$.phoneNumber").isEqualTo("912345678");

        client.patch().uri("/api/readers")
                .headers(h -> h.setBearerAuth(token))
                .header(HttpHeaders.IF_MATCH, "\"0\"")
                .body(fromFormData("phoneNumber", "219999999"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ETAG, "\"1\"")
                .expectBody().jsonPath("$.phoneNumber").isEqualTo("219999999");

        // a second client still holding version 0 must not overwrite the first update
        client.patch().uri("/api/readers")
                .headers(h -> h.setBearerAuth(token))
                .header(HttpHeaders.IF_MATCH, "\"0\"")
                .body(fromFormData("phoneNumber", "938888888"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Disabled("Known defect: the reader number comes from the count of users created this year, which ignores "
            + "the bootstrap readers (created_at moved to 2024), so new readers reuse their numbers and lendings")
    @Test
    void registeredReaderGetsAnUnusedReaderNumber() {
        final String readerNumber = registerReader(client, uniqueEmail("numero"), "Leitor Teste");

        final String readers = client.get().uri("/api/readers")
                .headers(h -> h.setBearerAuth(login(client, LIBRARIAN, LIBRARIAN_PASSWORD)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        final List<String> numbers = JsonPath.read(readers, "$[*].readerNumber");
        assertEquals(1, Collections.frequency(numbers, readerNumber));
    }

    @Test
    void usernameCanOnlyBeRegisteredOnce() {
        final String username = uniqueEmail("duplicado");
        registerReader(client, username, "Leitor Teste");

        client.post().uri("/api/readers").body(registration(username, "Outro Leitor", true))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void namesWithForbiddenWordsAreRejected() {
        client.post().uri("/api/readers").body(registration(uniqueEmail("proibido"), "Bruce Batman", true))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void registrationRequiresGdprConsent() {
        client.post().uri("/api/readers").body(registration(uniqueEmail("semgdpr"), "Leitor Teste", false))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
