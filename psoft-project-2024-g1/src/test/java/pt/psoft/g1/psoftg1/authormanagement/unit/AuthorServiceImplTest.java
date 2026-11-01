package pt.psoft.g1.psoftg1.authormanagement.unit;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorLendingView;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorMapper;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorServiceImpl;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test of {@link AuthorServiceImpl}, isolated from its collaborators with test doubles:
 * <ul>
 *     <li>{@code authorRepository}: stub (canned answers) and mock (calls are verified);</li>
 *     <li>{@code mapper}: stub;</li>
 *     <li>{@code photoRepository}: mock;</li>
 *     <li>{@code bookRepository} and the {@link MultipartFile}: dummies, only fill parameter lists;</li>
 *     <li>the {@link Author} in {@code removeAuthorPhoto}: spy over a real object.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AuthorServiceImplTest {
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private AuthorMapper mapper;
    @Mock
    private PhotoRepository photoRepository;
    @InjectMocks
    private AuthorServiceImpl service;

    private final Author author = new Author("Manuel Antonio Pina", "Jornalista e escritor", null);

    @Test
    void findByAuthorNumberReturnsTheRepositoryAnswer() {
        when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.of(author));

        assertEquals(Optional.of(author), service.findByAuthorNumber(1L));
    }

    /**
     * Transparent box, condition coverage of
     * {@code photo == null && photoURI != null || photo != null && photoURI == null}:
     * the photo is only kept when the uploaded file and its URI are both present or both absent.
     */
    static Stream<Arguments> photoAndUriCombinations() {
        final MultipartFile file = mock(MultipartFile.class);
        return Stream.of(
                arguments(null, null, false),
                arguments(null, "photo.jpg", true),
                arguments(file, null, true),
                arguments(file, "photo.jpg", false));
    }

    @ParameterizedTest
    @MethodSource("photoAndUriCombinations")
    void createKeepsPhotoOnlyWhenFileAndUriAgree(MultipartFile photo, String photoURI, boolean discarded) {
        final var request = new CreateAuthorRequest("Manuel Antonio Pina", "Jornalista e escritor", photo, photoURI);
        when(mapper.create(request)).thenReturn(author);
        when(authorRepository.save(author)).thenReturn(author);

        assertSame(author, service.create(request));
        assertEquals(discarded ? null : photo, request.getPhoto());
        assertEquals(discarded ? null : photoURI, request.getPhotoURI());
    }

    @Test
    void partialUpdateOfUnknownAuthorThrowsAndDoesNotSave() {
        when(authorRepository.findByAuthorNumber(99L)).thenReturn(Optional.empty());
        final var request = new UpdateAuthorRequest("Nova bio", null, null, null);

        assertThrows(NotFoundException.class, () -> service.partialUpdate(99L, request, 0));
        verify(authorRepository, never()).save(any());
    }

    @Test
    void partialUpdateWithStaleVersionThrowsAndDoesNotSave() {
        when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.of(author));
        final var request = new UpdateAuthorRequest("Nova bio", null, null, null);

        assertThrows(StaleObjectStateException.class, () -> service.partialUpdate(1L, request, 5));
        verify(authorRepository, never()).save(any());
    }

    @Test
    void partialUpdateChangesOnlyTheProvidedFieldsAndSaves() {
        when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.of(author));
        when(authorRepository.save(author)).thenReturn(author);

        service.partialUpdate(1L, new UpdateAuthorRequest("Nova bio", null, null, null), 0);

        verify(authorRepository).save(author);
        assertEquals("Nova bio", author.getBio());
        assertEquals("Manuel Antonio Pina", author.getName());
    }

    /**
     * Transparent box, condition coverage of the same photo/URI rule in {@code partialUpdate}:
     * the author's photo only changes when the new file and its URI are both present.
     */
    static Stream<Arguments> patchPhotoAndUriCombinations() {
        final MultipartFile file = mock(MultipartFile.class);
        return Stream.of(
                arguments(null, null, "foto.jpg"),
                arguments(null, "nova.jpg", "foto.jpg"),
                arguments(file, null, "foto.jpg"),
                arguments(file, "nova.jpg", "nova.jpg"));
    }

    @ParameterizedTest
    @MethodSource("patchPhotoAndUriCombinations")
    void partialUpdateChangesPhotoOnlyWhenFileAndUriAgree(MultipartFile photo, String photoURI, String expectedPhoto) {
        final Author withPhoto = new Author("Manuel Antonio Pina", "Jornalista e escritor", "foto.jpg");
        when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.of(withPhoto));
        when(authorRepository.save(withPhoto)).thenReturn(withPhoto);

        assertSame(withPhoto, service.partialUpdate(1L, new UpdateAuthorRequest(null, null, photo, photoURI), 0));
        assertEquals(expectedPhoto, withPhoto.getPhoto().getPhotoFile());
    }

    @Test
    void removeAuthorPhotoRemovesItFromTheAuthorAndDeletesTheStoredPhoto() {
        final Author withPhoto = spy(new Author("Manuel Antonio Pina", "Jornalista e escritor", "foto.jpg"));
        when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.of(withPhoto));
        when(authorRepository.save(withPhoto)).thenReturn(withPhoto);

        assertEquals(Optional.of(withPhoto), service.removeAuthorPhoto(1L, 0));

        verify(withPhoto).removePhoto(0);
        verify(photoRepository).deleteByPhotoFile("foto.jpg");
        assertNull(withPhoto.getPhoto());
    }

    @Test
    void findTopAuthorByLendingsAsksForTheFirstPageOfFive() {
        final AuthorLendingView top = new AuthorLendingView();
        when(authorRepository.findTopAuthorByLendings(any())).thenReturn(new PageImpl<>(List.of(top)));
        final ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);

        assertEquals(List.of(top), service.findTopAuthorByLendings());

        verify(authorRepository).findTopAuthorByLendings(page.capture());
        assertEquals(0, page.getValue().getPageNumber());
        assertEquals(5, page.getValue().getPageSize());
    }
}
