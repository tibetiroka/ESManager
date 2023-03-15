/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Utilities related to semantic versioning.
 *
 * @since 0.0.1
 */
public class VersioningUtils {
	/**
	 * Sorts the specified semantic version numbers, from latest to oldest.
	 *
	 * @return The comparator
	 * @since 0.0.1
	 */
	public static @NotNull Comparator<@NotNull String> semVerComparator() {
		return (o1, o2) -> {
			if(o1.startsWith("v")) {
				o1 = o1.substring("v".length());
			}
			if(o2.startsWith("v")) {
				o2 = o2.substring("v".length());
			}
			//
			String[] firstParts = o1.split("\\.");
			String[] secondParts = o2.split("\\.");
			int i = 0;
			for(; i < firstParts.length && i < secondParts.length; i++) {
				int first = Integer.parseInt(firstParts[i]);
				int second = Integer.parseInt(secondParts[i]);
				// greatest release number comes first
				if(first < second) {
					return 1;
				} else if(second < first) {
					return -1;
				}
			}
			if(firstParts.length > i) {
				return -1;
			} else if(secondParts.length > i) {
				return 1;
			}
			return 0;
		};
	}
	public static boolean isSameRelease(String first, String second){
		if(first.startsWith("v")){
			first = first.substring("v".length());
		}
		if(second.startsWith("v")){
			second = second.substring("v".length());
		}
		return first.equalsIgnoreCase(second);
	}
}