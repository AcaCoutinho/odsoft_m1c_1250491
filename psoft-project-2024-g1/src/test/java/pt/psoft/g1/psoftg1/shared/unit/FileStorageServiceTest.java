package pt.psoft.g1.psoftg1.shared.unit;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.shared.services.FileStorageProperties;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test of {@link FileStorageService} on a temporary folder (a fake for the uploads folder).
 * <p>Opaque box: equivalence classes of the content type (png/jpeg vs. others vs. missing) and
 * boundary value analysis on the photo size limit ({@code file.photo_max_size} = 20000 bytes).
 */
class FileStorageServiceTest {
    private static final int MAX_SIZE = 20000;

    @TempDir
    Path uploads;
    private FileStorageService service;

    @BeforeEach
    void setUp() {
        final var properties = new FileStorageProperties();
        properties.setUploadDir(uploads.toString());
        properties.setPhotoMaxSize(MAX_SIZE);
        service = new FileStorageService(properties);
        // the stored file URI is built from the current HTTP request
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static MockMultipartFile photo(String contentType, int size) {
        return new MockMultipartFile("photo", "foto.png", contentType, new byte[size]);
    }

    @Test
    void noFileMeansNoPhoto() {
        assertNull(service.getRequestPhoto(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/png", "image/jpeg"})
    void pngAndJpegPhotosAreStored(String contentType) {
        final String fileName = service.getRequestPhoto(photo(contentType, 100));

        assertTrue(fileName.endsWith(".png"));
        assertTrue(Files.exists(uploads.resolve(fileName)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/gif", "application/pdf", "text/plain"})
    void otherContentTypesAreRejected(String contentType) {
        assertThrows(ValidationException.class, () -> service.getRequestPhoto(photo(contentType, 100)));
    }

    @Test
    void missingContentTypeIsRejected() {
        assertThrows(ValidationException.class, () -> service.getRequestPhoto(photo(null, 100)));
    }

    @Test
    void photoWithMaxSizeIsAccepted() {
        assertNotNull(service.getRequestPhoto(photo("image/png", MAX_SIZE)));
    }

    @Test
    void photoAboveMaxSizeIsRejected() {
        assertThrows(ValidationException.class, () -> service.getRequestPhoto(photo("image/png", MAX_SIZE + 1)));
    }

    @Test
    void storedFileIsNamedByThePrefixAndTheOriginalExtension() {
        assertEquals("abc.png", service.storeFile("abc", photo("image/png", 10)));
        assertTrue(Files.exists(uploads.resolve("abc.png")));
    }

    @Test
    void storedFileCanBeReadAndDeleted() throws IOException {
        Files.write(uploads.resolve("capa.jpg"), new byte[]{1, 2, 3});

        assertArrayEquals(new byte[]{1, 2, 3}, service.getFile("capa.jpg"));
        service.deleteFile("capa.jpg");
        assertFalse(Files.exists(uploads.resolve("capa.jpg")));
    }

    @Test
    void missingFileReadsAsNull() {
        assertNull(service.getFile("nao-existe.jpg"));
    }

    @Test
    void deletingAMissingFileIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.deleteFile("nao-existe.jpg"));
    }

    @Test
    void deletingNullIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteFile(null));
    }

    @Test
    void extensionIsTheTextAfterTheLastDot() {
        assertEquals(Optional.of("png"), service.getExtension("foto.final.png"));
        assertEquals(Optional.empty(), service.getExtension("sem-extensao"));
        assertEquals(Optional.empty(), service.getExtension(null));
    }
}
