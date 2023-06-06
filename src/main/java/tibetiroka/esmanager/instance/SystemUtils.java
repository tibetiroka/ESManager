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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.config.AppConfiguration;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;

import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * Utilities for managing os-dependent utilities, such as selecting programs for opening files.
 *
 * @since 1.0.0
 */
public class SystemUtils {
	private static final Logger log = LoggerFactory.getLogger(SystemUtils.class);

	/**
	 * Opens the specified directory, if supported by the operating system.
	 *
	 * @param file The directory to open
	 * @since 1.0.0
	 */
	public static void openDirectory(@NotNull File file) {
		log.info(localize("log.directory.open", file.getPath()));
		if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.OPEN) && Desktop.getDesktop().isSupported(Action.BROWSE) && Desktop.getDesktop().isSupported(Action.BROWSE_FILE_DIR)) {
			Desktop.getDesktop().browseFileDirectory(file);
		} else if(isGioSupported()) {
			ProcessBuilder builder = new ProcessBuilder("gio", "open", file.getAbsolutePath());
			try {
				builder.start();
			} catch(IOException e) {
				log.error(localize("log.directory.open.fail", file.getPath(), e.getMessage()));
				throw new RuntimeException(e);
			}
		} else {
			log.error(localize("log.directory.open.unsupported", file.getPath()));
		}
	}

	/**
	 * Opens the specified file for editing, if supported.
	 *
	 * @param file The file to open
	 * @since 1.0.0
	 */
	public static void openFile(@NotNull File file) {
		if(!file.exists()) {
			return;
		}

		log.info(localize("log.file.open", file.getPath()));
		if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.OPEN) && Desktop.getDesktop().isSupported(Action.BROWSE) && Desktop.getDesktop().isSupported(Action.APP_OPEN_FILE) && Desktop.getDesktop().isSupported(Action.APP_OPEN_URI)) {
			try {
				Desktop.getDesktop().open(file);
			} catch(IOException e) {
				log.error(localize("log.file.open.fail", file.getPath(), e.getMessage()));
				throw new RuntimeException(e);
			}
		} else if(isGioSupported()) {
			ProcessBuilder builder = new ProcessBuilder("gio", "open", file.getAbsolutePath());
			try {
				builder.start();
			} catch(IOException e) {
				log.error(localize("log.file.open.fail", file.getPath(), e.getMessage()));
				throw new RuntimeException(e);
			}
		} else {
			log.error(localize("log.file.open.unsupported", file.getPath()));
		}
	}

	/**
	 * Checks whether the {@code gio} command is available on this platform. Always returns false on Windows.
	 *
	 * @return True if available, false otherwise
	 * @since 1.0.0
	 */
	private static boolean isGioSupported() {
		if(AppConfiguration.isWindows()) {
			return false;
		}
		try {
			Process p = Runtime.getRuntime().exec(new String[]{"which", "gio"});
			int result = p.waitFor();
			return result == 0;
		} catch(Exception e) {
			return false;
		}
	}
}