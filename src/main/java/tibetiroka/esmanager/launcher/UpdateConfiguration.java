/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.launcher;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tibetiroka.esmanager.config.AppConfiguration;

import java.util.HashMap;

/**
 * Stores the update configuration for a specific version. This configuration might contain an array of file-specific migrations that specify how to update the launcher.
 *
 * @since 0.0.3
 */
public class UpdateConfiguration {
	/**
	 * The array of file-specific migrations for upgrading the launcher. Might be null or empty.
	 *
	 * @since 0.0.3
	 */
	private @NotNull Migration @Nullable [] migrations;
	/**
	 * The semantic version this configuration updates to.
	 *
	 * @since 0.0.3
	 */
	private @NotNull String version;

	/**
	 * Gets the array of file-specific migrations for upgrading the launcher. Might be null or empty.
	 *
	 * @return {@link #migrations}
	 * @since 0.0.3
	 */
	public @NotNull Migration @Nullable [] getMigrations() {
		return migrations;
	}

	/**
	 * Gets the semantic version this configuration updates to.
	 *
	 * @return {@link #version}
	 * @since 0.0.3
	 */
	public @NotNull String getVersion() {
		return version;
	}

	/**
	 * The supported operating systems.
	 *
	 * @since 0.0.3
	 */
	public enum OS {
		LINUX {
			@Override
			public boolean isCurrentOs() {
				return AppConfiguration.isLinux();
			}
		}, WINDOWS {
			@Override
			public boolean isCurrentOs() {
				return AppConfiguration.isWindows();
			}
		}, MAC {
			@Override
			public boolean isCurrentOs() {
				return !AppConfiguration.isWindows() && !AppConfiguration.isLinux();
			}
		}, ANY {
			@Override
			public boolean isCurrentOs() {
				return true;
			}
		};

		public abstract boolean isCurrentOs();
	}

	/**
	 * Specifies how to choose a download artifact from the new version based on a file name from the old version.
	 *
	 * @since 0.0.3
	 */
	public static class Migration {
		/**
		 * A map of custom instructions for compatibility.
		 *
		 * @since 0.0.3
		 */
		private @Nullable HashMap<@NotNull String, ?> customInstructions;
		/**
		 * The operating system to use, if this migration is OS-specific.
		 *
		 * @since 0.0.3
		 */
		private @Nullable OS os;
		/**
		 * The name of the source file (the currently installed launcher executable)
		 *
		 * @since 0.0.3
		 */
		private @NotNull String source;
		/**
		 * The name of the target file (the release artifact to download)
		 *
		 * @since 0.0.3
		 */
		private @NotNull String target;
		/**
		 * The version this migration migrates files from.
		 *
		 * @since 0.0.3
		 */
		private @NotNull String version;

		/**
		 * Gets a map of custom instructions provided with this migration. This is provided for two-way compatibility between launcher versions.
		 *
		 * @return {@link #customInstructions}
		 * @since 0.0.3
		 */
		public @Nullable HashMap<@NotNull String, ?> getCustomInstructions() {
			return customInstructions;
		}

		/**
		 * Gets the operating system this migration is specific to, or {@link OS#ANY} if no os is specified.
		 *
		 * @return {@link #os} or {@link OS#ANY}
		 * @since 0.0.3
		 */
		public @NotNull OS getOperatingSystem() {
			return os == null ? OS.ANY : os;
		}

		/**
		 * Gets the name of the source file (the currently installed launcher executable).
		 *
		 * @return {@link #source}
		 * @since 0.0.3
		 */
		public @NotNull String getSource() {
			return source;
		}

		/**
		 * Gets the name of the target file (the release artifact to download).
		 *
		 * @return {@link #source}
		 * @since 0.0.3
		 */
		public @NotNull String getTarget() {
			return target;
		}

		/**
		 * Gets the version this migration migrates files from.
		 *
		 * @return {@link #version}
		 * @since 0.0.3
		 */
		public @NotNull String getVersion() {
			return version;
		}
	}
}