package pt.psoft.g1.psoftg1.readermanagement.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.services.CreateReaderRequest;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderMapper;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderServiceImpl;
import pt.psoft.g1.psoftg1.readermanagement.services.SearchReadersQuery;
import pt.psoft.g1.psoftg1.readermanagement.services.UpdateReaderRequest;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test of {@link ReaderServiceImpl}, isolated with stubs/mocks of the repositories and of the
 * MapStruct {@link ReaderMapper}.
 * <p>Covers the registration rules (unique username, forbidden names, known genres of interest,
 * next reader number) and the boundaries of {@code findTopReaders} and {@code findTopByGenre}.
 */
@ExtendWith(MockitoExtension.class)
class ReaderServiceImplTest {
    @Mock
    private ReaderRepository readerRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private ReaderMapper readerMapper;
    @Mock
    private GenreRepository genreRepo;
    @Mock
    private ForbiddenNameRepository forbiddenNameRepository;
    @Mock
    private PhotoRepository photoRepository;
    @InjectMocks
    private ReaderServiceImpl service;

    private static CreateReaderRequest createRequest(String fullName, List<String> interests) {
        final var request = new CreateReaderRequest();
        request.setUsername("ana@gmail.com");
        request.setPassword("Anaanaana!1");
        request.setFullName(fullName);
        request.setBirthDate("2000-01-01");
        request.setPhoneNumber("912345678");
        request.setGdpr(true);
        request.setInterestList(interests);
        return request;
    }

    private static ReaderDetails readerDetails(String photoURI) {
        final var details = new ReaderDetails(1, Reader.newReader("ana@gmail.com", "Anaanaana!1", "Ana Silva"),
                "2000-01-01", "912345678", true, false, false, photoURI, null);
        ReflectionTestUtils.setField(details, "version", 0L);
        return details;
    }

    @Test
    void createRejectsAnExistingUsername() {
        when(userRepo.findByUsername("ana@gmail.com")).thenReturn(Optional.of(mock(User.class)));

        assertThrows(ConflictException.class, () -> service.create(createRequest("Ana Silva", null), null));
        verify(readerRepo, never()).save(any());
    }

    @Test
    void createRejectsNamesWithAForbiddenWord() {
        when(forbiddenNameRepository.findByForbiddenNameIsContained("Batman"))
                .thenReturn(List.of(new ForbiddenName("Batman")));

        assertThrows(IllegalArgumentException.class, () -> service.create(createRequest("Batman Silva", null), null));
        verify(readerRepo, never()).save(any());
    }

    @Test
    void createRejectsUnknownGenresOfInterest() {
        when(genreRepo.findByString("Culinaria")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.create(createRequest("Ana Silva", List.of("Culinaria")), null));
        verify(readerRepo, never()).save(any());
    }

    @Test
    void createAssignsTheNextReaderNumberAndSavesUserAndDetails() {
        final var request = createRequest("Ana Silva", List.of("Fantasia"));
        final Genre fantasia = new Genre("Fantasia");
        final Reader reader = Reader.newReader("ana@gmail.com", "Anaanaana!1", "Ana Silva");
        final ReaderDetails details = readerDetails(null);
        when(genreRepo.findByString("Fantasia")).thenReturn(Optional.of(fantasia));
        when(readerRepo.getCountFromCurrentYear()).thenReturn(7);
        when(readerMapper.createReader(request)).thenReturn(reader);
        when(readerMapper.createReaderDetails(eq(8), eq(reader), eq(request), isNull(), eq(List.of(fantasia))))
                .thenReturn(details);
        when(readerRepo.save(details)).thenReturn(details);

        assertSame(details, service.create(request, null));
        verify(userRepo).save(reader);
    }

    // Boundary value analysis: minTop must be >= 1
    @Test
    void topReadersRejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> service.findTopReaders(0));
        verifyNoInteractions(readerRepo);
    }

    @Test
    void topReadersAcceptsOne() {
        when(readerRepo.findTopReaders(any())).thenReturn(new PageImpl<>(List.of()));
        final ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        service.findTopReaders(1);

        verify(readerRepo).findTopReaders(pageable.capture());
        assertEquals(1, pageable.getValue().getPageSize());
    }

    @Test
    void topByGenreRejectsStartAfterEnd() {
        final LocalDate day = LocalDate.of(2024, 2, 1);
        assertThrows(IllegalArgumentException.class, () -> service.findTopByGenre("Fantasia", day.plusDays(1), day));
        verifyNoInteractions(readerRepo);
    }

    @Test
    void topByGenreAcceptsASingleDay() {
        final LocalDate day = LocalDate.of(2024, 2, 1);
        when(readerRepo.findTopByGenre(any(), eq("Fantasia"), eq(day), eq(day))).thenReturn(new PageImpl<>(List.of()));

        assertTrue(service.findTopByGenre("Fantasia", day, day).isEmpty());
    }

    @Test
    void updateOfUnknownReaderIsRejected() {
        when(readerRepo.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(1L, new UpdateReaderRequest(), 0, null));
    }

    @Test
    void searchWithoutResultsIsNotFoundAndUsesDefaults() {
        final ArgumentCaptor<Page> page = ArgumentCaptor.forClass(Page.class);
        final ArgumentCaptor<SearchReadersQuery> query = ArgumentCaptor.forClass(SearchReadersQuery.class);

        assertThrows(NotFoundException.class, () -> service.searchReaders(null, null));

        verify(readerRepo).searchReaderDetails(page.capture(), query.capture());
        assertEquals(new Page(1, 10), page.getValue());
        assertEquals(new SearchReadersQuery("", "", ""), query.getValue());
    }

    @Test
    void removeReaderPhotoDeletesTheStoredPhoto() {
        final ReaderDetails details = readerDetails("foto.jpg");
        when(readerRepo.findByReaderNumber("2024/1")).thenReturn(Optional.of(details));
        when(readerRepo.save(details)).thenReturn(details);

        service.removeReaderPhoto("2024/1", 0);

        assertNull(details.getPhoto());
        verify(photoRepository).deleteByPhotoFile("foto.jpg");
    }
}
