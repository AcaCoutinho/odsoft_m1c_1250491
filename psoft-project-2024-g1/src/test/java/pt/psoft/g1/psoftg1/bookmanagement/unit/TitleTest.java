package pt.psoft.g1.psoftg1.bookmanagement.unit;

import pt.psoft.g1.psoftg1.bookmanagement.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit, opaque box, functional: title is mandatory, trimmed and at most 128 characters (boundary 128/129).
 */
class TitleTest {

    @Test
    void ensureTitleMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new Title(null));
    }

    @Test
    void ensureTitleMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Title(""));
    }

    @Test
    void ensureTitleCantStartWithWhitespace() {
        final var title = new Title(" Some title");
        assertEquals("Some title", title.toString());
    }

    @Test
    void ensureTitleCantEndWithWhitespace() {
        final var title = new Title("Some title ");
        assertEquals("Some title", title.toString());
    }

    @Test
    void ensureTitleIsSet() {
        final var title = new Title("Some title");
        assertEquals("Some title", title.toString());
    }

    @Test
    void ensureTitleIsChanged() {
        final var title = new Title("Some title");
        title.setTitle("Some other title");
        assertEquals("Some other title", title.toString());
    }

    // Boundary value analysis: TITLE_MAX_LENGTH = 128
    @Test
    void ensureTitleWithMaxLengthIsAccepted() {
        assertEquals(128, new Title("a".repeat(128)).toString().length());
    }

    @Test
    void ensureTitleAboveMaxLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Title("a".repeat(129)));
    }

    // Equivalence partitioning: blank titles (any whitespace-only string) are one invalid class
    @ParameterizedTest
    @ValueSource(strings = {" ", "   ", "\t", "\n"})
    void ensureWhitespaceOnlyTitleIsRejected(String blank) {
        assertThrows(IllegalArgumentException.class, () -> new Title(blank));
    }
}
