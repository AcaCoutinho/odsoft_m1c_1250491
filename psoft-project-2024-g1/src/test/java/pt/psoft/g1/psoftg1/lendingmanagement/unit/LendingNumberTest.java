package pt.psoft.g1.psoftg1.lendingmanagement.unit;

import pt.psoft.g1.psoftg1.lendingmanagement.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LendingNumberTest {
    @Test
    void ensureLendingNumberNotNull(){
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(null));
    }
    @Test
    void ensureLendingNumberNotBlank(){
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(""));
    }
    @Test
    void ensureLendingNumberNotWrongFormat(){
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber("1/2024"));
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber("24/1"));
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber("2024-1"));
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber("2024\\1"));
    }
    @Test
    void ensureLendingNumberIsSetWithString() {
        final var ln = new LendingNumber("2024/1");
        assertEquals("2024/1", ln.toString());
    }

    @Test
    void ensureLendingNumberIsSetWithSequential() {
        final LendingNumber ln = new LendingNumber(1);
        assertNotNull(ln);
        assertEquals(LocalDate.now().getYear() + "/1", ln.toString());
    }

    @Test
    void ensureLendingNumberIsSetWithYearAndSequential() {
        final LendingNumber ln = new LendingNumber(2024,1);
        assertNotNull(ln);
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
    void ensureSequentialZeroIsAccepted() {
        assertEquals("2024/0", new LendingNumber(2024, 0).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024/", "/1", "2024/a", "abcd/1", "2024"})
    void ensureIncompleteLendingNumberIsRejected(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(invalid));
    }
}
