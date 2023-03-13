/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.instance;

import org.jetbrains.annotations.NotNull;
import tibetiroka.esmanager.utils.VersioningUtils;

import java.util.Comparator;

/**
 * Utility class for ordering and choosing releases in the official repository.
 *
 * @since 0.0.1
 */
public class ReleaseUtils {
	/**
	 * Checks whether the specified tag conforms to the naming scheme.
	 *
	 * @param tag The tag to check
	 * @return True if standard
	 * @since 0.0.1
	 */
	public static boolean isStandardTag(@NotNull String tag) {
		if(!tag.startsWith("refs/tags/")) {
			return false;
		}
		tag = tag.substring("refs/tags/".length());
		if(tag.equals("continuous")) {
			return true;
		}
		String[] parts = tag.split("\\.");
		if(parts[0].startsWith("v")) {
			parts[0] = parts[0].substring("v".length());
		}
		for(String part : parts) {
			try {
				Integer.parseInt(part);
			} catch(Exception e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Creates a comparator that sorts the latest release to the first place.
	 *
	 * @return The comparator
	 * @since 0.0.1
	 */
	public static @NotNull Comparator<@NotNull String> latestFirst() {
		return (o1, o2) -> {
			if(!isStandardTag(o1) || !isStandardTag(o2)) {
				return String.CASE_INSENSITIVE_ORDER.compare(o1, o2);
			}
			o1 = standardizeTag(o1);
			o2 = standardizeTag(o2);
			if(o1.equals(o2)) {
				return 0;
			}
			if(o1.equals("continuous")) {
				return 1;
			} else if(o2.equals("continuous")) {
				return -1;
			} else {
				return VersioningUtils.semVerComparator().compare(o1, o2);
			}
		};
	}

	/**
	 * Creates a comparator that sorts the oldest release to the first place.
	 *
	 * @return The comparator
	 * @since 0.0.1
	 */
	public static @NotNull Comparator<@NotNull String> oldestFirst() {
		return latestFirst().reversed();
	}

	/**
	 * Gets the semver portion of the release name.
	 *
	 * @param tag The tag to parse
	 * @return The semver part
	 * @since 0.0.1
	 */
	private static @NotNull String standardizeTag(@NotNull String tag) {
		tag = tag.substring("refs/tags/".length());
		if(tag.startsWith("v")) {
			tag = tag.substring(1);
		}
		if(tag.endsWith("^{}")) {
			tag = tag.substring(0, tag.length() - "^{}".length());
		}
		return tag;
	}
}