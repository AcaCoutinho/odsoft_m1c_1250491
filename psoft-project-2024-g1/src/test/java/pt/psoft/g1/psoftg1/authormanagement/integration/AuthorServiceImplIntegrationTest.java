package pt.psoft.g1.psoftg1.authormanagement.integration;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration test of AuthorService + AuthorMapper + AuthorRepository + JPA, without doubles.
 * The H2 in-memory database plays the role of a fake for the production database.
 * Each test runs in a transaction that is rolled back at the end.
 */
@SpringBootTest
@Transactional
class AuthorServiceImplIntegrationTest {
    @Autowired
    private AuthorService authorService;

    private Author createAuthor() {
        return authorService.create(new CreateAuthorRequest("Alexandra Lucas", "Escritora portuguesa", null, null));
    }

    @Test
    void createdAuthorCanBeFoundByNumberAndByNamePrefix() {
        final Author created = createAuthor();

        assertThat(created.getAuthorNumber()).isNotNull();
        assertThat(authorService.findByAuthorNumber(created.getAuthorNumber())).contains(created);
        assertThat(authorService.findByName("Alexandra")).extracting(Author::getName).contains("Alexandra Lucas");
    }

    @Test
    void unknownAuthorIsNotFound() {
        assertThat(authorService.findByAuthorNumber(-1L)).isEmpty();
        assertThat(authorService.findByName("Ninguem Com Este Nome")).isEmpty();
    }

    @Test
    void partialUpdateIsPersisted() {
        final Author created = createAuthor();

        authorService.partialUpdate(created.getAuthorNumber(),
                new UpdateAuthorRequest("Nova bio", null, null, null), created.getVersion());

        assertThat(authorService.findByAuthorNumber(created.getAuthorNumber()))
                .get().extracting(Author::getBio).isEqualTo("Nova bio");
    }

    @Test
    void partialUpdateWithStaleVersionIsRejected() {
        final Author created = createAuthor();
        final var request = new UpdateAuthorRequest("Nova bio", null, null, null);

        assertThrows(StaleObjectStateException.class,
                () -> authorService.partialUpdate(created.getAuthorNumber(), request, created.getVersion() + 1));
    }

    @Test
    void partialUpdateOfUnknownAuthorIsRejected() {
        final var request = new UpdateAuthorRequest("Nova bio", null, null, null);

        assertThrows(NotFoundException.class, () -> authorService.partialUpdate(-1L, request, 0));
    }
}
