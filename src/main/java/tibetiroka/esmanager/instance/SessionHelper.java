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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import tibetiroka.esmanager.Main;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.instance.source.ReleaseSource;
import tibetiroka.esmanager.plugin.PluginManager;
import tibetiroka.esmanager.utils.LogUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;

import static tibetiroka.esmanager.config.Launcher.getLauncher;
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
		PluginManager.getManager().installPluginsFor(instance);
		//
		ArrayList<String> commands = new ArrayList<>();
		if(!AppConfiguration.isLinux() && !AppConfiguration.isWindows() && instance.getSource() instanceof ReleaseSource s && "continuous".equals(s.getTargetName())) {
			try {
				commands.add(instance.getExecutable().toPath().toRealPath().toFile().getAbsolutePath());
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			commands.add(instance.getExecutable().getAbsolutePath());
		}
		if(debug) {
			commands.add("--debug");
			log.info(localize("log.instance.play.debug", instance.getPublicName()));
		} else {
			log.info(localize("log.instance.play.normal", instance.getPublicName()));
		}
		ProcessBuilder builder = new ProcessBuilder(commands);
		log.debug(localize("log.instance.play.command", builder.command().toString()));
		//starting process
		new Thread(() -> {
			Main.configureThread(Thread.currentThread(), false);
			Timeline timer = new Timeline(new KeyFrame(Duration.ZERO, new EventHandler<>() {
				Instant last = Instant.now();

				@Override
				public void handle(ActionEvent event) {
					Instant now = Instant.now();
					instance.getStatistics().advanceActiveTimeCounter(now.toEpochMilli() - last.toEpochMilli());
					last = now;
				}
			}), new KeyFrame(new Duration(100)));
			timer.setCycleCount(Timeline.INDEFINITE);
			try {
				Process process = builder.start();
				Platform.runLater(timer::play);
				if(getLauncher().logGameOutputProperty().get()) {
					LogUtils.logAsync(process.getInputStream(), Level.DEBUG);
					LogUtils.logAsync(process.getErrorStream(), Level.WARN);
				}
				instance.getStatistics().advanceLaunchCounter();
				process.waitFor();
			} catch(IOException | InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				log.info(localize("log.instance.play.end", instance.getPublicName()));
				Platform.runLater(() -> ANY_RUNNING.set(false));
				Platform.runLater(timer::stop);
				PluginManager.MANAGER.installAllPlugins();
			}
		}, "Instance manager thread for " + instance.getInternalName()).start();
	}
}