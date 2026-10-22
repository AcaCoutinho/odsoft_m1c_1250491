package pt.psoft.g1.psoftg1.usermanagement.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pt.psoft.g1.psoftg1.usermanagement.model.Password;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Opaque box: one valid class (8+ characters with lower case, upper case and a digit or symbol)
 * and one invalid class per missing rule, with the length boundary at 7/8 characters.
 */
class PasswordTest {

    @ParameterizedTest
    @ValueSource(strings = {"Abcdefg1", "Abcdefg!", "Mariaroberta!123"})
    void ensureValidPasswordsAreAccepted(String valid) {
        assertDoesNotThrow(() -> new Password(valid));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abcdefg1", "ABCDEFG1", "Abcdefgh", "        "})
    void ensureInvalidPasswordsAreRejected(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new Password(invalid));
    }

    // Boundary value analysis: minimum length is 8
    @Test
    void ensureSevenCharactersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Password("Abcdef1"));
    }

    @Test
    void ensureEightCharactersAreAccepted() {
        assertDoesNotThrow(() -> new Password("Abcdef12"));
    }
}
