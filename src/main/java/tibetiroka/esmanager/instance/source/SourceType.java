/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.instance.source;

/**
 * The different types of sources that can be used in a source. Not all {@link Source} implementations support all types.
 *
 * @since 0.0.1
 */
public enum SourceType {
	/**
	 * A direct download is a source type where the single executable can be downloaded directly from a specified address.
	 *
	 * @since 0.0.1
	 */
	DIRECT_DOWNLOAD,
	/**
	 * A branch of a git repository from which the game can be built.
	 *
	 * @since 0.0.1
	 */
	BRANCH,
	/**
	 * A pull request of a git repository from which the game can be built.
	 *
	 * @since 0.0.1
	 */
	PULL_REQUEST,
	/**
	 * A locally installed single executable file.
	 *
	 * @since 0.0.1
	 */
	LOCAL_EXECUTABLE,
	/**
	 * A release in a git repository from which an artifact can be downloaded, or the game can be built.
	 *
	 * @since 0.0.1
	 */
	RELEASE,
	/**
	 * The latest release in a git repository. Acts like a {@link #RELEASE} that changes target when a new release comes out.
	 *
	 * @since 0.0.1
	 */
	LATEST_RELEASE,
	/**
	 * Multiple git sources combined into a single source. Merge conflicts between sources might cause issues.
	 *
	 * @since 0.0.1
	 */
	MULTIPLE_SOURCES;
}