package pt.psoft.g1.psoftg1.lendingmanagement.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit, opaque box, functional. The fine is the number of days of delay times the value per day;
 * a lending with no delay cannot be fined (boundary at 0/1 days). {@link Lending} is a stub.
 */
class FineTest {
    private static final int FINE_PER_DAY = 200;

    private static Lending lendingDelayedBy(int days) {
        final Lending lending = mock(Lending.class);
        when(lending.getDaysDelayed()).thenReturn(days);
        when(lending.getFineValuePerDayInCents()).thenReturn(FINE_PER_DAY);
        return lending;
    }

    @ParameterizedTest(name = "{0} days late -> {1} cents")
    @CsvSource({"1, 200", "2, 400", "30, 6000"})
    void fineIsTheDelayTimesTheValuePerDay(int days, int cents) {
        final Lending lending = lendingDelayedBy(days);
        final Fine fine = new Fine(lending);

        assertEquals(cents, fine.getCentsValue());
        assertEquals(FINE_PER_DAY, fine.getFineValuePerDayInCents());
        assertSame(lending, fine.getLending());
    }

    @Test
    void lendingWithoutDelayIsNotFined() {
        assertThrows(IllegalArgumentException.class, () -> new Fine(lendingDelayedBy(0)));
    }
}
