package pt.psoft.g1.psoftg1.shared.api;

import java.util.List;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Environment")
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "api/public")
public class EnvironmentApi {

	private final Environment environment;

	@Operation(summary = "Shows the active Spring profiles (dev or prod in the deployed environments) and the commit the image was built from")
	@GetMapping("environment")
	public Map<String, Object> environment() {
		return Map.of("activeProfiles", List.of(environment.getActiveProfiles()),
				"commit", environment.getProperty("APP_COMMIT", "local"));
	}
}
