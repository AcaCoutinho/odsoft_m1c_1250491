package pt.psoft.g1.psoftg1.bookmanagement.unit;

import pt.psoft.g1.psoftg1.bookmanagement.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit, opaque box, functional: equivalence classes of ISBN-10 and ISBN-13 (valid check digit, wrong
 * check digit, wrong characters) and boundary values on the length (9/10/11 and 12/13/14).
 */
class IsbnTest {

    @Test
    void ensureIsbnMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new Isbn(null));
    }

    @Test
    void ensureIsbnMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Isbn(""));
    }

    @Test
    void ensureIsbn13IsSet() {
        final var isbn = new Isbn("9782826012092");
        assertEquals("9782826012092", isbn.toString());
    }

    @Test
    void ensureChecksum13IsCorrect() {
        assertThrows(IllegalArgumentException.class, () -> new Isbn("9782826012099"));
    }

    @Test
    void ensureIsbn10IsSet() {
        final var isbn = new Isbn("8175257660");
        assertEquals("8175257660", isbn.toString());
    }

    @Test
    void ensureChecksum10IsCorrect() {
        assertThrows(IllegalArgumentException.class, () -> new Isbn("8175257667"));
    }

    // Equivalence partitioning: valid ISBN-10 (digit and 'X' check digit) and valid ISBN-13
    @ParameterizedTest
    @ValueSource(strings = {"0306406152", "080442957X", "9780306406157", "9782826012092"})
    void ensureValidIsbnIsAccepted(String valid) {
        assertEquals(valid, new Isbn(valid).toString());
    }

    // Invalid classes: wrong check digit, lowercase 'x', non-digits, and lengths around 10 and 13
    @ParameterizedTest
    @ValueSource(strings = {"0306406153", "9780306406158", "080442957x", "97803064061AB",
            "030640615", "03064061520", "978030640615", "97803064061570"})
    void ensureInvalidIsbnIsRejected(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new Isbn(invalid));
    }
}
