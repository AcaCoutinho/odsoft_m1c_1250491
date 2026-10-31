package pt.psoft.g1.psoftg1.shared.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.psoft.g1.psoftg1.shared.model.StringUtilsCustom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit, opaque box. Functional (equivalence classes of characters) and security (HTML sanitization
 * against XSS, OWASP sanitizer with the FORMATTING and LINKS policies).
 */
class StringUtilsCustomTest {

    @ParameterizedTest
    @ValueSource(strings = {"", "Ana", "João Alberto", "Maria-José", "D'Artagnan", "Agent 007"})
    void lettersDigitsSpacesApostrophesAndHyphensAreAlphanumeric(String valid) {
        assertTrue(StringUtilsCustom.isAlphanumeric(valid));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ana!", "a@b", "<b>", "Ana;Rui", "Ana_Rui"})
    void symbolsAreNotAlphanumeric(String invalid) {
        assertFalse(StringUtilsCustom.isAlphanumeric(invalid));
    }

    // The name reads as the opposite of the result: it returns true when there is NO leading or trailing space
    @ParameterizedTest
    @ValueSource(strings = {" Ana", "Ana ", " Ana "})
    void leadingOrTrailingSpaceGivesFalse(String padded) {
        assertFalse(StringUtilsCustom.startsOrEndsInWhiteSpace(padded));
    }

    @Test
    void textWithoutLeadingOrTrailingSpaceGivesTrue() {
        assertTrue(StringUtilsCustom.startsOrEndsInWhiteSpace("Ana Silva"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>Texto",
            "<img src=x onerror=alert(1)>Texto",
            "<iframe src=\"https://evil.test\"></iframe>Texto"})
    void dangerousHtmlIsRemoved(String html) {
        assertEquals("Texto", StringUtilsCustom.sanitizeHtml(html));
    }

    @Test
    void formattingTagsAreKept() {
        assertEquals("<b>Texto</b>", StringUtilsCustom.sanitizeHtml("<b>Texto</b>"));
    }

    @Test
    void javascriptLinksAreNeutralized() {
        assertFalse(StringUtilsCustom.sanitizeHtml("<a href=\"javascript:alert(1)\">x</a>").contains("javascript"));
    }
}
