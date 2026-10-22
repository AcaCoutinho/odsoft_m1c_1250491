package pt.psoft.g1.psoftg1.bookmanagement.integration;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.SearchBooksQuery;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.shared.services.Page;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration test of the book queries (JPQL, native SQL and Criteria API) against the H2 database.
 */
@DataJpaTest
class BookRepositoryIntegrationTest {
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private BookRepository bookRepository;

    private Author pina;
    private Author sophia;

    @BeforeEach
    void setUp() {
        final Genre fantasia = entityManager.persist(new Genre("Fantasia"));
        final Genre romance = entityManager.persist(new Genre("Romance"));
        pina = entityManager.persist(new Author("Manuel Antonio Pina", "Jornalista", null));
        sophia = entityManager.persist(new Author("Sophia de Mello Breyner", "Poetisa", null));
        entityManager.persist(new Book("9782826012092", "O Inspetor Max", null, fantasia, List.of(pina), null));
        entityManager.persist(new Book("9780306406157", "O Rapaz de Bronze", null, romance, List.of(pina, sophia), null));
        entityManager.persist(new Book("9789720706386", "Contos Exemplares", null, romance, List.of(sophia), null));
        entityManager.flush();
    }

    private static List<String> titles(List<Book> books) {
        return books.stream().map(b -> b.getTitle().toString()).toList();
    }

    @Test
    void findByIsbnReturnsOnlyThatBook() {
        assertThat(bookRepository.findByIsbn("9780306406157")).get()
                .extracting(b -> b.getTitle().toString()).isEqualTo("O Rapaz de Bronze");
        assertThat(bookRepository.findByIsbn("9789723716160")).isEmpty();
    }

    @Test
    void findByTitleMatchesAnyPartOfTheTitle() {
        assertThat(titles(bookRepository.findByTitle("Bronze"))).containsExactly("O Rapaz de Bronze");
    }

    @Test
    void findByGenreMatchesAnyPartOfTheGenre() {
        assertThat(titles(bookRepository.findByGenre("oman")))
                .containsExactlyInAnyOrder("O Rapaz de Bronze", "Contos Exemplares");
    }

    @Test
    void findByAuthorNameUsesTheGivenPattern() {
        assertThat(titles(bookRepository.findByAuthorName("Sophia%")))
                .containsExactlyInAnyOrder("O Rapaz de Bronze", "Contos Exemplares");
    }

    @Test
    void findBooksByAuthorNumberReturnsAllBooksOfTheAuthor() {
        assertThat(titles(bookRepository.findBooksByAuthorNumber(pina.getAuthorNumber())))
                .containsExactlyInAnyOrder("O Inspetor Max", "O Rapaz de Bronze");
    }

    @Test
    void searchBooksCombinesCriteriaWithAnd() {
        final var query = new SearchBooksQuery("O ", "Romance", "Manuel");

        assertThat(titles(bookRepository.searchBooks(new Page(1, 10), query))).containsExactly("O Rapaz de Bronze");
    }

    @Test
    void searchBooksIsPagedAndOrderedByTitle() {
        final var query = new SearchBooksQuery("", "", "Sophia");

        assertThat(titles(bookRepository.searchBooks(new Page(1, 1), query))).containsExactly("Contos Exemplares");
        assertThat(titles(bookRepository.searchBooks(new Page(2, 1), query))).containsExactly("O Rapaz de Bronze");
    }

    @Test
    void isbnMustBeUnique() {
        final Genre genre = entityManager.persist(new Genre("Thriller"));
        assertThrows(PersistenceException.class, () -> entityManager.persistAndFlush(
                new Book("9782826012092", "Outro livro", null, genre, List.of(sophia), null)));
    }
}
