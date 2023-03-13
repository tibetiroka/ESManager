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

import com.owlike.genson.annotation.JsonConverter;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.config.GensonFactory.BuildSystemPropertyConverter;
import tibetiroka.esmanager.instance.source.Source;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * Helper class that can build an instance of Endless Sky from source. This class is effectively a singleton for configuration purposes, and a utility class for most other uses.
 *
 * @since 0.0.1
 */
public class BuildHelper {
	private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(BuildHelper.class);
	/**
	 * The {@link BuildHelper} instance.
	 *
	 * @since 0.0.1
	 */
	private static BuildHelper BUILDER;
	/**
	 * Stores the preferred/active build system. This value is stored in the configuration files.
	 *
	 * @since 0.0.1
	 */
	@JsonConverter(BuildSystemPropertyConverter.class)
	private @NotNull SimpleObjectProperty<@NotNull BuildSystem> buildSystem = new SimpleObjectProperty<>(BuildSystem.valueOf(AppConfiguration.DEFAULT_CONFIGURATION.get("build.system.preferred").toString().toUpperCase()));

	public BuildHelper() {
		BUILDER = this;
	}

	/**
	 * Gets the active {@link BuildHelper} instance.
	 *
	 * @return {@link #BUILDER}
	 * @since 0.0.1
	 */
	public static @NotNull BuildHelper getBuilder() {
		return BUILDER;
	}

	/**
	 * Returns a function that, when given a source, will determine the location of the built executable.
	 *
	 * @return The executable file; not the same location as {@link Source#getExecutable()}.
	 * @since 0.0.1
	 */
	private static @NotNull Function<@NotNull Source, @NotNull File> getStandardExecutable() {
		return repo -> {
			if(AppConfiguration.isLinux()) {
				return Path.of(Source.getRepository().toString(), "build", "linux", "Release", "endless-sky").toFile();
			} else if(AppConfiguration.isWindows()) {
				return Path.of(Source.getRepository().toString(), "build", "windows", "Release", "endless-sky.exe").toFile();
			} else if(System.getProperty("os.arch").contains("arm") || System.getProperty("os.arch").contains("aarch")) {
				return Path.of(Source.getRepository().toString(), "build", "macos-arm", "Release", "endless-sky.dmg").toFile();
			} else {
				return Path.of(Source.getRepository().toString(), "build", "macos", "Release", "endless-sky.dmg").toFile();
			}
		};
	}

	/**
	 * Creates a {@link ProcessBuilder} that runs the specified command in the directory, inheriting the application's io streams.
	 *
	 * @param repo    The directory to execute the command within
	 * @param command The command to execute
	 * @return The {@link ProcessBuilder}
	 * @see ProcessBuilder#inheritIO()
	 * @since 0.0.1
	 */
	private static @NotNull ProcessBuilder runPiped(@NotNull File repo, @NotNull String @NotNull ... command) {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.inheritIO();
		builder.directory(repo);
		return builder;
	}

	/**
	 * Gets the preferred/active build system.
	 *
	 * @return {@link #buildSystem}
	 * @since 0.0.1
	 */
	public @NotNull SimpleObjectProperty<@NotNull BuildSystem> buildSystemProperty() {
		return buildSystem;
	}

	/**
	 * The supported build systems. They can all build the game from the local git repository.
	 *
	 * @since 0.0.1
	 */
	public enum BuildSystem {
		/**
		 * The CMake integration
		 *
		 * @since 0.0.1
		 */
		CMAKE(source -> {
			String preset = AppConfiguration.isWindows() ? "windows" : AppConfiguration.isLinux() ? "linux" : (System.getProperty("os.arch").contains("arm") || System.getProperty("os.arch").contains("aarch")) ? "macos-arm" : "macos";
			log.debug(localize("log.source.build.cmake.preset", preset));
			try {
				source.getInstance().getTracker().beginTask(0.5);
				File repo = Source.getRepository();
				//vcpkg setup
				ProcessBuilder setup = runPiped(repo, "cmake", "--preset", preset);
				log.info(localize("log.source.build.cmake.setup", String.join(" ", setup.command())));
				Process setupProcess = setup.start();
				int result = setupProcess.waitFor();
				if(result != 0) {
					log.error(localize("log.source.build.cmake.setup.fail", result));
					throw new IllegalStateException("Cmake vcpkg setup failed");
				}
				log.info(localize("log.source.build.cmake.setup.done"));
				source.getInstance().getTracker().endTask();
				//
				//compilation
				source.getInstance().getTracker().beginTask(0.5);
				ProcessBuilder cmake = runPiped(repo, "cmake", "--build", "--preset", preset + "-release");
				cmake.environment().put("CMAKE_BUILD_PARALLEL_LEVEL", String.valueOf(Runtime.getRuntime().availableProcessors()));
				log.info(localize("log.source.build.cmake.compile", String.join(" ", cmake.command())));
				Process compileProcess = cmake.start();
				result = compileProcess.waitFor();
				source.getInstance().getTracker().endTask();
				if(result != 0) {
					log.error(localize("log.source.build.cmake.compile.fail", result));
					throw new IllegalStateException("Cmake compilation failed");
				}
				log.info(localize("log.source.build.cmake.compile.done"));
			} catch(IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}, getStandardExecutable()),
		/**
		 * The SCons integration
		 *
		 * @since 0.0.1
		 */
		SCONS(source -> {
			try {
				ProcessBuilder scons = runPiped(Source.getRepository(), "scons", "-j", String.valueOf(Runtime.getRuntime().availableProcessors()));
				log.info(localize("log.source.build.scons", String.join(" ", scons.command())));
				Process p = scons.start();
				int result = p.waitFor();
				if(result != 0) {
					log.error(localize("log.source.build.scons.fail", result));
					throw new IllegalStateException("Scons compilation failed");
				} else {
					log.info(localize("log.source.build.scons.done"));
				}
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}, getStandardExecutable());
		/**
		 * The function used to build a specific source.
		 *
		 * @since 0.0.1
		 */
		private final @NotNull Consumer<@NotNull Source> builder;
		/**
		 * The getter function that provides the built executable for the specified source.
		 *
		 * @since 0.0.1
		 */
		private final @NotNull Function<@NotNull Source, @NotNull File> executable;

		/**
		 * Creates a new build system.
		 *
		 * @param build      The build function
		 * @param executable The getter function for the executable
		 * @see #build
		 * @see #executable
		 * @since 0.0.1
		 */
		BuildSystem(@NotNull Consumer<@NotNull Source> build, @NotNull Function<@NotNull Source, @NotNull File> executable) {
			this.builder = build;
			this.executable = executable;
		}

		/**
		 * Builds the specified source.
		 *
		 * @param source The source to build
		 * @return The built executable
		 * @since 0.0.1
		 */
		public @NotNull File build(@NotNull Source source) {
			builder.accept(source);
			return executable.apply(source);
		}
	}
}