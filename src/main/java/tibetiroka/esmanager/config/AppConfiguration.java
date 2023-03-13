/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.config;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import tibetiroka.esmanager.Main;
import tibetiroka.esmanager.audio.AudioPlayer;
import tibetiroka.esmanager.instance.BuildHelper;
import tibetiroka.esmanager.instance.GitSettings;
import tibetiroka.esmanager.instance.Instance;
import tibetiroka.esmanager.instance.InstanceUtils;
import tibetiroka.esmanager.plugin.PluginManager;
import tibetiroka.esmanager.plugin.RemotePlugin;
import tibetiroka.esmanager.ui.MainController;
import tibetiroka.esmanager.ui.PluginController;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Utility class for loading and saving configurations, finding default values, and getting os-specific properties.
 *
 * @since 0.0.1
 */
public class AppConfiguration {
	/**
	 * The directory where configuration files should be stored.
	 * <ul>
	 *     <li>On Windows, this is {@code AppData/Local/ESManager/config}</li>
	 *     <li>On Linux, this is {@code $XDG_CONFIG_HOME}</li>
	 *     <li>On macOS, this is {@code Library/Application Support/ESManager/config}</li>
	 * </ul>
	 *
	 * @since 0.0.1
	 */
	public static final File CONFIG_HOME;
	/**
	 * The directory where data files are stored. These data files should generally be grouped into their own subdirectories.
	 * <ul>
	 *     <li>On Windows, this is {@code AppData/Local/ESManager/data}</li>
	 *     <li>On Linux, this is {@code $XDG_DATA_HOME}</li>
	 *     <li>On macOS, this is {@code Library/Application Support/ESManager/data}</li>
	 * </ul>
	 *
	 * @since 0.0.1
	 */
	public static final File DATA_HOME;
	/**
	 * The default configurations provided in the package.
	 *
	 * @since 0.0.1
	 */
	public static final HashMap<String, Object> DEFAULT_CONFIGURATION = loadConfiguration("defaults.json", HashMap.class);
	/**
	 * Home to the plugins and save files for Endless Sky.
	 * <ul>
	 *     <li>On Windows, this is {@code AppData/Roaming/endless-sky}</li>
	 *     <li>On Linux, this is {@code .local/share/endless-sky}</li>
	 *     <li>On macOS, this is {@code Library/Application Support/endless-sky}</li>
	 * </ul>
	 *
	 * @since 0.0.1
	 */
	public static final File ES_DATA_HOME;
	/**
	 * The directory where log files are stored.
	 * <ul>
	 *     <li>On Windows, this is {@code AppData/Local/ESManager/logs}</li>
	 *     <li>On Linux, this is {@code $XDG_STATE_HOME/log}</li>
	 *     <li>On macOS, this is {@code Library/Application Support/ESManager/logs}</li>
	 * </ul>
	 *
	 * @since 0.0.1
	 */
	public static final File LOG_HOME;
	/**
	 * Stores whether the audio configuration has been loaded.
	 *
	 * @since 0.0.1
	 */
	private static final AtomicBoolean AUDIO_LOADED = new AtomicBoolean(false);
	/**
	 * Stores whether the build configuration has been loaded.
	 *
	 * @since 0.0.1
	 */
	private static final AtomicBoolean BUILD_LOADED = new AtomicBoolean(false);
	/**
	 * Stores whether the git configuration has been loaded.
	 *
	 * @since 0.0.1
	 */
	private static final AtomicBoolean GIT_LOADED = new AtomicBoolean(false);
	/**
	 * Stores whether the instance configurations has been loaded.
	 *
	 * @since 0.0.1
	 */
	private static final AtomicBoolean INSTANCES_LOADED = new AtomicBoolean(false);
	/**
	 * Stores whether the launcher configuration has been loaded.
	 *
	 * @since 0.0.1
	 */
	private static final AtomicBoolean LAUNCHER_LOADED = new AtomicBoolean(false);
	/**
	 * Stores whether the plugin configurations has been loaded.
	 *
	 * @since 0.0.1
	 */
	private static final AtomicBoolean PLUGINS_LOADED = new AtomicBoolean(false);

	static {
		if(isWindows()) {
			DATA_HOME = new File(System.getProperty("user.home") + "/AppData/Local/ESManager/data");
			CONFIG_HOME = new File(System.getProperty("user.home") + "/AppData/Local/ESManager/config");
			LOG_HOME = new File(System.getProperty("user.home") + "/AppData/Local/ESManager/logs");
			ES_DATA_HOME = new File(System.getProperty("user.home") + "/AppData/Roaming/endless-sky");
		} else if(isLinux()) {
			String data = System.getenv("XDG_DATA_HOME");
			if(data == null || data.isBlank()) {
				data = System.getProperty("user.home") + "/.local/share/";
			}
			DATA_HOME = new File(data, "ESManager");
			String config = System.getenv("XDG_CONFIG_HOME");
			if(config == null || config.isBlank()) {
				config = System.getProperty("user.home") + "/.config/";
			}
			CONFIG_HOME = new File(config, "ESManager");
			String state = System.getenv("XDG_STATE_HOME");
			if(state == null || state.isBlank()) {
				state = System.getProperty("user.home") + "/.local/state/";
			}
			LOG_HOME = new File(state + "/ESManager", "logs");
			ES_DATA_HOME = new File(System.getProperty("user.home") + "/.local/share/endless-sky");
		} else {
			DATA_HOME = new File(System.getProperty("user.home") + "/Library/Application Support/ESManager/data");
			CONFIG_HOME = new File(System.getProperty("user.home") + "/Library/Application Support/ESManager/config");
			LOG_HOME = new File(System.getProperty("user.home") + "/Library/Application Support/ESManager/logs");
			ES_DATA_HOME = new File(System.getProperty("user.home") + "/Library/Application Support/endless-sky");
		}
		if(!DATA_HOME.exists()) {
			DATA_HOME.mkdirs();
		}
		if(!CONFIG_HOME.exists()) {
			CONFIG_HOME.mkdirs();
		}
		if(!LOG_HOME.exists()) {
			LOG_HOME.mkdirs();
		}
		if(!ES_DATA_HOME.exists()) {
			ES_DATA_HOME.mkdirs();
		}
	}

	/**
	 * Finds all instances of Endless Sky installed via this manager.
	 *
	 * @since 0.0.1
	 */
	public static void discoverInstances() {
		File instanceFile = new File(AppConfiguration.CONFIG_HOME, "instances.json");
		if(instanceFile.exists()) {
			Instance[] instances = AppConfiguration.loadConfiguration(instanceFile, Instance[].class);
			Arrays.sort(instances, Comparator.comparing(a -> a.getName().toLowerCase()));

			for(Instance instance : instances) {
				InstanceUtils.createDisplay(instance);
				Instance.getInstances().add(instance);
				instance.getSource().setInstance(instance);
			}
		}
		INSTANCES_LOADED.set(true);
	}

	/**
	 * Discovers all remote plugins from the plugin index.
	 *
	 * @since 0.0.1
	 */
	public static void discoverPlugins() throws IOException {
		try {
			PluginManager.getManager().loadRemotePlugins();
		} finally {
			Platform.runLater(() -> {
				for(RemotePlugin remotePlugin : PluginManager.getManager().getRemotePlugins()) {
					try {
						FXMLLoader loader = new FXMLLoader(PluginController.class.getResource("plugin.fxml"));
						Parent p = loader.load();
						((PluginController) loader.getController()).initialize(remotePlugin);
						MainController.getController().getPluginListBox().getChildren().add(p);
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
	}

	/**
	 * Checks whether the operating system the application is running on uses a derivative of the Linux kernel.
	 *
	 * @return True if linux
	 * @since 0.0.1
	 */
	public static boolean isLinux() {
		return System.getProperty("os.name").toLowerCase().contains("linux");
	}

	/**
	 * Checks whether the operating system the application is running on is a version of Windows.
	 *
	 * @return True if windows
	 * @since 0.0.1
	 */
	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("windows");
	}

	/**
	 * Loads the audio player's configuration.
	 *
	 * @since 0.0.1
	 */
	public static void loadAudioPlayer() {
		loadConfigFile("audio.json", AudioPlayer.class, () -> AudioPlayer.getPlayer() != null, AudioPlayer::new);
		AUDIO_LOADED.set(true);
	}

	/**
	 * Loads the build helper's configuration.
	 *
	 * @since 0.0.1
	 */
	public static void loadBuildConfiguration() {
		loadConfigFile("build.json", BuildHelper.class, () -> BuildHelper.getBuilder() != null, BuildHelper::new);
		BUILD_LOADED.set(true);
	}

	/**
	 * Loads data from the specified classpath resource.
	 *
	 * @param resource The file to load from
	 * @param type     The class of the configuration object
	 * @param <T>      The type of the object
	 * @return The loaded configuration
	 * @since 0.0.1
	 */
	public static <T> T loadConfiguration(String resource, Class<T> type) {
		InputStream in = AppConfiguration.class.getResourceAsStream(resource);
		if(in == null) {
			throw new IllegalArgumentException("Classpath resource '" + resource + "' not found");
		}
		return loadConfiguration(in, type);
	}

	/**
	 * Loads data from the specified input stream.
	 *
	 * @param in   The stream to read from
	 * @param type The class of the configuration object
	 * @param <T>  The type of the object
	 * @return The loaded configuration
	 * @since 0.0.1
	 */
	public static <T> T loadConfiguration(InputStream in, Class<T> type) {
		return GensonFactory.createGenson().deserialize(in, type);
	}

	/**
	 * Loads data from the specified file.
	 *
	 * @param file The file to load from
	 * @param type The class of the configuration object
	 * @param <T>  The type of the object
	 * @return The loaded configuration
	 * @since 0.0.1
	 */
	public static <T> T loadConfiguration(File file, Class<T> type) {
		try {
			return loadConfiguration(new FileInputStream(file), type);
		} catch(FileNotFoundException e) {
			throw new IllegalArgumentException("Configuration file " + file.getPath() + " not found");
		}
	}

	/**
	 * Loads the configuration used with git.
	 *
	 * @since 0.0.1
	 */
	public static void loadGitConfiguration() {
		loadConfigFile("git.json", GitSettings.class, () -> GitSettings.getSettings() != null, GitSettings::new);
		GIT_LOADED.set(true);
	}

	/**
	 * Loads the launcher's configuration.
	 *
	 * @since 0.0.1
	 */
	public static void loadLauncherConfiguration() {
		loadConfigFile("launcher.json", Launcher.class, () -> Launcher.getLauncher() != null, Launcher::new);
		LAUNCHER_LOADED.set(true);
	}

	/**
	 * Loads the configuration of the installed plugins.
	 *
	 * @since 0.0.1
	 */
	public static void loadPluginConfiguration() {
		loadConfigFile("plugins.json", PluginManager.class, () -> PluginManager.getManager() != null, PluginManager::new);
		PLUGINS_LOADED.set(true);
	}

	/**
	 * Saves all loaded configurations.
	 *
	 * @since 0.0.1
	 */
	public static void saveAll() {
		if(Main.ERROR.get()) {
			return;
		}
		saveInstances();
		savePluginConfiguration();
		saveAudioPlayer();
		saveBuildConfiguration();
		saveGitConfiguration();
		saveLauncherConfiguration();
	}

	/**
	 * Saves the audio player's configuration, if loaded.
	 *
	 * @since 0.0.1
	 */
	public static void saveAudioPlayer() {
		if(Main.ERROR.get() || !AUDIO_LOADED.get()) {
			return;
		}
		if(AudioPlayer.getPlayer() != null) {
			saveConfigFile("audio.json", AudioPlayer.getPlayer());
		}
	}

	/**
	 * Saves the build helper's configuration, if loaded.
	 *
	 * @since 0.0.1
	 */
	public static void saveBuildConfiguration() {
		if(Main.ERROR.get() || !BUILD_LOADED.get()) {
			return;
		}
		if(BuildHelper.getBuilder() != null) {
			saveConfigFile("build.json", BuildHelper.getBuilder());
		}
	}

	/**
	 * Saves the configuration used with git, if loaded.
	 *
	 * @since 0.0.1
	 */
	public static void saveGitConfiguration() {
		if(Main.ERROR.get() || !GIT_LOADED.get()) {
			return;
		}
		if(GitSettings.getSettings() != null) {
			saveConfigFile("git.json", GitSettings.getSettings());
		}
	}

	/**
	 * Saves the configuration of the created instances, if loaded.
	 *
	 * @since 0.0.1
	 */
	public static void saveInstances() {
		if(Main.ERROR.get() || !INSTANCES_LOADED.get()) {
			return;
		}
		saveConfigFile("instances.json", Instance.getInstances().toArray(new Instance[0]));
	}

	/**
	 * SAves the launcher's configuration, if loaded.
	 *
	 * @since 0.0.1
	 */
	public static void saveLauncherConfiguration() {
		if(Main.ERROR.get() || !LAUNCHER_LOADED.get()) {
			return;
		}
		if(Launcher.getLauncher() != null) {
			saveConfigFile("launcher.json", Launcher.getLauncher());
		}
	}

	/**
	 * Saves the configuration of the installed plugins, if loaded.
	 *
	 * @since 0.0.1
	 */
	public static void savePluginConfiguration() {
		if(Main.ERROR.get() || !PLUGINS_LOADED.get()) {
			return;
		}
		if(PluginManager.getManager() != null) {
			saveConfigFile("plugins.json", PluginManager.getManager());
		}
	}

	/**
	 * Loads the specified configuration file.
	 *
	 * @param file      The name of the file
	 * @param type      The type of object to load
	 * @param checker   Checks if the object has been loaded
	 * @param onFailure Supplies a fallback object if the configuration could not be loaded
	 * @param <T>       The type of the object
	 * @return The loaded object
	 * @since 0.0.1
	 */
	private static <T> T loadConfigFile(String file, Class<T> type, Supplier<Boolean> checker, Supplier<T> onFailure) {
		File config = new File(CONFIG_HOME, file);
		if(config.isFile()) {
			try {
				return GensonFactory.createGenson().deserialize(new FileInputStream(config), type);
			} catch(FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		if(checker.get()) {
			return null;
		}
		return onFailure.get();
	}

	/**
	 * Saves the specified object into the configuration file.
	 *
	 * @param file   The name of the file
	 * @param object The object to save
	 * @since 0.0.1
	 */
	private static void saveConfigFile(String file, Object object) {
		try {
			Files.writeString(
					new File(CONFIG_HOME, file).toPath(),
					GensonFactory.createGenson().serialize(object),
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}