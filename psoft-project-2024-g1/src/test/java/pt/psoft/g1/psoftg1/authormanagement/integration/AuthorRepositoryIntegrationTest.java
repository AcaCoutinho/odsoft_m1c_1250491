package pt.psoft.g1.psoftg1.authormanagement.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test of the author name queries against the H2 database ({@code @DataJpaTest}, rolled back).
 * <p>Based on https://www.baeldung.com/spring-boot-testing
 */
@DataJpaTest
public class AuthorRepositoryIntegrationTest {
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private AuthorRepository authorRepository;

    @Test
    public void whenFindByName_thenReturnAuthor() {
        // given
        Author alex = new Author("Alex", "O Alex escreveu livros", null);
        entityManager.persist(alex);
        entityManager.flush();

        // when
        List<Author> list = authorRepository.searchByNameName(alex.getName());

        // then
        assertThat(list).isNotEmpty();
        assertThat(list.get(0).getName())
                .isEqualTo(alex.getName());
    }

    @Test
    public void whenFindByUnknownName_thenReturnEmpty() {
        assertThat(authorRepository.searchByNameName("Ninguem")).isEmpty();
    }

    @Test
    public void whenSearchByNamePrefix_thenReturnMatchingAuthorsOnly() {
        entityManager.persist(new Author("Alexandre Pereira", "Professor", null));
        entityManager.persist(new Author("Alexandra Lucas", "Escritora", null));
        entityManager.persist(new Author("Manuel Pina", "Jornalista", null));
        entityManager.flush();

        assertThat(authorRepository.searchByNameNameStartsWith("Alexand"))
                .extracting(Author::getName)
                .containsExactlyInAnyOrder("Alexandre Pereira", "Alexandra Lucas");
    }
}
