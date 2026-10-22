package pt.psoft.g1.psoftg1.readermanagement.unit;

import pt.psoft.g1.psoftg1.readermanagement.model.*;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.readermanagement.services.UpdateReaderRequest;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ReaderTest {
    @Test
    void ensureValidReaderDetailsAreCreated() {
        Reader mockReader = mock(Reader.class);
        assertDoesNotThrow(() -> new ReaderDetails(123, mockReader, "2010-01-01", "912345678", true, false, false,null, null));
    }

    @Test
    void ensureExceptionIsThrownForNullReader() {
        assertThrows(IllegalArgumentException.class, () -> new ReaderDetails(123, null, "2010-01-01", "912345678", true, false, false,null,null));
    }

    @Test
    void ensureExceptionIsThrownForNullPhoneNumber() {
        Reader mockReader = mock(Reader.class);
        assertThrows(IllegalArgumentException.class, () -> new ReaderDetails(123, mockReader, "2010-01-01", null, true, false, false,null,null));
    }

    @Test
    void ensureExceptionIsThrownForNoGdprConsent() {
        Reader mockReader = mock(Reader.class);
        assertThrows(IllegalArgumentException.class, () -> new ReaderDetails(123, mockReader, "2010-01-01", "912345678", false, false, false,null,null));
    }

    @Test
    void ensureGdprConsentIsTrue() {
        Reader mockReader = mock(Reader.class);
        ReaderDetails readerDetails = new ReaderDetails(123, mockReader, "2010-01-01", "912345678", true, false, false,null,null);
        assertTrue(readerDetails.isGdprConsent());
    }

    @Test
    void ensurePhotoCanBeNull_AkaOptional() {
        Reader mockReader = mock(Reader.class);
        ReaderDetails readerDetails = new ReaderDetails(123, mockReader, "2010-01-01", "912345678", true, false, false,null,null);
        assertNull(readerDetails.getPhoto());
    }

    @Test
    void ensureValidPhoto() {
        Reader mockReader = mock(Reader.class);
        ReaderDetails readerDetails = new ReaderDetails(123, mockReader, "2010-01-01", "912345678", true, false, false,"readerPhotoTest.jpg",null);
        Photo photo = readerDetails.getPhoto();

        //This is here to force the test to fail if the photo is null
        assertNotNull(photo);

        String readerPhoto = photo.getPhotoFile();
        assertEquals("readerPhotoTest.jpg", readerPhoto);
    }

    @Test
    void ensureInterestListCanBeNullOrEmptyList_AkaOptional() {
        Reader mockReader = mock(Reader.class);
        ReaderDetails readerDetailsNullInterestList = new ReaderDetails(123, mockReader, "2010-01-01", "912345678", true, false, false,"readerPhotoTest.jpg",null);
        assertNull(readerDetailsNullInterestList.getInterestList());

        ReaderDetails readerDetailsInterestListEmpty = new ReaderDetails(123, mockReader, "2010-01-01", "912345678", true, false, false,"readerPhotoTest.jpg",new ArrayList<>());
        assertEquals(0, readerDetailsInterestListEmpty.getInterestList().size());
    }

    @Test
    void ensureInterestListCanTakeAnyValidGenre() {
        Reader mockReader = mock(Reader.class);
        Genre g1 = new Genre("genre1");
        Genre g2 = new Genre("genre2");
        List<Genre> genreList = new ArrayList<>();
        genreList.add(g1);
        genreList.add(g2);

        ReaderDetails readerDetails = new ReaderDetails(123, mockReader, "2010-01-01", "912345678", true, false, false,"readerPhotoTest.jpg",genreList);
        assertEquals(2, readerDetails.getInterestList().size());
    }

    // Optimistic concurrency and partial update of the reader details
    private ReaderDetails readerDetailsWithVersion(long version) {
        final Reader reader = Reader.newReader("ana@gmail.com", "Anaanaana!1", "Ana Silva");
        final ReaderDetails details = new ReaderDetails(1, reader, "2000-01-01", "912345678", true, true, false, null, null);
        ReflectionTestUtils.setField(details, "version", version);
        return details;
    }

    @Test
    void ensurePatchWithStaleVersionIsRejected() {
        final ReaderDetails details = readerDetailsWithVersion(2);
        final var request = new UpdateReaderRequest();
        request.setPhoneNumber("219999999");

        assertThrows(ConflictException.class, () -> details.applyPatch(1, request, null, null));
        assertEquals("912345678", details.getPhoneNumber());
    }

    @Test
    void ensurePatchChangesProvidedFieldsAndConsents() {
        final ReaderDetails details = readerDetailsWithVersion(2);
        final var request = new UpdateReaderRequest();
        request.setPhoneNumber("219999999");
        request.setFullName("Ana Maria Silva");
        request.setThirdParty(true);

        details.applyPatch(2, request, null, List.of(new Genre("Romance")));

        assertEquals("219999999", details.getPhoneNumber());
        assertEquals("Ana Maria Silva", details.getReader().getName().toString());
        assertEquals("2000-01-01", details.getBirthDate().getBirthDate().toString());
        assertFalse(details.isMarketingConsent());
        assertTrue(details.isThirdPartySharingConsent());
        assertEquals(1, details.getInterestList().size());
    }

    @Test
    void ensurePatchWithInvalidPhoneNumberIsRejected() {
        final ReaderDetails details = readerDetailsWithVersion(2);
        final var request = new UpdateReaderRequest();
        request.setPhoneNumber("512345678");

        assertThrows(IllegalArgumentException.class, () -> details.applyPatch(2, request, null, null));
    }

    @Test
    void ensureReaderNumberUsesTheCurrentYear() {
        assertEquals(LocalDate.now().getYear() + "/1", readerDetailsWithVersion(0).getReaderNumber());
    }
}
