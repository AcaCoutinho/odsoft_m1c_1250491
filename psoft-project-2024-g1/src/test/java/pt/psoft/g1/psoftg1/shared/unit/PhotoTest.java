package pt.psoft.g1.psoftg1.shared.unit;

import pt.psoft.g1.psoftg1.shared.model.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit, opaque box, functional: {@link Photo} keeps the path it is given, and a missing path is rejected.
 */
class PhotoTest {
    @Test
    void ensurePathMustNotBeNull() {
        assertThrows(NullPointerException.class, () -> new Photo(null));
    }

    @Test
    void ensurePhotoFileIsThePath() {
        assertEquals("photoTest.jpg", new Photo(Paths.get("photoTest.jpg")).getPhotoFile());
    }
}
