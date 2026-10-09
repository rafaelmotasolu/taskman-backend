package com.solutis.projeto.taskman_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class TaskmanBackendApplication {

	public static void main(String[] args) {
		loadDotEnvIfExists();
		SpringApplication.run(TaskmanBackendApplication.class, args);
	}

	private static void loadDotEnvIfExists() {
		Path envPath = Paths.get(".env");
		if (!Files.exists(envPath)) {
			envPath = Paths.get("../.env");
		}
		if (!Files.exists(envPath)) {
			return;
		}

		try {
			List<String> lines = Files.readAllLines(envPath);
			for (String line : lines) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}
				int equalsIndex = trimmed.indexOf('=');
				if (equalsIndex > 0) {
					String key = trimmed.substring(0, equalsIndex).trim();
					String value = trimmed.substring(equalsIndex + 1).trim();
					if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
						if (value.length() >= 2) {
							value = value.substring(1, value.length() - 1);
						}
					}
					if (System.getenv(key) == null && System.getProperty(key) == null) {
						System.setProperty(key, value);
					}
				}
			}
		} catch (IOException ignored) {
			// Fallback silently if .env cannot be read
		}
	}

}

