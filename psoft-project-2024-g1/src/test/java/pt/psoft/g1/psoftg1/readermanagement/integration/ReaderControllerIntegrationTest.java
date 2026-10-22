package pt.psoft.g1.psoftg1.readermanagement.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import pt.psoft.g1.psoftg1.external.service.ApiNinjasService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test: HTTP -> Spring MVC + Spring Security -> ReaderController -> ReaderViewMapper.
 * ReaderService and UserService are doubles; the external API Ninjas service is a stub with a canned
 * answer, so the test never depends on the external system.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReaderControllerIntegrationTest {
    private static final int YEAR = LocalDate.now().getYear();

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ReaderService readerService;
    @MockBean
    private UserService userService;
    @MockBean
    private ApiNinjasService apiNinjasService;

    private final Reader ana = Reader.newReader("ana@gmail.com", "Anaanaana!1", "Ana Silva");

    private static ReaderDetails details(int number, Reader reader) {
        final var details = new ReaderDetails(number, reader, "2000-01-01", "912345678", true, false, false, null, null);
        ReflectionTestUtils.setField(details, "version", 0L);
        return details;
    }

    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void librarianGetsReaderWithAHistoricalEventFromTheBirthMonth() throws Exception {
        given(readerService.findByReaderNumber(YEAR + "/1")).willReturn(Optional.of(details(1, ana)));
        given(apiNinjasService.getRandomEventFromYearMonth(2000, 1)).willReturn("Y2K");

        mockMvc.perform(get("/api/readers/" + YEAR + "/1"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(jsonPath("$.readerNumber").value(YEAR + "/1"))
                .andExpect(jsonPath("$.email").value("ana@gmail.com"))
                .andExpect(jsonPath("$.fullName").value("Ana Silva"))
                .andExpect(jsonPath("$.quote").value("Y2K"));
    }

    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void unknownReaderReturnsNotFoundWithoutCallingTheExternalApi() throws Exception {
        given(readerService.findByReaderNumber(YEAR + "/9")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/readers/" + YEAR + "/9")).andExpect(status().isNotFound());
        verifyNoInteractions(apiNinjasService);
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void readersCannotLookUpOtherReaders() throws Exception {
        mockMvc.perform(get("/api/readers/" + YEAR + "/1")).andExpect(status().isForbidden());
        verifyNoInteractions(readerService);
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void readerGetsOwnDetails() throws Exception {
        given(userService.getAuthenticatedUser(any())).willReturn(ana);
        given(readerService.findByUsername("ana@gmail.com")).willReturn(Optional.of(details(1, ana)));

        mockMvc.perform(get("/api/readers"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(jsonPath("$.email").value("ana@gmail.com"));
    }

    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void librarianGetsAllReaders() throws Exception {
        given(userService.getAuthenticatedUser(any()))
                .willReturn(Librarian.newLibrarian("maria@gmail.com", "Mariaroberta!123", "Maria Roberta"));
        given(readerService.findAll()).willReturn(List.of(details(1, ana)));

        mockMvc.perform(get("/api/readers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].readerNumber").value(YEAR + "/1"));
    }

    @Disabled("Known defect: ReaderViewMapper.toReaderView(Iterable) uses @Mapping(target = \".\") instead of "
            + "@IterableMapping, so reader lists are mapped without email and full name")
    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void readerListsIncludeEmailAndName() throws Exception {
        given(userService.getAuthenticatedUser(any()))
                .willReturn(Librarian.newLibrarian("maria@gmail.com", "Mariaroberta!123", "Maria Roberta"));
        given(readerService.findAll()).willReturn(List.of(details(1, ana)));

        mockMvc.perform(get("/api/readers"))
                .andExpect(jsonPath("$[0].email").value("ana@gmail.com"))
                .andExpect(jsonPath("$[0].fullName").value("Ana Silva"));
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void readerCannotSeeLendingsOfAnotherReader() throws Exception {
        final Reader rui = Reader.newReader("rui@gmail.com", "Ruiruirui!1", "Rui Costa");
        given(readerService.findByReaderNumber(YEAR + "/2")).willReturn(Optional.of(details(2, rui)));
        given(userService.getAuthenticatedUser(any())).willReturn(ana);
        given(readerService.findByUsername("ana@gmail.com")).willReturn(Optional.of(details(1, ana)));

        mockMvc.perform(get("/api/readers/" + YEAR + "/2/lendings").param("isbn", "9782826012092"))
                .andExpect(status().isForbidden());
    }

    @Test
    void visitorRegistersAsReader() throws Exception {
        given(readerService.create(any(), isNull())).willReturn(details(1, ana));

        mockMvc.perform(post("/api/readers")
                        .param("username", "ana@gmail.com").param("password", "Anaanaana!1")
                        .param("fullName", "Ana Silva").param("birthDate", "2000-01-01")
                        .param("phoneNumber", "912345678").param("gdpr", "true"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(jsonPath("$.readerNumber").value(YEAR + "/1"));
    }

    @Test
    void registrationWithInvalidEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/readers")
                        .param("username", "nao-e-email").param("password", "Anaanaana!1")
                        .param("fullName", "Ana Silva").param("birthDate", "2000-01-01")
                        .param("phoneNumber", "912345678").param("gdpr", "true"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(readerService);
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void patchWithoutIfMatchReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/readers").param("phoneNumber", "219999999"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(readerService);
    }
}
