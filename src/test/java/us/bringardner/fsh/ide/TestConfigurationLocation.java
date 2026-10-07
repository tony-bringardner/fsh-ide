package us.bringardner.fsh.ide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The configuration moved from ~/.bjlshellIde to ~/.fsh-ide (BJL-88). */
public class TestConfigurationLocation {

	@TempDir
	Path home;

	@Test
	public void usesTheNewFolder() {
		File file = Configuration.configFileIn(home.toFile());
		assertEquals(home.resolve(".fsh-ide").resolve("Config.xml").toFile(), file);
		assertTrue(file.getParentFile().isDirectory());
		assertFalse(file.exists());
	}

	@Test
	public void copiesTheOldConfigurationTheFirstTime() throws IOException {
		Path old = home.resolve(".bjlshellIde");
		Files.createDirectories(old);
		Files.writeString(old.resolve("Config.xml"), "<old/>");

		File file = Configuration.configFileIn(home.toFile());
		assertEquals("<old/>", Files.readString(file.toPath()));
	}

	@Test
	public void keepsTheNewConfigurationOnceItExists() throws IOException {
		Path old = home.resolve(".bjlshellIde");
		Files.createDirectories(old);
		Files.writeString(old.resolve("Config.xml"), "<old/>");
		Path now = home.resolve(".fsh-ide");
		Files.createDirectories(now);
		Files.writeString(now.resolve("Config.xml"), "<new/>");

		File file = Configuration.configFileIn(home.toFile());
		assertEquals("<new/>", Files.readString(file.toPath()));
	}
}
