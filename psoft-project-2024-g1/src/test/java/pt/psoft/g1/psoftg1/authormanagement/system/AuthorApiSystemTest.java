package pt.psoft.g1.psoftg1.authormanagement.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorView;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.web.reactive.function.BodyInserters.fromFormData;
import static pt.psoft.g1.psoftg1.testutils.ApiTestClient.*;

/**
 * System (end-to-end) test: the whole application runs on a random port with the bootstrap data,
 * and is used only through its public HTTP API, as a client would (real login, real JWT, real database).
 * No test doubles. All system tests share one in-memory database, separate from the other tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:system-test")
@ActiveProfiles("bootstrap")
class AuthorApiSystemTest {
    @Autowired
    private WebTestClient client;

    private AuthorView createAuthor(String librarianToken, String name, String bio) {
        return client.post().uri("/api/authors")
                .headers(h -> h.setBearerAuth(librarianToken))
                .body(fromFormData("name", name).with("bio", bio))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals(HttpHeaders.ETAG, "\"0\"")
                .expectBody(AuthorView.class)
                .returnResult().getResponseBody();
    }

    @Test
    void librarianRegistersAnAuthorAndReaderFindsIt() {
        final String librarian = login(client, LIBRARIAN, LIBRARIAN_PASSWORD);
        final String reader = login(client, READER, READER_PASSWORD);

        final AuthorView created = createAuthor(librarian, "Agustina Bessa Luis", "Romancista portuguesa");
        assertNotNull(created.getAuthorNumber());

        client.get().uri("/api/authors/{authorNumber}", created.getAuthorNumber())
                .headers(h -> h.setBearerAuth(reader))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Agustina Bessa Luis")
                .jsonPath("$.bio").isEqualTo("Romancista portuguesa");

        client.get().uri(uri -> uri.path("/api/authors").queryParam("name", "Agustina").build())
                .headers(h -> h.setBearerAuth(reader))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].authorNumber").isEqualTo(created.getAuthorNumber());
    }

    @Test
    void concurrentUpdatesAreDetectedWithIfMatch() {
        final String librarian = login(client, LIBRARIAN, LIBRARIAN_PASSWORD);
        final AuthorView created = createAuthor(librarian, "Sophia de Mello Breyner", "Poetisa");

        client.patch().uri("/api/authors/{authorNumber}", created.getAuthorNumber())
                .headers(h -> h.setBearerAuth(librarian))
                .header(HttpHeaders.IF_MATCH, "\"0\"")
                .body(fromFormData("bio", "Poetisa portuguesa"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ETAG, "\"1\"")
                .expectBody().jsonPath("$.bio").isEqualTo("Poetisa portuguesa");

        // a second client still holding version 0 must not overwrite the first update
        client.patch().uri("/api/authors/{authorNumber}", created.getAuthorNumber())
                .headers(h -> h.setBearerAuth(librarian))
                .header(HttpHeaders.IF_MATCH, "\"0\"")
                .body(fromFormData("bio", "Outra bio"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void accessRulesAreEnforced() {
        client.get().uri("/api/authors/1").exchange().expectStatus().isUnauthorized();

        final String reader = login(client, READER, READER_PASSWORD);
        client.post().uri("/api/authors")
                .headers(h -> h.setBearerAuth(reader))
                .body(fromFormData("name", "Ana").with("bio", "Bio"))
                .exchange()
                .expectStatus().isForbidden();

        client.post().uri("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", READER, "password", "wrong-password"))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
