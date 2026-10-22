package pt.psoft.g1.psoftg1.bookmanagement.system;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.web.reactive.function.BodyInserters.fromFormData;
import static pt.psoft.g1.psoftg1.testutils.ApiTestClient.*;

/**
 * System (end-to-end) test of the book catalogue, through the public HTTP API only, with the
 * bootstrap data (authors and genres). No test doubles.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:system-test")
@ActiveProfiles("bootstrap")
class BookApiSystemTest {
    private static final String NEW_ISBN = "9780306406157";

    @Autowired
    private WebTestClient client;

    private String authorNumber(String token, String name) {
        final String body = client.get().uri(uri -> uri.path("/api/authors").queryParam("name", name).build())
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        return JsonPath.read(body, "$.items[0].authorNumber").toString();
    }

    @Test
    void librarianAddsABookAndReaderFindsIt() {
        final String librarian = login(client, LIBRARIAN, LIBRARIAN_PASSWORD);
        final String reader = login(client, READER, READER_PASSWORD);

        client.put().uri("/api/books/{isbn}", NEW_ISBN)
                .headers(h -> h.setBearerAuth(librarian))
                .body(fromFormData("title", "Livro de Testes")
                        .with("genre", "Fantasia")
                        .with("description", "Um livro criado pelo teste de sistema")
                        .with("authors", authorNumber(librarian, "Manuel Antonio Pina")))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals(HttpHeaders.ETAG, "\"0\"");

        client.get().uri("/api/books/{isbn}", NEW_ISBN)
                .headers(h -> h.setBearerAuth(reader))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Livro de Testes")
                .jsonPath("$.genre").isEqualTo("Fantasia")
                .jsonPath("$.authors[0]").isEqualTo("Manuel Antonio Pina");

        client.post().uri("/api/books/search")
                .headers(h -> h.setBearerAuth(reader))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"page\":{\"number\":1,\"limit\":10},"
                        + "\"query\":{\"title\":\"Livro de Testes\",\"genre\":\"Fantasia\",\"authorName\":\"Manuel\"}}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].isbn").isEqualTo(NEW_ISBN);
    }

    @Test
    void bookWithUnknownGenreIsRejected() {
        final String librarian = login(client, LIBRARIAN, LIBRARIAN_PASSWORD);

        client.put().uri("/api/books/{isbn}", "9789723716160")
                .headers(h -> h.setBearerAuth(librarian))
                .body(fromFormData("title", "Sem genero").with("genre", "Culinaria")
                        .with("authors", authorNumber(librarian, "Manuel Antonio Pina")))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void readersCannotAddBooks() {
        final String reader = login(client, READER, READER_PASSWORD);

        client.put().uri("/api/books/{isbn}", "9789723716160")
                .headers(h -> h.setBearerAuth(reader))
                .body(fromFormData("title", "Proibido").with("genre", "Fantasia"))
                .exchange()
                .expectStatus().isForbidden();
    }
}
