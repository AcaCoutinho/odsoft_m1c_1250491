package pt.psoft.g1.psoftg1.bookmanagement.unit;

import pt.psoft.g1.psoftg1.bookmanagement.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import org.hibernate.StaleObjectStateException;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.bookmanagement.services.UpdateBookRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit, opaque box, functional: invariants of the {@link Book} aggregate (ISBN, title, genre and at
 * least one author are mandatory) and the optimistic concurrency of patch and photo removal.
 */
class BookTest {
    private final String validIsbn = "9782826012092";
    private final String validTitle = "Encantos de contar";
    private final Author validAuthor1 = new Author("João Alberto", "O João Alberto nasceu em Chaves e foi pedreiro a maior parte da sua vida.", null);
    private final Author validAuthor2 = new Author("Maria José", "A Maria José nasceu em Viseu e só come laranjas às segundas feiras.", null);
    private final Genre validGenre = new Genre("Fantasia");
    private ArrayList<Author> authors = new ArrayList<>();

    @BeforeEach
    void setUp(){
        authors.clear();
    }

    @Test
    void ensureIsbnNotNull(){
        authors.add(validAuthor1);
        assertThrows(IllegalArgumentException.class, () -> new Book(null, validTitle, null, validGenre, authors, null));
    }

    @Test
    void ensureTitleNotNull(){
        authors.add(validAuthor1);
        assertThrows(IllegalArgumentException.class, () -> new Book(validIsbn, null, null, validGenre, authors, null));
    }

    @Test
    void ensureGenreNotNull(){
        authors.add(validAuthor1);
        assertThrows(IllegalArgumentException.class, () -> new Book(validIsbn, validTitle, null,null, authors, null));
    }

    @Test
    void ensureAuthorsNotNull(){
        authors.add(validAuthor1);
        assertThrows(IllegalArgumentException.class, () -> new Book(validIsbn, validTitle, null, validGenre, null, null));
    }

    @Test
    void ensureAuthorsNotEmpty(){
        assertThrows(IllegalArgumentException.class, () -> new Book(validIsbn, validTitle, null, validGenre, authors, null));
    }

    @Test
    void ensureBookCreatedWithMultipleAuthors() {
        authors.add(validAuthor1);
        authors.add(validAuthor2);
        assertDoesNotThrow(() -> new Book(validIsbn, validTitle, null, validGenre, authors, null));
    }

    @Test
    void ensureDescriptionIsStoredWhenGiven() {
        authors.add(validAuthor1);
        final Book book = new Book(validIsbn, validTitle, "Policial juvenil", validGenre, authors, null);
        assertEquals("Policial juvenil", book.getDescription());
    }

    // Optimistic concurrency: the patch and the photo removal must target the current version
    private Book bookWithVersion(long version) {
        authors.add(validAuthor1);
        final Book book = new Book(validIsbn, validTitle, null, validGenre, authors, "capa.jpg");
        ReflectionTestUtils.setField(book, "version", version);
        return book;
    }

    @Test
    void ensurePatchWithStaleVersionIsRejected() {
        final Book book = bookWithVersion(2);
        final var request = new UpdateBookRequest();
        request.setTitle("Outro titulo");

        assertThrows(StaleObjectStateException.class, () -> book.applyPatch(1L, request));
        assertEquals(validTitle, book.getTitle().toString());
    }

    @Test
    void ensurePatchChangesOnlyTheProvidedFields() {
        final Book book = bookWithVersion(2);
        final var request = new UpdateBookRequest();
        request.setDescription("Nova descricao");

        book.applyPatch(2L, request);

        assertEquals("Nova descricao", book.getDescription());
        assertEquals(validTitle, book.getTitle().toString());
        assertEquals(validGenre, book.getGenre());
        assertEquals("capa.jpg", book.getPhoto().getPhotoFile());
    }

    @Test
    void ensurePhotoRemovalWithStaleVersionIsRejected() {
        final Book book = bookWithVersion(2);

        assertThrows(ConflictException.class, () -> book.removePhoto(1));
        assertNotNull(book.getPhoto());
    }

    @Test
    void ensurePhotoIsRemoved() {
        final Book book = bookWithVersion(2);
        book.removePhoto(2);
        assertNull(book.getPhoto());
    }
}
