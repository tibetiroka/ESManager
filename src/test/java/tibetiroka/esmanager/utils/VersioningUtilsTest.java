/*
 * Copyright (c) 2024 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.utils;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class VersioningUtilsTest {
	@Test
	void isNewerRelease() {
		assertTrue(VersioningUtils.isNewerRelease("0.1", "1.0"));
		assertTrue(VersioningUtils.isNewerRelease("0.1", "0.1.1"));
		assertTrue(VersioningUtils.isNewerRelease("v0.1", "0.2.0"));
		assertTrue(VersioningUtils.isNewerRelease("0.1", "v0.2.0"));
		assertTrue(VersioningUtils.isNewerRelease("1.0.1", "1.0.1.1"));
	}

	@Test
	void isSameRelease() {
		assertTrue(VersioningUtils.isSameRelease("0.1", "0.1.0"));
		assertTrue(VersioningUtils.isSameRelease("v0.1", "0.1.0"));
		assertTrue(VersioningUtils.isSameRelease("0.1", "v0.1.0"));
		assertTrue(VersioningUtils.isSameRelease("v0.1", "v0.1.0"));
	}
}