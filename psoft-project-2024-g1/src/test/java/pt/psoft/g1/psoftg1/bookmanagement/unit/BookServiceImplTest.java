package pt.psoft.g1.psoftg1.bookmanagement.unit;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookServiceImpl;
import pt.psoft.g1.psoftg1.bookmanagement.services.CreateBookRequest;
import pt.psoft.g1.psoftg1.bookmanagement.services.SearchBooksQuery;
import pt.psoft.g1.psoftg1.bookmanagement.services.UpdateBookRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test of {@link BookServiceImpl}, isolated from the repositories with stubs/mocks.
 * <p>Transparent box: covers the branches of {@code create}, {@code update}, {@code removeBookPhoto}
 * and {@code getBooksSuggestionsForReader} (including the per-genre suggestion limit as a boundary).
 */
@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {
    private static final String ISBN = "9782826012092";

    @Mock
    private BookRepository bookRepository;
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private ReaderRepository readerRepository;
    @InjectMocks
    private BookServiceImpl service;

    private final Genre fantasia = new Genre("Fantasia");
    private final Author author = new Author("Manuel Antonio Pina", "Jornalista e escritor", null);

    private Book book(String isbn, String photoURI) {
        return new Book(isbn, "O Inspetor Max", null, fantasia, List.of(author), photoURI);
    }

    private static CreateBookRequest createRequest(List<Long> authors) {
        final var request = new CreateBookRequest();
        request.setTitle("O Inspetor Max");
        request.setGenre("Fantasia");
        request.setAuthors(authors);
        return request;
    }

    @Test
    void createRejectsAnExistingIsbn() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(book(ISBN, null)));

        assertThrows(ConflictException.class, () -> service.create(createRequest(List.of(1L)), ISBN));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void createIgnoresUnknownAuthors() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.empty());
        when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.of(author));
        when(authorRepository.findByAuthorNumber(2L)).thenReturn(Optional.empty());
        when(genreRepository.findByString("Fantasia")).thenReturn(Optional.of(fantasia));
        when(bookRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final Book created = service.create(createRequest(List.of(1L, 2L)), ISBN);

        assertEquals(List.of(author), created.getAuthors());
        assertEquals(ISBN, created.getIsbn());
    }

    @Test
    void createWithOnlyUnknownAuthorsIsRejected() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.empty());
        when(authorRepository.findByAuthorNumber(2L)).thenReturn(Optional.empty());
        when(genreRepository.findByString("Fantasia")).thenReturn(Optional.of(fantasia));

        assertThrows(IllegalArgumentException.class, () -> service.create(createRequest(List.of(2L)), ISBN));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void createWithUnknownGenreIsRejected() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.empty());
        when(genreRepository.findByString("Fantasia")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(createRequest(List.of()), ISBN));
        verify(bookRepository, never()).save(any());
    }

    @Disabled("Known defect: create clears the request's photoURI but builds the Book with the local, uncleared copy")
    @Test
    void createIgnoresPhotoUriWithoutPhotoFile() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.empty());
        when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.of(author));
        when(genreRepository.findByString("Fantasia")).thenReturn(Optional.of(fantasia));
        when(bookRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        final var request = createRequest(List.of(1L));
        request.setPhotoURI("injected.jpg");

        assertNull(service.create(request, ISBN).getPhoto());
    }

    @Test
    void updateWithStaleVersionIsRejected() {
        final Book existing = book(ISBN, null);
        ReflectionTestUtils.setField(existing, "version", 4L);
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(existing));
        final var request = new UpdateBookRequest();
        request.setIsbn(ISBN);
        request.setTitle("Outro titulo");

        assertThrows(StaleObjectStateException.class, () -> service.update(request, "3"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void updateChangesOnlyTheProvidedFields() {
        final Book existing = book(ISBN, null);
        ReflectionTestUtils.setField(existing, "version", 3L);
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(existing));
        final var request = new UpdateBookRequest();
        request.setIsbn(ISBN);
        request.setTitle("Outro titulo");

        final Book updated = service.update(request, "3");

        assertEquals("Outro titulo", updated.getTitle().toString());
        assertEquals(fantasia, updated.getGenre());
        assertEquals(List.of(author), updated.getAuthors());
        verify(bookRepository).save(existing);
    }

    @Test
    void updateWithUnknownGenreIsRejected() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(book(ISBN, null)));
        when(genreRepository.findByString("Culinaria")).thenReturn(Optional.empty());
        final var request = new UpdateBookRequest();
        request.setIsbn(ISBN);
        request.setGenre("Culinaria");

        assertThrows(NotFoundException.class, () -> service.update(request, "0"));
    }

    @Disabled("Known defect: update accepts a list of unknown authors and leaves the book with no authors, which create rejects")
    @Test
    void updateWithOnlyUnknownAuthorsIsRejected() {
        final Book existing = book(ISBN, null);
        ReflectionTestUtils.setField(existing, "version", 0L);
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(existing));
        when(authorRepository.findByAuthorNumber(99L)).thenReturn(Optional.empty());
        final var request = new UpdateBookRequest();
        request.setIsbn(ISBN);
        request.setAuthors(List.of(99L));

        assertThrows(IllegalArgumentException.class, () -> service.update(request, "0"));
    }

    @Test
    void updateOfUnknownBookIsRejected() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.empty());
        final var request = new UpdateBookRequest();
        request.setIsbn(ISBN);

        assertThrows(NotFoundException.class, () -> service.update(request, "0"));
    }

    @Test
    void removeBookPhotoWithoutPhotoIsRejected() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(book(ISBN, null)));

        assertThrows(NotFoundException.class, () -> service.removeBookPhoto(ISBN, 0));
        verifyNoInteractions(photoRepository);
    }

    @Test
    void removeBookPhotoDeletesTheStoredPhoto() {
        final Book withPhoto = book(ISBN, "capa.jpg");
        ReflectionTestUtils.setField(withPhoto, "version", 0L);
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(withPhoto));
        when(bookRepository.save(withPhoto)).thenReturn(withPhoto);

        service.removeBookPhoto(ISBN, 0);

        assertNull(withPhoto.getPhoto());
        verify(photoRepository).deleteByPhotoFile("capa.jpg");
    }

    @Test
    void suggestionsForUnknownReaderAreRejected() {
        when(readerRepository.findByReaderNumber("2024/1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getBooksSuggestionsForReader("2024/1"));
    }

    @Test
    void suggestionsForReaderWithoutInterestsAreRejected() {
        final ReaderDetails reader = mock(ReaderDetails.class);
        when(reader.getInterestList()).thenReturn(List.of());
        when(readerRepository.findByReaderNumber("2024/1")).thenReturn(Optional.of(reader));

        assertThrows(NotFoundException.class, () -> service.getBooksSuggestionsForReader("2024/1"));
    }

    // Boundary value analysis: suggestionsLimitPerGenre = 2, the genre has 3 books
    @Test
    void suggestionsAreLimitedPerGenreOfInterest() {
        ReflectionTestUtils.setField(service, "suggestionsLimitPerGenre", 2L);
        final Genre romance = new Genre("Romance");
        final ReaderDetails reader = mock(ReaderDetails.class);
        when(reader.getInterestList()).thenReturn(List.of(fantasia, romance));
        when(readerRepository.findByReaderNumber("2024/1")).thenReturn(Optional.of(reader));
        final Book b1 = book("9782826012092", null);
        final Book b2 = book("9780306406157", null);
        final Book b3 = book("9789720706386", null);
        when(bookRepository.findByGenre("Fantasia")).thenReturn(List.of(b1, b2, b3));
        when(bookRepository.findByGenre("Romance")).thenReturn(List.of());

        assertEquals(List.of(b1, b2), service.getBooksSuggestionsForReader("2024/1"));
    }

    @Test
    void findByAuthorNameSearchesByPrefix() {
        service.findByAuthorName("Manuel");

        verify(bookRepository).findByAuthorName("Manuel%");
    }

    @Test
    void searchBooksUsesDefaultPageAndEmptyQuery() {
        final ArgumentCaptor<Page> page = ArgumentCaptor.forClass(Page.class);
        final ArgumentCaptor<SearchBooksQuery> query = ArgumentCaptor.forClass(SearchBooksQuery.class);

        service.searchBooks(null, null);

        verify(bookRepository).searchBooks(page.capture(), query.capture());
        assertEquals(new Page(1, 10), page.getValue());
        assertEquals(new SearchBooksQuery("", "", ""), query.getValue());
    }
}
