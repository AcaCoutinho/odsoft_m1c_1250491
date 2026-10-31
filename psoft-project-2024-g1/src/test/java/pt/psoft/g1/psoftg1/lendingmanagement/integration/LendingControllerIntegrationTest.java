package pt.psoft.g1.psoftg1.lendingmanagement.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test: HTTP -> Spring MVC + Spring Security -> LendingController -> LendingViewMapper.
 * LendingService, ReaderService and UserService are doubles, so only the web layer is integrated.
 * <p>Opaque box on the HTTP contract: status codes, ETag/If-Match, JSON body, access rules (librarian
 * lends, only the reader of a lending sees and returns it) and request validation (boundary values of
 * the ISBN length, 10..13).
 */
@SpringBootTest
@AutoConfigureMockMvc
class LendingControllerIntegrationTest {
    private static final int YEAR = LocalDate.now().getYear();
    private static final String ISBN = "9782826012092";
    private static final String LENDING = YEAR + "/1";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private LendingService lendingService;
    @MockBean
    private ReaderService readerService;
    @MockBean
    private UserService userService;

    private final Reader ana = Reader.newReader("ana@gmail.com", "Anaanaana!1", "Ana Silva");
    private final Reader rui = Reader.newReader("rui@gmail.com", "Ruiruirui!1", "Rui Costa");
    private final ReaderDetails anaDetails = new ReaderDetails(1, ana, "2000-01-01", "912345678", true, false, false, null, null);
    private final ReaderDetails ruiDetails = new ReaderDetails(2, rui, "2000-01-01", "912345679", true, false, false, null, null);

    private Lending lendingOf(ReaderDetails reader) {
        final Author author = new Author("Manuel Antonio Pina", "Jornalista e escritor", null);
        final Book book = new Book(ISBN, "O Inspetor Max", null, new Genre("Infantil"), List.of(author), null);
        final Lending lending = new Lending(book, reader, 1, 15, 200);
        ReflectionTestUtils.setField(lending, "version", 0L);
        return lending;
    }

    private static String createBody(String isbn) {
        return "{\"isbn\":\"" + isbn + "\",\"readerNumber\":\"" + YEAR + "/1\"}";
    }

    private void givenLoggedIn(Reader user, ReaderDetails details) {
        given(userService.getAuthenticatedUser(any())).willReturn(user);
        given(readerService.findByUsername(user.getUsername())).willReturn(Optional.of(details));
    }

    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void librarianLendsABook() throws Exception {
        given(lendingService.create(any())).willReturn(lendingOf(anaDetails));

        mockMvc.perform(post("/api/lendings").contentType(MediaType.APPLICATION_JSON).content(createBody(ISBN)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/api/lendings/" + LENDING)))
                .andExpect(jsonPath("$.lendingNumber").value(LENDING))
                .andExpect(jsonPath("$.bookTitle").value("O Inspetor Max"))
                .andExpect(jsonPath("$.limitDate").value(LocalDate.now().plusDays(15).toString()));
    }

    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void lendingRefusedByTheBusinessRulesIsForbidden() throws Exception {
        given(lendingService.create(any())).willThrow(new LendingForbiddenException("Reader has three outstanding lendings"));

        mockMvc.perform(post("/api/lendings").contentType(MediaType.APPLICATION_JSON).content(createBody(ISBN)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void isbnWithTenOrThirteenCharactersIsAccepted() throws Exception {
        given(lendingService.create(any())).willReturn(lendingOf(anaDetails));

        mockMvc.perform(post("/api/lendings").contentType(MediaType.APPLICATION_JSON).content(createBody("0306406152")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/lendings").contentType(MediaType.APPLICATION_JSON).content(createBody(ISBN)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void isbnWithNineOrFourteenCharactersIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/lendings").contentType(MediaType.APPLICATION_JSON).content(createBody("030640615")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/lendings").contentType(MediaType.APPLICATION_JSON).content(createBody("97828260120921")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(lendingService);
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void readersCannotLendBooks() throws Exception {
        mockMvc.perform(post("/api/lendings").contentType(MediaType.APPLICATION_JSON).content(createBody(ISBN)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(lendingService);
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/lendings/" + LENDING)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void readerSeesOwnLending() throws Exception {
        given(lendingService.findByLendingNumber(LENDING)).willReturn(Optional.of(lendingOf(anaDetails)));
        givenLoggedIn(ana, anaDetails);

        mockMvc.perform(get("/api/lendings/" + LENDING))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(jsonPath("$.lendingNumber").value(LENDING))
                .andExpect(jsonPath("$.daysUntilReturn").value(15));
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void readerCannotSeeTheLendingOfAnotherReader() throws Exception {
        given(lendingService.findByLendingNumber(LENDING)).willReturn(Optional.of(lendingOf(anaDetails)));
        givenLoggedIn(rui, ruiDetails);

        mockMvc.perform(get("/api/lendings/" + LENDING)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void librarianSeesAnyLending() throws Exception {
        given(lendingService.findByLendingNumber(LENDING)).willReturn(Optional.of(lendingOf(anaDetails)));
        given(userService.getAuthenticatedUser(any()))
                .willReturn(Librarian.newLibrarian("maria@gmail.com", "Mariaroberta!123", "Maria Roberta"));

        mockMvc.perform(get("/api/lendings/" + LENDING)).andExpect(status().isOk());
        verifyNoInteractions(readerService);
    }

    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void unknownLendingIsNotFound() throws Exception {
        given(lendingService.findByLendingNumber(LENDING)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/lendings/" + LENDING)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void returnWithoutIfMatchIsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/lendings/" + LENDING).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(lendingService);
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void readerReturnsOwnLendingWithTheCurrentVersion() throws Exception {
        final Lending lending = lendingOf(anaDetails);
        given(lendingService.findByLendingNumber(LENDING)).willReturn(Optional.of(lending));
        givenLoggedIn(ana, anaDetails);
        lending.setReturned(0, "Gostei");
        given(lendingService.setReturned(eq(LENDING), any(), eq(0L))).willReturn(lending);

        mockMvc.perform(patch("/api/lendings/" + LENDING).header(HttpHeaders.IF_MATCH, "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"commentary\":\"Gostei\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedDate").value(LocalDate.now().toString()));
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void readerCannotReturnTheLendingOfAnotherReader() throws Exception {
        given(lendingService.findByLendingNumber(LENDING)).willReturn(Optional.of(lendingOf(anaDetails)));
        givenLoggedIn(rui, ruiDetails);

        mockMvc.perform(patch("/api/lendings/" + LENDING).header(HttpHeaders.IF_MATCH, "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verify(lendingService, never()).setReturned(any(), any(), anyLong());
    }

    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void overdueWithoutLendingsIsNotFound() throws Exception {
        given(lendingService.getOverdue(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lendings/overdue").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":1,\"limit\":10}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void readersCannotSeeOverdueLendings() throws Exception {
        mockMvc.perform(get("/api/lendings/overdue").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":1,\"limit\":10}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(lendingService);
    }
}
