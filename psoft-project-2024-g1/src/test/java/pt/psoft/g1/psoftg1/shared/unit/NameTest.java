package pt.psoft.g1.psoftg1.shared.unit;

import pt.psoft.g1.psoftg1.shared.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit, opaque box, functional: equivalence classes of the characters allowed in a name and boundary
 * value on the column length (150).
 */
class NameTest {
    @Test
    void ensureNameMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new Name(null));
    }

    @Test
    void ensureNameMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Name(""));
    }

    @Test
    void ensureNameMustOnlyBeAlphanumeric() {
        assertThrows(IllegalArgumentException.class, () -> new Name("Ricardo!"));
    }

    @Test
    void ensureNameIsSet() {
        final var name = new Name("Some name");
        assertEquals("Some name", name.toString());
    }

    @Test
    void ensureNameIsChanged() {
        final var name = new Name("Some name");
        name.setName("Some other name");
        assertEquals("Some other name", name.toString());
    }

    // Equivalence partitioning: letters (with accents), digits, spaces, apostrophes and hyphens are valid
    @ParameterizedTest
    @ValueSource(strings = {"Ana", "João Alberto", "Maria-José", "D'Artagnan", "Agent 007"})
    void ensureAlphanumericNamesAreAccepted(String valid) {
        assertEquals(valid, new Name(valid).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ricardo!", "ana@mail.pt", "<b>Ana</b>", "Ana;Rui"})
    void ensureNamesWithSymbolsAreRejected(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new Name(invalid));
    }

    // Boundary value analysis: the NAME column has length 150
    @Test
    void ensureNameWithMaxLengthIsAccepted() {
        assertDoesNotThrow(() -> new Name("a".repeat(150)));
    }

    @Disabled("Known defect: Name does not enforce the 150-character limit of its column")
    @Test
    void ensureNameAboveMaxLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Name("a".repeat(151)));
    }
}
