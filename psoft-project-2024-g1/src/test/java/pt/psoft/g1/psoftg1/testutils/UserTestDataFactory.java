package pt.psoft.g1.psoftg1.testutils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.usermanagement.api.UserView;
import pt.psoft.g1.psoftg1.usermanagement.api.UserViewMapper;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Based on https://github.com/Yoh0xFF/java-spring-security-example
 *
 */
@Service
public class UserTestDataFactory {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserViewMapper userViewMapper;

    /**
     * Persists the user the same way the bootstrap does. UserService.create is not used because it
     * encodes the password twice (see AuthApiIntegrationTest.testRegisteredUserCanLogin).
     */
    @Transactional
    public UserView createUser(final String username, final String fullName, final String password) {
        final User user = userRepository.save(Reader.newReader(username, password, fullName));

        assertNotNull(user.getId(), "User id must not be null!");
        assertEquals(fullName, user.getName().toString(), "User name update isn't applied!");

        return userViewMapper.toUserView(user);
    }

    public UserView createUser(final String username, final String fullName) {
        return createUser(username, fullName, "Test12345_");
    }

    @Transactional
    public void deleteUser(final Long id) {
        userService.delete(id);
    }

}