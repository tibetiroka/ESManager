/*
 * Copyright (c) 2024 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager;

import org.junit.jupiter.api.*;
import tibetiroka.esmanager.config.AppConfiguration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
	@Test
	void versionTest() throws IOException {
		if(!AppConfiguration.isNativeImage()) {
			String configVersion = (String) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.version");

			// check version in pom
			BufferedReader reader = new BufferedReader(new FileReader("pom.xml"));
			Pattern version = Pattern.compile(".*<version>.*</version>.*");
			for(String line = reader.readLine(); line != null; line = reader.readLine()) {
				if(version.matcher(line).matches()) {
					Pattern extractor = Pattern.compile("(?<=<version>).*(?=</version>)");
					Matcher matcher = extractor.matcher(line);
					matcher.find();
					String ver = matcher.group();
					assertEquals(configVersion, ver);
					break;
				}
			}
			reader.close();
		}
	}
}