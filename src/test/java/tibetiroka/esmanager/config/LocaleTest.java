/*
 * Copyright (c) 2024 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.config;

import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

public class LocaleTest {
	@BeforeAll
	static void setup() {
		AppConfiguration.loadLauncherConfiguration();
	}

	/**
	 * Checks that all default locales have the same entries specified.
	 */
	@Test
	void localeKeys() {
		ResourceBundle[] bundles = Launcher.LAUNCHER.builtinBundles.values().toArray(new ResourceBundle[0]);
		HashSet<String> keys = new HashSet<>(bundles[0].keySet());
		for(ResourceBundle bundle : bundles) {
			assertEquals(keys, bundle.keySet());
		}
	}
}