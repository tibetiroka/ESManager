/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.plugin;

import com.owlike.genson.annotation.JsonIgnore;
import javafx.beans.property.SimpleBooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.Main;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.config.GensonFactory;
import tibetiroka.esmanager.instance.Instance;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Phaser;

import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * Handles plugin-related settings and the list of installed plugins. Acts as a singleton for configuration purposes. This is often used as a switchboard between {@link LocalPlugin} and {@link RemotePlugin}.
 *
 * @since 0.0.1
 */
public class PluginManager {
	/**
	 * Stores whether any plugin update is in progress. Since plugins are always updated in parallel, there is always a central location where all updates are started from. This property should not be set outside of {@link #updatePlugins()}.
	 *
	 * @since 0.0.1
	 */
	private static final @NotNull SimpleBooleanProperty UPDATE_IN_PROGRESS = new SimpleBooleanProperty(false);
	private static final Logger log = LoggerFactory.getLogger(PluginManager.class);
	/**
	 * The active instance. Never null after the configuration is loaded.
	 *
	 * @since 0.0.1
	 */
	public static @NotNull PluginManager MANAGER;
	/**
	 * Stores whether to update plugins on startup.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private SimpleBooleanProperty autoUpdatePlugins = new SimpleBooleanProperty((Boolean) AppConfiguration.DEFAULT_CONFIGURATION.get("plugins.autoupdate"));
	/**
	 * The list of installed plugins.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private ArrayList<@NotNull LocalPlugin> installedPlugins = new ArrayList<>();
	/**
	 * Stores whether to preserve existing installations of plugins in the Endless Sky data directory.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private SimpleBooleanProperty preservePlugins = new SimpleBooleanProperty((Boolean) AppConfiguration.DEFAULT_CONFIGURATION.get("plugins.preserve"));
	/**
	 * The list of remote plugins queried from the plugin index.
	 *
	 * @since 0.0.1
	 */
	@JsonIgnore
	private transient @NotNull ArrayList<@NotNull RemotePlugin> remotePlugins = new ArrayList<>();

	public PluginManager() {
		MANAGER = this;
	}

	/**
	 * Finds a locally installed plugin with the specified name.
	 *
	 * @param name The name of the plugin
	 * @return The plugin instance or null if not found
	 * @since 0.0.1
	 */
	public static @Nullable LocalPlugin findLocal(@NotNull String name) {
		Optional<LocalPlugin> plugin = MANAGER.installedPlugins.stream().filter(p -> Objects.equals(p.getName(), name)).findAny();
		return plugin.orElse(null);
	}

	/**
	 * Finds a plugin in the plugin index by name. This method uses a cached version of the plugin index, which can be updated using {@link #loadRemotePlugins()}.
	 *
	 * @param name The name of the plugin to find
	 * @return The plugin instance or null if not found
	 * @since 0.0.1
	 */
	public static @Nullable RemotePlugin findRemote(@NotNull String name) {
		Optional<RemotePlugin> plugin = MANAGER.remotePlugins.stream().filter(p -> Objects.equals(p.getName(), name)).findAny();
		return plugin.orElse(null);
	}

	/**
	 * Gets the active instance. Never null after the configuration is loaded.
	 *
	 * @since 0.0.1
	 */
	public static @NotNull PluginManager getManager() {
		return MANAGER;
	}

	/**
	 * Gets whether any plugin update is in progress. Since plugins are always updated in parallel, there is always a central location where all updates are started from. This property should not be set outside of {@link #updatePlugins()}.
	 *
	 * @since 0.0.1
	 */
	public static @NotNull SimpleBooleanProperty updateInProgressProperty() {
		return UPDATE_IN_PROGRESS;
	}

	/**
	 * Updates all plugins. While plugins are updated in parallel, this method doesn't exit until all update are finished and all plugin updater threads have finished execution.
	 *
	 * @since 0.0.1
	 */
	public static void updatePlugins() {
		UPDATE_IN_PROGRESS.set(true);
		Phaser phaser = new Phaser(1);
		for(LocalPlugin local : MANAGER.installedPlugins) {
			phaser.register();
			new Thread(() -> {
				Main.configureThread(Thread.currentThread(), false);
				try {
					local.updateIfRequired();
				} finally {
					phaser.arriveAndDeregister();
				}
			}, "Plugin updater thread for " + local.getName()).start();
		}
		phaser.arriveAndAwaitAdvance();
		AppConfiguration.savePluginConfiguration();
		UPDATE_IN_PROGRESS.set(false);
	}

	/**
	 * Stores whether to update plugins on startup.
	 *
	 * @since 0.0.1
	 */
	public @NotNull SimpleBooleanProperty getAutoUpdatePlugins() {
		return autoUpdatePlugins;
	}

	/**
	 * The list of installed plugins.
	 *
	 * @since 0.0.1
	 */
	public @NotNull ArrayList<@NotNull LocalPlugin> getInstalledPlugins() {
		return installedPlugins;
	}

	/**
	 * Stores whether to preserve existing installations of plugins in the Endless Sky data directory.
	 *
	 * @since 0.0.1
	 */
	public @NotNull SimpleBooleanProperty getPreservePlugins() {
		return preservePlugins;
	}

	/**
	 * The list of remote plugins queried from the plugin index.
	 *
	 * @since 0.0.1
	 */
	public @NotNull ArrayList<@NotNull RemotePlugin> getRemotePlugins() {
		return remotePlugins;
	}

	/**
	 * Installs all downloaded plugins. This only changes how the game sees installed plugins, and doesn't affect the internal mechanics of the launcher.
	 */
	public void installAllPlugins() {
		for(LocalPlugin plugin : installedPlugins) {
			managePlugin(plugin, true);
		}
	}

	/**
	 * Installs all enabled plugins for this instance, and uninstalls all disabled ones. This only changes how the game sees installed plugins, and doesn't affect the internal mechanics of the launcher.
	 *
	 * @param instance The instance to configure plugins for
	 * @since 1.1.0
	 */
	public void installPluginsFor(@NotNull Instance instance) {
		for(LocalPlugin plugin : installedPlugins) {
			managePlugin(plugin, plugin.isEnabledFor(instance));
		}
	}

	/**
	 * Loads all remote plugins from the plugin index. Any currently loaded remote plugins are cleared from the remote plugin list.
	 *
	 * @since 0.0.1
	 */
	public void loadRemotePlugins() throws IOException {
		remotePlugins.clear();
		try {
			remotePlugins.addAll(new ArrayList<>(Arrays.asList(GensonFactory.createGenson().deserialize(new URL((String) AppConfiguration.DEFAULT_CONFIGURATION.get("plugins.index.remote")).openStream(), RemotePlugin[].class))));
			remotePlugins.sort(Comparator.comparing(p -> p.getName().toLowerCase()));
		} finally {
			for(LocalPlugin plugin : installedPlugins) {
				if(plugin.findRemote() == null) {
					RemotePlugin remote = new RemotePlugin(plugin.getName(), plugin.getVersion());
					remotePlugins.add(remote);
				}
			}
		}
		for(RemotePlugin remotePlugin : remotePlugins) {
			remotePlugin.updateInstalledStatus();
		}
	}

	/**
	 * Installs/uninstalls the specified plugin to/from the game's plugin directory. Doesn't delete or download plugins. This only changes how the game sees installed plugins, and doesn't affect the internal mechanics of the launcher.
	 *
	 * @param plugin  The plugin to install/uninstall
	 * @param install True to install the plugin, false to uninstall
	 * @since 1.1.1
	 */
	private void managePlugin(@NotNull LocalPlugin plugin, boolean install) {
		if(install) {
			if(!plugin.getSymlink().exists()) {
				plugin.symlinkPlugin();
				log.debug(localize("log.instance.plugin.install"), plugin.getName(), plugin.getVersion());
			}
		} else {
			if(plugin.getSymlink().exists() && Files.isSymbolicLink(plugin.getSymlink().toPath())) {
				plugin.getSymlink().delete();
				log.debug(localize("log.instance.plugin.uninstall"), plugin.getName(), plugin.getVersion());
			}
		}
	}
}