package pt.psoft.g1.psoftg1.genremanagement.unit;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit, opaque box, functional: genre is mandatory and at most 100 characters (boundary 100/101).
 */
class GenreTest {

    @Test
    void ensureGenreMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new Genre(null));
    }

    @Test
    void ensureGenreMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Genre(""));
    }

    @Test
    void ensureGenreIsSet() {
        final var genre = new Genre("Some genre");
        assertEquals("Some genre", genre.toString());
    }

    // Boundary value analysis: GENRE_MAX_LENGTH = 100
    @Test
    void ensureGenreWithMaxLengthIsAccepted() {
        assertDoesNotThrow(() -> new Genre("a".repeat(100)));
    }

    @Test
    void ensureGenreAboveMaxLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Genre("a".repeat(101)));
    }
}
