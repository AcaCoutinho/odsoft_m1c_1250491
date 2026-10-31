package pt.psoft.g1.psoftg1.shared.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit, opaque box, functional: parsing of the If-Match header (quoted and unquoted ETags valid, any
 * other text invalid).
 */
class ConcurrencyServiceTest {
    private final ConcurrencyService service = new ConcurrencyService();

    @Test
    void quotedAndUnquotedEtagsAreAccepted() {
        assertEquals(3L, service.getVersionFromIfMatchHeader("\"3\""));
        assertEquals(3L, service.getVersionFromIfMatchHeader("3"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "\"abc\"", "W/\"3\"", ""})
    void nonNumericEtagsAreRejected(String etag) {
        assertThrows(NumberFormatException.class, () -> service.getVersionFromIfMatchHeader(etag));
    }
}
