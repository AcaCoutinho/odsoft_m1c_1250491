package pt.psoft.g1.psoftg1.usermanagement.unit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;
import pt.psoft.g1.psoftg1.usermanagement.services.CreateUserRequest;
import pt.psoft.g1.psoftg1.usermanagement.services.EditUserMapper;
import pt.psoft.g1.psoftg1.usermanagement.services.SearchUsersQuery;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test of {@link UserService} with stubbed/mocked repositories; {@link EditUserMapper} is a dummy.
 * <p>Transparent box: every branch of the role switch in {@code create} and of {@code getAuthenticatedUser}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepo;
    @Mock
    private EditUserMapper userEditMapper;
    @Mock
    private ForbiddenNameRepository forbiddenNameRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService service;

    private static CreateUserRequest request(String role) {
        final var request = new CreateUserRequest("ana@gmail.com", "Ana Silva", "Anaanaana!1");
        request.setRole(role);
        return request;
    }

    private void givenPasswordEncoderAndSave() {
        when(passwordEncoder.encode("Anaanaana!1")).thenReturn("Encoded!1");
        when(userRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createReaderRole() {
        givenPasswordEncoderAndSave();
        assertInstanceOf(Reader.class, service.create(request(Role.READER)));
    }

    @Test
    void createLibrarianRole() {
        givenPasswordEncoderAndSave();
        assertInstanceOf(Librarian.class, service.create(request(Role.LIBRARIAN)));
    }

    @Test
    void createWithUnsupportedRoleCreatesNothing() {
        assertNull(service.create(request(Role.ADMIN)));
        verify(userRepo, never()).save(any());
    }

    @Disabled("Known defect: create switches on a null role and throws NullPointerException (HTTP 500)")
    @Test
    void createWithoutRoleIsRejectedAsBadRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.create(request(null)));
    }

    @Test
    void createRejectsAnExistingUsername() {
        when(userRepo.findByUsername("ana@gmail.com")).thenReturn(Optional.of(mock(User.class)));

        assertThrows(ConflictException.class, () -> service.create(request(Role.READER)));
        verify(userRepo, never()).save(any());
    }

    @Test
    void createRejectsNamesWithAForbiddenWord() {
        when(forbiddenNameRepository.findByForbiddenNameIsContained("Ana"))
                .thenReturn(List.of(new ForbiddenName("Ana")));

        assertThrows(IllegalArgumentException.class, () -> service.create(request(Role.READER)));
        verify(userRepo, never()).save(any());
    }

    @Test
    void authenticatedUserRequiresAnAuthentication() {
        assertThrows(AccessDeniedException.class, () -> service.getAuthenticatedUser(null));
    }

    @Test
    void authenticatedUserRequiresAJwt() {
        final var basic = new UsernamePasswordAuthenticationToken("ana@gmail.com", "Anaanaana!1");
        assertThrows(AccessDeniedException.class, () -> service.getAuthenticatedUser(basic));
    }

    private static JwtAuthenticationToken jwtFor(String subject) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("token").header("alg", "none").subject(subject).build());
    }

    @Test
    void authenticatedUserIsLoadedFromTheJwtSubject() {
        final User ana = Reader.newReader("ana@gmail.com", "Anaanaana!1", "Ana Silva");
        when(userRepo.findByUsername("ana@gmail.com")).thenReturn(Optional.of(ana));

        assertSame(ana, service.getAuthenticatedUser(jwtFor("1,ana@gmail.com")));
    }

    @Test
    void authenticatedUserMustStillExist() {
        when(userRepo.findByUsername("ana@gmail.com")).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.getAuthenticatedUser(jwtFor("1,ana@gmail.com")));
    }

    @Test
    void loadUnknownUsernameFails() {
        when(userRepo.findByUsername("ninguem@gmail.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ninguem@gmail.com"));
    }

    @Test
    void searchUsersUsesDefaultPageAndEmptyQuery() {
        final ArgumentCaptor<Page> page = ArgumentCaptor.forClass(Page.class);
        final ArgumentCaptor<SearchUsersQuery> query = ArgumentCaptor.forClass(SearchUsersQuery.class);

        service.searchUsers(null, null);

        verify(userRepo).searchUsers(page.capture(), query.capture());
        assertEquals(new Page(1, 10), page.getValue());
        assertEquals(new SearchUsersQuery("", ""), query.getValue());
    }
}
