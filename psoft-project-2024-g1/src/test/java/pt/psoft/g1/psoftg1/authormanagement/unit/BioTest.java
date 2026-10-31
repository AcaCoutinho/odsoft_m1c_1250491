package pt.psoft.g1.psoftg1.authormanagement.unit;

import pt.psoft.g1.psoftg1.authormanagement.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit, opaque box, functional and security. Bio is mandatory, at most 4096 characters
 * (boundary 4096/4097) and HTML-sanitized.
 */
public class BioTest {

    @Test
    void ensureBioMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new Bio(null));
    }

    @Test
    void ensureBioMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Bio(""));
    }

    @Test
    void ensureBioIsSet() {
        final var bio = new Bio("Some bio");
        assertEquals("Some bio", bio.toString());
    }

    @Test
    void ensureBioIsChanged() {
        final var bio = new Bio("Some bio");
        bio.setBio("Some other bio");
        assertEquals("Some other bio", bio.toString());
    }

    // Boundary value analysis: BIO_MAX_LENGTH = 4096
    @Test
    void ensureBioWithMaxLengthIsAccepted() {
        assertEquals(4096, new Bio("a".repeat(4096)).toString().length());
    }

    @Test
    void ensureBioAboveMaxLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Bio("a".repeat(4097)));
    }

    @Test
    void ensureBioIsSanitized() {
        assertEquals("Bio", new Bio("<script>alert(1)</script>Bio").toString());
    }
}
