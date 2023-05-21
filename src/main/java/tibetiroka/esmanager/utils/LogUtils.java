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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Logging utilities, mainly used for getting around the limitations of the default streams.
 *
 * @since 0.0.6
 */
public class LogUtils {
	private static final Logger log = LoggerFactory.getLogger(LogUtils.class);

	/**
	 * Logs all incoming messages from the input stream at the specified level.
	 *
	 * @param stream The stream to read
	 * @param level  The logging level
	 * @since 0.0.6
	 */
	public static void log(@NotNull InputStream stream, @NotNull Level level) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		for(String line = reader.readLine(); line != null; line = reader.readLine()) {
			log.atLevel(level).log(line);
		}
	}

	/**
	 * Logs all incoming messages from the input stream at the specified level without blocking the caller thread.
	 *
	 * @param stream The stream to read
	 * @param level  The logging level
	 * @since 0.0.6
	 */
	public static void logAsync(@NotNull InputStream stream, @NotNull Level level) {
		new Thread(() -> {
			try {
				log(stream, level);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}, "Logging thread for stream").start();
	}
}