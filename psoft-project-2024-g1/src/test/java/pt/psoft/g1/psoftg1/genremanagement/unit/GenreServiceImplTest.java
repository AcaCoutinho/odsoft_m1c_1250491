package pt.psoft.g1.psoftg1.genremanagement.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDTO;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreServiceImpl;
import pt.psoft.g1.psoftg1.genremanagement.services.GetAverageLendingsQuery;
import pt.psoft.g1.psoftg1.shared.services.Page;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit test of {@link GenreServiceImpl} with a stubbed/mocked {@link GenreRepository}.
 * <p>Opaque box: equivalence classes and boundaries of the date range of
 * {@code getLendingsAverageDurationPerMonth} (bad format, start after end, start equal to end).
 */
@ExtendWith(MockitoExtension.class)
class GenreServiceImplTest {
    @Mock
    private GenreRepository genreRepository;
    @InjectMocks
    private GenreServiceImpl service;

    @ParameterizedTest
    @CsvSource({"2024/01/01,2024-12-31", "2024-01-01,31-12-2024", "2024-13-01,2024-12-31", "'',2024-12-31"})
    void averageDurationRejectsInvalidDates(String start, String end) {
        assertThrows(IllegalArgumentException.class, () -> service.getLendingsAverageDurationPerMonth(start, end));
        verifyNoInteractions(genreRepository);
    }

    @Test
    void averageDurationRejectsStartAfterEnd() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getLendingsAverageDurationPerMonth("2024-02-02", "2024-02-01"));
        verifyNoInteractions(genreRepository);
    }

    @Test
    void averageDurationAcceptsASingleDayRange() {
        final var result = List.of(mock(GenreLendingsPerMonthDTO.class));
        final LocalDate day = LocalDate.of(2024, 2, 1);
        when(genreRepository.getLendingsAverageDurationPerMonth(day, day)).thenReturn(result);

        assertEquals(result, service.getLendingsAverageDurationPerMonth("2024-02-01", "2024-02-01"));
    }

    @Test
    void averageDurationWithoutDataIsNotFound() {
        when(genreRepository.getLendingsAverageDurationPerMonth(any(), any())).thenReturn(List.of());

        assertThrows(NotFoundException.class,
                () -> service.getLendingsAverageDurationPerMonth("2024-01-01", "2024-12-31"));
    }

    @Test
    void averageLendingsUsesFirstDayOfMonthAndDefaultPage() {
        final ArgumentCaptor<Page> page = ArgumentCaptor.forClass(Page.class);

        service.getAverageLendings(new GetAverageLendingsQuery(2024, 2), null);

        verify(genreRepository).getAverageLendingsInMonth(eq(LocalDate.of(2024, 2, 1)), page.capture());
        assertEquals(new Page(1, 10), page.getValue());
    }

    @Test
    void topGenresAsksForTheFirstPageOfFive() {
        when(genreRepository.findTop5GenreByBookCount(any())).thenReturn(new PageImpl<>(List.of()));
        final ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        assertTrue(service.findTopGenreByBooks().isEmpty());

        verify(genreRepository).findTop5GenreByBookCount(pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
    }
}
