package pt.psoft.g1.psoftg1.lendingmanagement.unit;

import pt.psoft.g1.psoftg1.lendingmanagement.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit, opaque box, functional. Format "{year}/{sequential}", with boundary values on the year
 * (1970 .. current year) and on the sequential (>= 0).
 */
class LendingNumberTest {
    @Test
    void ensureLendingNumberNotNull(){
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(null));
    }
    @Test
    void ensureLendingNumberNotBlank(){
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(""));
    }
    @ParameterizedTest
    @ValueSource(strings = {"1/2024", "24/1", "2024-1", "2024\\1"})
    void ensureLendingNumberNotWrongFormat(String invalid){
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(invalid));
    }
    @Test
    void ensureLendingNumberIsSetWithString() {
        final var ln = new LendingNumber("2024/1");
        assertEquals("2024/1", ln.toString());
    }

    @Test
    void ensureLendingNumberIsSetWithSequential() {
        final LendingNumber ln = new LendingNumber(1);
        assertEquals(LocalDate.now().getYear() + "/1", ln.toString());
    }

    @Test
    void ensureLendingNumberIsSetWithYearAndSequential() {
        assertEquals("2024/1", new LendingNumber(2024,1).toString());
    }

    @Test
    void ensureSequentialCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(2024,-1));
    }

    @Test
    void ensureYearCannotBeInTheFuture() {
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(LocalDate.now().getYear()+1,1));
    }

    // Boundary value analysis: year must be in [1970, current year], sequential >= 0
    @Test
    void ensureYearBefore1970IsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(1969, 1));
    }

    @Test
    void ensureYear1970IsAccepted() {
        assertEquals("1970/1", new LendingNumber(1970, 1).toString());
    }

    @Test
    void ensureCurrentYearIsAccepted() {
        final int year = LocalDate.now().getYear();
        assertEquals(year + "/1", new LendingNumber(year, 1).toString());
    }

    @Test
    void ensureSequentialOnlyConstructorAcceptsZero() {
        assertEquals(LocalDate.now().getYear() + "/0", new LendingNumber(0).toString());
    }

    @Test
    void ensureSequentialOnlyConstructorRejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(-1));
    }

    @Test
    void ensureSequentialZeroIsAccepted() {
        assertEquals("2024/0", new LendingNumber(2024, 0).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024/", "/1", "2024/a", "abcd/1", "2024"})
    void ensureIncompleteLendingNumberIsRejected(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(invalid));
    }
}
