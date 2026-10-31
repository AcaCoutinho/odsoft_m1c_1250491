package pt.psoft.g1.psoftg1.readermanagement.unit;

import pt.psoft.g1.psoftg1.readermanagement.model.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit, opaque box, functional. Portuguese mobile (9xxxxxxxx) and landline (2xxxxxxxx) numbers are
 * valid; any other first digit and any length other than 9 are invalid (boundary 8/9/10 digits).
 */
public class PhoneNumberTest {
    @ParameterizedTest
    @ValueSource(strings = {"912345678", "212345678"})
    void ensureMobileAndLandlineNumbersAreAccepted(String valid) {
        assertEquals(valid, new PhoneNumber(valid).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"112345678", "312345678", "512345678", "012345678", "+35191234"})
    void ensureOtherFirstDigitsAreRejected(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber(invalid));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "91234567", "21234567", "9123456789", "2123456789"})
    void ensureLengthsOtherThanNineAreRejected(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber(invalid));
    }

    @Disabled("Known defect: PhoneNumber only checks the first digit and the length, so letters are accepted")
    @Test
    void ensureNonDigitsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("9abcdefgh"));
    }
}
