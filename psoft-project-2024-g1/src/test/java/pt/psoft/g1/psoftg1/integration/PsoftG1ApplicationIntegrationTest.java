package pt.psoft.g1.psoftg1.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke test: the whole Spring context starts and the public endpoints answer, so the build is
 * minimally testable. Integration level, opaque box, automatic, no doubles.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PsoftG1ApplicationIntegrationTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthEndpointIsUp() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void environmentEndpointIsPublicAndShowsTheCommit() throws Exception {
		mockMvc.perform(get("/api/public/environment"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activeProfiles").isArray())
				.andExpect(jsonPath("$.commit").value("local"));
	}
}
