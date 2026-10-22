package pt.psoft.g1.psoftg1.lendingmanagement.system;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.Map;

import static pt.psoft.g1.psoftg1.testutils.ApiTestClient.*;

/**
 * System (end-to-end) test of the lending life cycle across the reader, book and lending modules:
 * the librarian lends a book and the reader returns it. Only the public HTTP API is used, with the
 * bootstrap data. No test doubles.
 * <p>Uses bootstrap readers 7 and 8, which have no lendings. Newly registered readers are not used
 * because they get reader numbers already taken (see ReaderApiSystemTest.registeredReaderGetsAnUnusedReaderNumber).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:system-test")
@ActiveProfiles("bootstrap")
class LendingApiSystemTest {
    private static final String ISBN = "9789720706386";
    private static final String ANTONIO = "antonio@gmail.com";
    private static final int YEAR = LocalDate.now().getYear();

    @Autowired
    private WebTestClient client;

    private WebTestClient.ResponseSpec lend(String librarianToken, String readerNumber) {
        return client.post().uri("/api/lendings")
                .headers(h -> h.setBearerAuth(librarianToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("isbn", ISBN, "readerNumber", readerNumber))
                .exchange();
    }

    private WebTestClient.ResponseSpec returnLending(String readerToken, String lendingNumber, String version) {
        return client.patch().uri("/api/lendings/" + lendingNumber)
                .headers(h -> h.setBearerAuth(readerToken))
                .header(HttpHeaders.IF_MATCH, "\"" + version + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("commentary", "Gostei muito"))
                .exchange();
    }

    @Test
    void readerBorrowsAndReturnsABook() {
        final String librarian = login(client, LIBRARIAN, LIBRARIAN_PASSWORD);
        final String reader = login(client, ANTONIO, "Antoniocosta!123");

        final String body = lend(librarian, YEAR + "/7")
                .expectStatus().isCreated()
                .expectHeader().valueEquals(HttpHeaders.ETAG, "\"0\"")
                .expectBody(String.class).returnResult().getResponseBody();
        final String lendingNumber = JsonPath.read(body, "$.lendingNumber");

        client.get().uri("/api/lendings/" + lendingNumber)
                .headers(h -> h.setBearerAuth(reader))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.daysUntilReturn").isEqualTo(15)
                .jsonPath("$.returnedDate").doesNotExist();

        // another reader cannot see this lending
        client.get().uri("/api/lendings/" + lendingNumber)
                .headers(h -> h.setBearerAuth(login(client, READER, READER_PASSWORD)))
                .exchange()
                .expectStatus().isForbidden();

        returnLending(reader, lendingNumber, "0")
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ETAG, "\"1\"")
                .expectBody()
                .jsonPath("$.returnedDate").isEqualTo(LocalDate.now().toString())
                .jsonPath("$.fineValueInCents").doesNotExist();

        // a lending can only be returned once
        returnLending(reader, lendingNumber, "1").expectStatus().isBadRequest();
    }

    @Test
    void readerCannotHaveMoreThanThreeOutstandingLendings() {
        final String librarian = login(client, LIBRARIAN, LIBRARIAN_PASSWORD);
        final String andre = YEAR + "/8";

        lend(librarian, andre).expectStatus().isCreated();
        lend(librarian, andre).expectStatus().isCreated();
        lend(librarian, andre).expectStatus().isCreated();
        lend(librarian, andre).expectStatus().isForbidden();
    }

    @Test
    void readersCannotCreateLendings() {
        final String reader = login(client, READER, READER_PASSWORD);

        client.post().uri("/api/lendings")
                .headers(h -> h.setBearerAuth(reader))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("isbn", ISBN, "readerNumber", YEAR + "/1"))
                .exchange()
                .expectStatus().isForbidden();
    }
}
