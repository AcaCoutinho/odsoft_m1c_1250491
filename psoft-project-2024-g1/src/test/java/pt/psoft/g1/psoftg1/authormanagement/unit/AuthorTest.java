package pt.psoft.g1.psoftg1.authormanagement.unit;

import pt.psoft.g1.psoftg1.authormanagement.model.*;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit, opaque box, functional: invariants of the {@link Author} aggregate (valid name and bio,
 * optional photo) and the optimistic concurrency check of {@code applyPatch}.
 */
class AuthorTest {
    private final String validName = "João Alberto";
    private final String validBio = "O João Alberto nasceu em Chaves e foi pedreiro a maior parte da sua vida.";

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Ana!", "<b>Ana</b>"})
    void ensureInvalidNameIsRejected(String name) {
        assertThrows(IllegalArgumentException.class, () -> new Author(name, validBio, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void ensureMissingBioIsRejected(String bio) {
        assertThrows(IllegalArgumentException.class, () -> new Author(validName, bio, null));
    }

    @Test
    void ensureAuthorIsCreatedWithNameAndBio() {
        final Author author = new Author(validName, validBio, null);
        assertEquals(validName, author.getName());
        assertEquals(validBio, author.getBio());
    }

    @Test
    void ensurePhotoCanBeNull_AkaOptional() {
        assertNull(new Author(validName, validBio, null).getPhoto());
    }

    @Test
    void ensureValidPhoto() {
        final Author author = new Author(validName, validBio, "photoTest.jpg");
        assertEquals("photoTest.jpg", author.getPhoto().getPhotoFile());
    }

    @Test
    void whenVersionIsStaleItIsNotPossibleToPatch() {
        final var subject = new Author(validName, validBio, null);
        final var request = new UpdateAuthorRequest("Outra bio", "Outro Nome", null, null);

        assertThrows(StaleObjectStateException.class, () -> subject.applyPatch(999, request));
        assertEquals(validName, subject.getName());
    }

    @Test
    void ensurePatchWithCurrentVersionChangesTheProvidedFields() {
        final var subject = new Author(validName, validBio, null);

        subject.applyPatch(0, new UpdateAuthorRequest("Outra bio", "Outro Nome", null, null));

        assertEquals("Outro Nome", subject.getName());
        assertEquals("Outra bio", subject.getBio());
    }
}
