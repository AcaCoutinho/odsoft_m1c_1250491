package pt.psoft.g1.psoftg1.usermanagement.unit;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.model.User;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void ensureReaderHasOnlyTheReaderRole() {
        final Reader reader = Reader.newReader("ana@gmail.com", "Anaanaana!1", "Ana Silva");
        assertEquals(Set.of(new Role(Role.READER)), reader.getAuthorities());
    }

    @Test
    void ensureLibrarianHasOnlyTheLibrarianRole() {
        final Librarian librarian = Librarian.newLibrarian("maria@gmail.com", "Mariaroberta!123", "Maria Roberta");
        assertEquals(Set.of(new Role(Role.LIBRARIAN)), librarian.getAuthorities());
    }

    @Test
    void ensurePasswordIsStoredHashed() {
        final User user = User.newUser("ana@gmail.com", "Anaanaana!1", "Ana Silva");
        assertNotEquals("Anaanaana!1", user.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches("Anaanaana!1", user.getPassword()));
    }

    @Test
    void ensureWeakPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> User.newUser("ana@gmail.com", "fraca", "Ana Silva"));
    }

    @Test
    void ensureInvalidNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> User.newUser("ana@gmail.com", "Anaanaana!1", "Ana!"));
    }

    @Test
    void ensureDisabledUserCannotAuthenticate() {
        final User user = User.newUser("ana@gmail.com", "Anaanaana!1", "Ana Silva");
        user.setEnabled(false);
        assertFalse(user.isAccountNonLocked());
        assertFalse(user.isAccountNonExpired());
        assertFalse(user.isCredentialsNonExpired());
    }
}
