package pt.psoft.g1.psoftg1.bookmanagement.unit;

import pt.psoft.g1.psoftg1.bookmanagement.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit, opaque box, functional: description is optional (null/blank stored as null) and at most 4096
 * characters (boundary 4096/4097).
 */
class DescriptionTest {

    @Test
    void ensureDescriptionCanBeNull() {
        assertDoesNotThrow(() -> new Description(null));
    }

    @Test
    void ensureDescriptionIsSet() {
        final var description = new Description("Some description");
        assertEquals("Some description", description.toString());
    }

    @Test
    void ensureDescriptionIsChanged() {
        final var description = new Description("Some description");
        description.setDescription("Some other description");
        assertEquals("Some other description", description.toString());
    }

    // Equivalence partitioning: null and blank descriptions are the same valid class (no description)
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void ensureMissingDescriptionIsStoredAsNull(String missing) {
        assertNull(new Description(missing).toString());
    }

    // Boundary value analysis: DESC_MAX_LENGTH = 4096
    @Test
    void ensureDescriptionWithMaxLengthIsAccepted() {
        assertEquals(4096, new Description("a".repeat(4096)).toString().length());
    }

    @Test
    void ensureDescriptionAboveMaxLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Description("a".repeat(4097)));
    }
}
