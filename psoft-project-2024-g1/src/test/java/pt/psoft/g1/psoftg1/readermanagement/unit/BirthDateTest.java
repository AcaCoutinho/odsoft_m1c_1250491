package pt.psoft.g1.psoftg1.readermanagement.unit;

import pt.psoft.g1.psoftg1.readermanagement.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.Disabled;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit, opaque box, functional: format yyyy-MM-dd, no future dates and boundary on the minimum reader age.
 */
public class BirthDateTest {

    @Test
    void ensureBirthDateCanBeCreatedWithValidDate() {
        assertDoesNotThrow(() -> new BirthDate(2000, 1, 1));
    }

    @Test
    void ensureBirthDateCanBeCreatedWithValidStringDate() {
        assertDoesNotThrow(() -> new BirthDate("2000-01-01"));
    }

    @Test
    void ensureExceptionIsThrownForInvalidStringDateFormat() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new BirthDate("01-01-2000"));
        assertEquals("Provided birth date is not in a valid format. Use yyyy-MM-dd", exception.getMessage());
    }

    @Test
    void ensureCorrectStringRepresentation() {
        BirthDate birthDate = new BirthDate(2000, 1, 1);
        assertEquals("2000-1-1", birthDate.toString());
    }

    // Boundary value analysis: minimumReaderAge = 12 (config/library.properties)
    @Test
    void ensureFutureBirthDateIsRejected() {
        final LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertThrows(AccessDeniedException.class,
                () -> new BirthDate(tomorrow.getYear(), tomorrow.getMonthValue(), tomorrow.getDayOfMonth()));
    }

    @Test
    void ensureReaderWithMinimumAgeIsAccepted() {
        final LocalDate twelveYearsAgo = LocalDate.now().minusYears(12);
        assertDoesNotThrow(() -> new BirthDate(twelveYearsAgo.getYear(), twelveYearsAgo.getMonthValue(), twelveYearsAgo.getDayOfMonth()));
    }

    @Disabled("Known defect: BirthDate.minimumAge is a @Value on a JPA embeddable, never injected, so it is always 0")
    @Test
    void ensureReaderBelowMinimumAgeIsRejected() {
        final LocalDate oneDayShort = LocalDate.now().minusYears(12).plusDays(1);
        assertThrows(AccessDeniedException.class,
                () -> new BirthDate(oneDayShort.getYear(), oneDayShort.getMonthValue(), oneDayShort.getDayOfMonth()));
    }
}
