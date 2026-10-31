package pt.psoft.g1.psoftg1.shared.unit;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.shared.model.EntityWithPhoto;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit, opaque box, functional. Equivalence classes of the photo URI: absent, a valid path, and a
 * string that is not a valid path (the photo is then dropped instead of keeping a broken reference).
 */
class EntityWithPhotoTest {
    private static class EntityWithPhotoImpl extends EntityWithPhoto { }

    private final EntityWithPhoto entity = new EntityWithPhotoImpl();

    @Test
    void validUriSetsThePhoto() {
        entity.setPhoto("photoTest.jpg");
        assertEquals("photoTest.jpg", entity.getPhoto().getPhotoFile());
    }

    @Test
    void nullUriRemovesThePhoto() {
        entity.setPhoto("photoTest.jpg");
        entity.setPhoto(null);
        assertNull(entity.getPhoto());
    }

    @Test
    void invalidPathIsDropped() {
        entity.setPhoto("foto\0.jpg");
        assertNull(entity.getPhoto());
    }
}
