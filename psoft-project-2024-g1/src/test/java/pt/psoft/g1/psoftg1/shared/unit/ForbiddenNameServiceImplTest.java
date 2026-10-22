package pt.psoft.g1.psoftg1.shared.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.services.ForbiddenNameServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Unit test of {@link ForbiddenNameServiceImpl} with a mocked repository, using the real
 * {@code forbiddenNames.txt} from the classpath.
 */
@ExtendWith(MockitoExtension.class)
class ForbiddenNameServiceImplTest {
    @Mock
    private ForbiddenNameRepository repo;
    @InjectMocks
    private ForbiddenNameServiceImpl service;

    @Test
    void onlyNamesNotYetStoredAreSaved() {
        when(repo.findByForbiddenName("Batman")).thenReturn(Optional.of(new ForbiddenName("Batman")));

        service.loadDataFromFile("forbiddenNames.txt");

        verify(repo).save(argThat(name -> name.getForbiddenName().equals("Hulk")));
        verify(repo, never()).save(argThat(name -> name.getForbiddenName().equals("Batman")));
    }

    @Test
    void missingFileFails() {
        assertThrows(RuntimeException.class, () -> service.loadDataFromFile("nao-existe.txt"));
        verify(repo, never()).save(any());
    }
}
