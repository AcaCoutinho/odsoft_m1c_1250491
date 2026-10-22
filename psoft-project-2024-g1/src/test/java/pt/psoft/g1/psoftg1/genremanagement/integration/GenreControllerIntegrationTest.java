package pt.psoft.g1.psoftg1.genremanagement.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test of the genre statistics API without doubles (HTTP -> controller -> service ->
 * repository -> H2). Each test runs in a transaction that is rolled back.
 * <p>Opaque box: boundary values of the month (1..12) and page limit (1..100), and invalid date ranges.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = Role.LIBRARIAN)
class GenreControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private BookRepository bookRepository;

    private static String averageLendingsRequest(int month, int limit) {
        return "{\"page\":{\"number\":1,\"limit\":" + limit + "},\"query\":{\"year\":2024,\"month\":" + month + "}}";
    }

    @Test
    void topGenresAreOrderedByNumberOfBooks() throws Exception {
        final Genre policial = genreRepository.save(new Genre("Policial"));
        final Genre poesia = genreRepository.save(new Genre("Poesia"));
        final Author author = authorRepository.save(new Author("Manuel Antonio Pina", "Jornalista", null));
        bookRepository.save(new Book("9782826012092", "O Inspetor Max", null, policial, List.of(author), null));
        bookRepository.save(new Book("9780306406157", "O Caso do Gato", null, policial, List.of(author), null));
        bookRepository.save(new Book("9789720706386", "Poemas", null, poesia, List.of(author), null));

        mockMvc.perform(get("/api/genres/top5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].genreView.genre").value("Policial"))
                .andExpect(jsonPath("$.items[0].bookCount").value(2))
                .andExpect(jsonPath("$.items[1].genreView.genre").value("Poesia"));
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void readersCannotSeeGenreStatistics() throws Exception {
        mockMvc.perform(get("/api/genres/top5")).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({"1,10", "12,10", "6,1", "6,100"})
    void averageLendingsAcceptsBoundaryValues(int month, int limit) throws Exception {
        mockMvc.perform(post("/api/genres/avgLendingsPerGenre")
                        .contentType(MediaType.APPLICATION_JSON).content(averageLendingsRequest(month, limit)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"0,10", "13,10", "6,0", "6,101"})
    void averageLendingsRejectsValuesOutsideTheBoundaries(int month, int limit) throws Exception {
        mockMvc.perform(post("/api/genres/avgLendingsPerGenre")
                        .contentType(MediaType.APPLICATION_JSON).content(averageLendingsRequest(month, limit)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({"2024/01/01,2024-12-31", "2024-12-31,2024-01-01"})
    void averageDurationRejectsInvalidRanges(String start, String end) throws Exception {
        mockMvc.perform(get("/api/genres/lendingsAverageDurationPerMonth").param("startDate", start).param("endDate", end))
                .andExpect(status().isBadRequest());
    }

    @Test
    void averageDurationWithoutLendingsIsNotFound() throws Exception {
        mockMvc.perform(get("/api/genres/lendingsAverageDurationPerMonth")
                        .param("startDate", "1990-01-01").param("endDate", "1990-12-31"))
                .andExpect(status().isNotFound());
    }
}
