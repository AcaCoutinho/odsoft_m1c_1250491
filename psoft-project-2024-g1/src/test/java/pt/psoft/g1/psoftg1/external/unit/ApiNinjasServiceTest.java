package pt.psoft.g1.psoftg1.external.unit;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pt.psoft.g1.psoftg1.external.service.ApiNinjasService;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test of {@link ApiNinjasService}. The external REST API is replaced by a stub that answers
 * every request with a canned response, and records the requests (spy) so the URI can be checked.
 */
class ApiNinjasServiceTest {
    private final List<ClientRequest> requests = new ArrayList<>();

    private ApiNinjasService serviceAnswering(HttpStatus status, String json) {
        final WebClient webClient = WebClient.builder()
                .baseUrl("https://api.example.test/v1/")
                .exchangeFunction(request -> {
                    requests.add(request);
                    return Mono.just(ClientResponse.create(status)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(json)
                            .build());
                })
                .build();
        return new ApiNinjasService(webClient);
    }

    @Test
    void returnsTheEventOfTheOnlyResultAndAsksForTheGivenYearAndMonth() {
        final var service = serviceAnswering(HttpStatus.OK,
                "[{\"year\":\"2000\",\"month\":\"01\",\"day\":\"01\",\"event\":\"Y2K\"}]");

        assertEquals("Y2K", service.getRandomEventFromYearMonth(2000, 1));
        assertEquals(1, requests.size());
        assertTrue(requests.get(0).url().toString().endsWith("/v1/historicalevents?year=2000&month=1"));
    }

    @Test
    void returnsOneOfTheEvents() {
        final var service = serviceAnswering(HttpStatus.OK,
                "[{\"year\":\"2000\",\"month\":\"01\",\"day\":\"01\",\"event\":\"A\"},"
                        + "{\"year\":\"2000\",\"month\":\"01\",\"day\":\"02\",\"event\":\"B\"}]");

        assertTrue(Set.of("A", "B").contains(service.getRandomEventFromYearMonth(2000, 1)));
    }

    @Test
    void errorsFromTheExternalApiArePropagated() {
        final var service = serviceAnswering(HttpStatus.INTERNAL_SERVER_ERROR, "{}");

        assertThrows(WebClientResponseException.class, () -> service.getRandomEventFromYearMonth(2000, 1));
    }
}
