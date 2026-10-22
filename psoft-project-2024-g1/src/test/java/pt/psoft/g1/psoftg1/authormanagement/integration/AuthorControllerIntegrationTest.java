package pt.psoft.g1.psoftg1.authormanagement.integration;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test: HTTP request -> Spring MVC + Spring Security (real SecurityConfig) -> AuthorController
 * -> AuthorViewMapper. AuthorService is replaced by a double so that the rest of the system is isolated.
 * <p>Covers the HTTP contract (status codes, ETag, JSON body), the access rules and request validation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthorControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AuthorService authorService;

    private static Author author() {
        final Author author = new Author("Manuel Antonio Pina", "Jornalista e escritor", null);
        ReflectionTestUtils.setField(author, "authorNumber", 1L);
        return author;
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void readerGetsAuthorWithItsVersionAsEtag() throws Exception {
        given(authorService.findByAuthorNumber(1L)).willReturn(Optional.of(author()));

        mockMvc.perform(get("/api/authors/1"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(jsonPath("$.authorNumber").value(1))
                .andExpect(jsonPath("$.name").value("Manuel Antonio Pina"))
                .andExpect(jsonPath("$.bio").value("Jornalista e escritor"))
                .andExpect(jsonPath("$._links.author").value("http://localhost/api/authors/1"));
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void unknownAuthorReturnsNotFound() throws Exception {
        given(authorService.findByAuthorNumber(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/authors/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void nonNumericAuthorNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/authors/abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(authorService);
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/authors/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(authorService);
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void readerCannotCreateAuthors() throws Exception {
        mockMvc.perform(multipart("/api/authors").param("name", "Ana").param("bio", "Bio"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(authorService);
    }

    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void librarianCreatesAuthor() throws Exception {
        given(authorService.create(any())).willReturn(author());

        mockMvc.perform(multipart("/api/authors")
                        .param("name", "Manuel Antonio Pina")
                        .param("bio", "Jornalista e escritor")
                        .param("photoURI", "injected-by-client.jpg"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(jsonPath("$.name").value("Manuel Antonio Pina"));

        final ArgumentCaptor<CreateAuthorRequest> request = ArgumentCaptor.forClass(CreateAuthorRequest.class);
        verify(authorService).create(request.capture());
        assertEquals("Manuel Antonio Pina", request.getValue().getName());
        // the controller must ignore a photo URI sent by the client without a file
        assertNull(request.getValue().getPhotoURI());
    }

    // Boundary value analysis on CreateAuthorRequest: @Size(min = 1, max = 150) on name
    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void nameAboveMaxLengthReturnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/authors").param("name", "a".repeat(151)).param("bio", "Bio"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authorService);
    }

    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void patchWithoutIfMatchReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/authors/1").param("bio", "Nova bio"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authorService);
    }

    @Test
    @WithMockUser(roles = Role.LIBRARIAN)
    void patchWithStaleVersionReturnsConflict() throws Exception {
        given(authorService.partialUpdate(eq(1L), any(), eq(3L)))
                .willThrow(new StaleObjectStateException(Author.class.getName(), 1L));

        mockMvc.perform(patch("/api/authors/1").header(HttpHeaders.IF_MATCH, "\"3\"").param("bio", "Nova bio"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = Role.READER)
    void top5WithoutLendingsReturnsNotFound() throws Exception {
        given(authorService.findTopAuthorByLendings()).willReturn(List.of());

        mockMvc.perform(get("/api/authors/top5")).andExpect(status().isNotFound());
    }
}
