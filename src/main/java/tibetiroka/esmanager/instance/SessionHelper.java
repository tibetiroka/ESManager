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

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.instance.source.ReleaseSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * Utility class for starting and managing instance sessions.
 *
 * @since 0.0.1
 */
public class SessionHelper {
	/**
	 * Stores whether any sessions are running.
	 *
	 * @since 0.0.1
	 */
	public static final SimpleBooleanProperty ANY_RUNNING = new SimpleBooleanProperty(false);
	private static final Logger log = LoggerFactory.getLogger(SessionHelper.class);

	/**
	 * Starts the specified instance. It is assumed that this method is not called while an instance is running.
	 *
	 * @param instance The instance to start
	 * @param debug    Whether to start it in debug mode
	 * @since 0.0.1
	 */
	public static void start(@NotNull Instance instance, boolean debug) {
		Platform.runLater(() -> ANY_RUNNING.set(true));
		//
		File directory = instance.getDirectory();
		ArrayList<String> commands = new ArrayList<>();
		if(!AppConfiguration.isLinux() && !AppConfiguration.isWindows() && instance.getSource() instanceof ReleaseSource s && "continuous".equals(s.getTargetName())) {
			try {
				directory = s.getDirectory();
				Path parent = s.getDirectory().toPath();
				Path executable = instance.getExecutable().toPath().toRealPath();
				commands.add(parent.relativize(executable).toUri().toString());
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			commands.add(instance.getDirectory().toURI().relativize(instance.getExecutable().toURI()).toString());
		}
		if(debug) {
			commands.add("--debug");
			log.info(localize("log.instance.play.debug", instance.getName()));
		} else {
			log.info(localize("log.instance.play.normal", instance.getName()));
		}
		ProcessBuilder builder = new ProcessBuilder(commands);
		builder.directory(directory);
		//starting process
		new Thread(() -> {
			try {
				Process process = builder.start();
				new Thread(() -> {
					try {
						process.getInputStream().transferTo(System.out);
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
				}, "Output appender thread for process " + process.pid()).start();
				new Thread(() -> {
					try {
						process.getErrorStream().transferTo(System.err);
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
				}, "Error appender thread for process " + process.pid()).start();
				process.waitFor();
			} catch(IOException | InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				log.info(localize("log.instance.play.end", instance.getName()));
				Platform.runLater(() -> ANY_RUNNING.set(false));
			}
		}).start();
	}
}