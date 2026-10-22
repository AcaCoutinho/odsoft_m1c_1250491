package pt.psoft.g1.psoftg1.lendingmanagement.unit;

import pt.psoft.g1.psoftg1.lendingmanagement.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import org.hibernate.StaleObjectStateException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LendingTest {
    private static final ArrayList<Author> authors = new ArrayList<>();
    private static Book book;
    private static ReaderDetails readerDetails;
    // Same values as config/library.properties. @Value is not injected here because there is no Spring context.
    private final int lendingDurationInDays = 15;
    private final int fineValuePerDayInCents = 200;

    @BeforeAll
    public static void setup(){
        Author author = new Author("Manuel Antonio Pina",
                "Manuel António Pina foi um jornalista e escritor português, premiado em 2011 com o Prémio Camões",
                null);
        authors.add(author);
        book = new Book("9782826012092",
                "O Inspetor Max",
                "conhecido pastor-alemão que trabalha para a Judiciária, vai ser fundamental para resolver um importante caso de uma rede de malfeitores que quer colocar uma bomba num megaconcerto de uma ilustre cantora",
                new Genre("Romance"),
                authors,
                null);
        readerDetails = new ReaderDetails(1,
                Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives"),
                "2000-01-01",
                "919191919",
                true,
                true,
                true,
                null,
                null);
    }

    @Test
    void ensureBookNotNull(){
        assertThrows(IllegalArgumentException.class, () -> new Lending(null, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents));
    }

    @Test
    void ensureReaderNotNull(){
        assertThrows(IllegalArgumentException.class, () -> new Lending(book, null, 1, lendingDurationInDays, fineValuePerDayInCents));
    }

    @Test
    void ensureValidReaderNumber(){
        assertThrows(IllegalArgumentException.class, () -> new Lending(book, readerDetails, -1, lendingDurationInDays, fineValuePerDayInCents));
    }

    @Test
    void testSetReturned(){
        Lending lending = new Lending(book, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents);
        lending.setReturned(0,null);
        assertEquals(LocalDate.now(), lending.getReturnedDate());
    }

    @Test
    void testGetDaysDelayed(){
        Lending lending = new Lending(book, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(0, lending.getDaysDelayed());
    }

    @Test
    void testGetDaysUntilReturn(){
        Lending lending = new Lending(book, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(Optional.of(lendingDurationInDays), lending.getDaysUntilReturn());
    }

    @Test
    void testGetDaysOverDue(){
        Lending lending = new Lending(book, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(Optional.empty(), lending.getDaysOverdue());
    }

    @Test
    void testGetTitle() {
        Lending lending = new Lending(book, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals("O Inspetor Max", lending.getTitle());
    }

    @Test
    void testGetLendingNumber() {
        Lending lending = new Lending(book, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(LocalDate.now().getYear() + "/1", lending.getLendingNumber());
    }

    @Test
    void testGetBook() {
        Lending lending = new Lending(book, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(book, lending.getBook());
    }

    @Test
    void testGetReaderDetails() {
        Lending lending = new Lending(book, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(readerDetails, lending.getReaderDetails());
    }

    @Test
    void testGetStartDate() {
        Lending lending = new Lending(book, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(LocalDate.now(), lending.getStartDate());
    }

    @Test
    void testGetLimitDate() {
        Lending lending = new Lending(book, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(LocalDate.now().plusDays(lendingDurationInDays), lending.getLimitDate());
    }

    @Test
    void testGetReturnedDate() {
        Lending lending = new Lending(book, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertNull(lending.getReturnedDate());
    }

    // Transparent box: branches of getDaysDelayed/getFineValueInCents (returned vs. not returned,
    // delay <= 0 vs. delay > 0), with boundary values around limitDate.
    private Lending lendingStartedDaysAgo(int daysAgo, Integer returnedAfterDays) {
        final LocalDate start = LocalDate.now().minusDays(daysAgo);
        final LocalDate returned = returnedAfterDays == null ? null : start.plusDays(returnedAfterDays);
        return Lending.newBootstrappingLending(book, readerDetails, LocalDate.now().getYear(), 1,
                start, returned, lendingDurationInDays, fineValuePerDayInCents);
    }

    @Test
    void ensureLendingReturnedOnLimitDateHasNoDelayNorFine() {
        final Lending lending = lendingStartedDaysAgo(30, lendingDurationInDays);
        assertEquals(0, lending.getDaysDelayed());
        assertEquals(Optional.empty(), lending.getFineValueInCents());
    }

    @Test
    void ensureLendingReturnedOneDayLateIsFinedForOneDay() {
        final Lending lending = lendingStartedDaysAgo(30, lendingDurationInDays + 1);
        assertEquals(1, lending.getDaysDelayed());
        assertEquals(Optional.of(fineValuePerDayInCents), lending.getFineValueInCents());
    }

    @Test
    void ensureOutstandingLendingOnLimitDateIsNotOverdue() {
        final Lending lending = lendingStartedDaysAgo(lendingDurationInDays, null);
        assertEquals(Optional.empty(), lending.getDaysOverdue());
        assertEquals(Optional.of(0), lending.getDaysUntilReturn());
    }

    @Test
    void ensureOutstandingLendingPastLimitDateAccumulatesFine() {
        final Lending lending = lendingStartedDaysAgo(lendingDurationInDays + 3, null);
        assertEquals(Optional.of(3), lending.getDaysOverdue());
        assertEquals(Optional.empty(), lending.getDaysUntilReturn());
        assertEquals(Optional.of(3 * fineValuePerDayInCents), lending.getFineValueInCents());
    }

    @Test
    void ensureLendingCannotBeReturnedTwice() {
        final Lending lending = new Lending(book, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents);
        lending.setReturned(0, "Gostei");
        assertThrows(IllegalArgumentException.class, () -> lending.setReturned(0, null));
    }

    @Test
    void ensureStaleVersionCannotSetReturned() {
        final Lending lending = new Lending(book, readerDetails, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertThrows(StaleObjectStateException.class, () -> lending.setReturned(1, null));
        assertNull(lending.getReturnedDate());
    }
}
