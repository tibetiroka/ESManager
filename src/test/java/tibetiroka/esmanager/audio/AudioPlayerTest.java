/*
 * Copyright (c) 2024 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.audio;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;
import tibetiroka.esmanager.config.AppConfiguration;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AudioPlayerTest {
	@Test
	void fileList() throws URISyntaxException {
		List<String> fileList = (List<String>) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.audio.files");
		HashSet<String> files = new HashSet<>(fileList);
		// check for duplicates
		assertEquals(files.size(), fileList.size());
		// check that all files exist
		for(String file : files) {
			assertNotNull(AudioPlayer.class.getResource(file));
		}
		// check that no files are missing
		if(!AppConfiguration.isNativeImage()) {
			Set<String> foundFiles;
			if(AudioPlayer.class.getResource("./") != null) {
				foundFiles = new HashSet<>(IOUtils.readLines(AudioPlayer.class.getResourceAsStream("./"), StandardCharsets.UTF_8));
			} else {
				foundFiles = new HashSet<>(FileUtils.listFiles(new File(new File(AudioPlayer.class.getProtectionDomain().getCodeSource().getLocation().toURI()), AudioPlayer.class.getPackageName().replace('.', '/')), null, false).stream().map(File::getName).toList());
			}
			foundFiles.removeIf(s -> !s.endsWith(".mp3"));
			assertEquals(files, new HashSet<>(foundFiles));
		}
	}
}