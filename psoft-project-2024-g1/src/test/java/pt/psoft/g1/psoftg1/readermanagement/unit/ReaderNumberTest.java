package pt.psoft.g1.psoftg1.readermanagement.unit;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderNumber;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit, opaque box, functional: a reader number is "{registry year}/{sequential}" (customer clarification).
 */
class ReaderNumberTest {
    @Test
    void ensureReaderNumberHasYearAndSequential() {
        assertEquals("2023/19876", new ReaderNumber(2023, 19876).toString());
    }

    @Test
    void ensureReaderNumberDefaultsToTheCurrentYear() {
        assertEquals(LocalDate.now().getYear() + "/1", new ReaderNumber(1).toString());
    }
}
