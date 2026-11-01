package pt.psoft.g1.psoftg1.lendingmanagement.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.services.CreateLendingRequest;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingServiceImpl;
import pt.psoft.g1.psoftg1.lendingmanagement.services.SetLendingReturnedRequest;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test of {@link LendingServiceImpl}, isolated from the repositories with stubs/mocks.
 * {@link Book} and {@link ReaderDetails} are dummies: the service only passes them along.
 * <p>Transparent box: covers every branch of {@code create} (overdue lending, outstanding limit,
 * missing book, missing reader) and of {@code setReturned} (with and without fine), using boundary
 * values for the lending duration (15 days) and the outstanding limit (3 lendings).
 */
@ExtendWith(MockitoExtension.class)
class LendingServiceImplTest {
    private static final int LENDING_DURATION = 15;
    private static final int FINE_PER_DAY = 200;
    private static final String ISBN = "9782826012092";
    private static final String READER_NUMBER = "2024/1";

    @Mock
    private LendingRepository lendingRepository;
    @Mock
    private FineRepository fineRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private ReaderRepository readerRepository;
    @InjectMocks
    private LendingServiceImpl service;

    private final Book book = mock(Book.class);
    private final ReaderDetails readerDetails = mock(ReaderDetails.class);
    private final CreateLendingRequest request = new CreateLendingRequest(ISBN, READER_NUMBER);

    @BeforeEach
    void setUp() {
        // values normally injected by @Value from config/library.properties
        ReflectionTestUtils.setField(service, "lendingDurationInDays", LENDING_DURATION);
        ReflectionTestUtils.setField(service, "fineValuePerDayInCents", FINE_PER_DAY);
    }

    private Lending outstandingLendingStartedDaysAgo(int daysAgo) {
        return Lending.newBootstrappingLending(book, readerDetails, LocalDate.now().getYear(), 1,
                LocalDate.now().minusDays(daysAgo), null, LENDING_DURATION, FINE_PER_DAY);
    }

    private void givenOutstanding(Lending... lendings) {
        when(lendingRepository.listOutstandingByReaderNumber(READER_NUMBER)).thenReturn(List.of(lendings));
    }

    private void givenBookAndReaderExist() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(book));
        when(readerRepository.findByReaderNumber(READER_NUMBER)).thenReturn(Optional.of(readerDetails));
        when(lendingRepository.getCountFromCurrentYear()).thenReturn(41);
        when(lendingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createWithoutOutstandingLendingsUsesNextSequentialAndConfiguredDuration() {
        givenOutstanding();
        givenBookAndReaderExist();

        final Lending lending = service.create(request);

        assertEquals(LocalDate.now().getYear() + "/42", lending.getLendingNumber());
        assertEquals(LocalDate.now().plusDays(LENDING_DURATION), lending.getLimitDate());
        assertEquals(FINE_PER_DAY, lending.getFineValuePerDayInCents());
        verify(lendingRepository).save(lending);
    }

    @Test
    void createIsAllowedWithTwoOutstandingLendingsOnTime() {
        givenOutstanding(outstandingLendingStartedDaysAgo(1), outstandingLendingStartedDaysAgo(LENDING_DURATION));
        givenBookAndReaderExist();

        assertNotNull(service.create(request));
    }

    @Test
    void createIsForbiddenWithThreeOutstandingLendings() {
        givenOutstanding(outstandingLendingStartedDaysAgo(1), outstandingLendingStartedDaysAgo(2),
                outstandingLendingStartedDaysAgo(3));

        assertThrows(LendingForbiddenException.class, () -> service.create(request));
        verify(lendingRepository, never()).save(any());
    }

    @Test
    void createIsForbiddenWithAnOutstandingLendingOneDayOverdue() {
        givenOutstanding(outstandingLendingStartedDaysAgo(LENDING_DURATION + 1));

        assertThrows(LendingForbiddenException.class, () -> service.create(request));
        verify(lendingRepository, never()).save(any());
    }

    @Test
    void createFailsWhenTheBookDoesNotExist() {
        givenOutstanding();
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(request));
        verifyNoInteractions(readerRepository);
    }

    @Test
    void createFailsWhenTheReaderDoesNotExist() {
        givenOutstanding();
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(book));
        when(readerRepository.findByReaderNumber(READER_NUMBER)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(request));
        verify(lendingRepository, never()).save(any());
    }

    @Test
    void setReturnedOnTimeDoesNotCreateAFine() {
        final Lending lending = outstandingLendingStartedDaysAgo(LENDING_DURATION);
        when(lendingRepository.findByLendingNumber("2024/1")).thenReturn(Optional.of(lending));
        when(lendingRepository.save(lending)).thenReturn(lending);

        assertSame(lending, service.setReturned("2024/1", new SetLendingReturnedRequest("Gostei"), 0));

        assertEquals(LocalDate.now(), lending.getReturnedDate());
        verifyNoInteractions(fineRepository);
    }

    @Test
    void setReturnedLateCreatesAFineForEachDayOfDelay() {
        final Lending lending = outstandingLendingStartedDaysAgo(LENDING_DURATION + 3);
        when(lendingRepository.findByLendingNumber("2024/1")).thenReturn(Optional.of(lending));
        when(lendingRepository.save(lending)).thenReturn(lending);
        final ArgumentCaptor<Fine> fine = ArgumentCaptor.forClass(Fine.class);

        service.setReturned("2024/1", new SetLendingReturnedRequest(null), 0);

        verify(fineRepository).save(fine.capture());
        assertEquals(3 * FINE_PER_DAY, fine.getValue().getCentsValue());
    }

    @Test
    void setReturnedOfUnknownLendingThrows() {
        when(lendingRepository.findByLendingNumber("2024/1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.setReturned("2024/1", new SetLendingReturnedRequest(null), 0));
        verify(lendingRepository, never()).save(any());
    }

    @Test
    void listByReaderNumberAndIsbnFiltersByReturnedState() {
        final Lending returned = Lending.newBootstrappingLending(book, readerDetails, 2024, 1,
                LocalDate.now().minusDays(5), LocalDate.now(), LENDING_DURATION, FINE_PER_DAY);
        final Lending outstanding = outstandingLendingStartedDaysAgo(5);
        when(lendingRepository.listByReaderNumberAndIsbn(READER_NUMBER, ISBN))
                .thenAnswer(invocation -> new ArrayList<>(List.of(returned, outstanding)));

        assertEquals(List.of(returned, outstanding), service.listByReaderNumberAndIsbn(READER_NUMBER, ISBN, Optional.empty()));
        assertEquals(List.of(returned), service.listByReaderNumberAndIsbn(READER_NUMBER, ISBN, Optional.of(true)));
        assertEquals(List.of(outstanding), service.listByReaderNumberAndIsbn(READER_NUMBER, ISBN, Optional.of(false)));
    }

    @Test
    void listByReaderNumberAndIsbnRemovesConsecutiveLendingsThatDoNotMatch() {
        final Lending outstanding1 = outstandingLendingStartedDaysAgo(1);
        final Lending outstanding2 = outstandingLendingStartedDaysAgo(2);
        final Lending returned = Lending.newBootstrappingLending(book, readerDetails, 2024, 1,
                LocalDate.now().minusDays(5), LocalDate.now(), LENDING_DURATION, FINE_PER_DAY);
        when(lendingRepository.listByReaderNumberAndIsbn(READER_NUMBER, ISBN))
                .thenReturn(new ArrayList<>(List.of(outstanding1, outstanding2, returned)));

        assertEquals(List.of(returned), service.listByReaderNumberAndIsbn(READER_NUMBER, ISBN, Optional.of(true)));
    }
}
