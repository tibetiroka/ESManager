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

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.instance.Instance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.stream.Collectors;

import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * Represents a locally installed plugin.
 *
 * @since 0.0.1
 */
public class LocalPlugin {
	private static final Logger log = LoggerFactory.getLogger(LocalPlugin.class);
	/**
	 * The name of instances this plugin is enabled for. When null, the plugin is enabled for all instances.
	 *
	 * @since 1.1.0
	 */
	@Nullable
	private HashSet<String> instances = null;
	/**
	 * The name of the plugin. This is the same as the name of this plugin in the plugin index, and is unique to each plugin. Never null after the installation of this plugin is finished.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private String name;
	/**
	 * The version of this plugin. This is expected to be user-readable and should change for every update. This is the same version that is reported by the plugin index for the installed version.  Never null after the installation of this plugin is finished.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private String version;

	public LocalPlugin() {
	}

	/**
	 * Creates a new local plugin with the specified name.
	 *
	 * @param name {@link #name}
	 * @since 0.0.1
	 */
	public LocalPlugin(@NotNull String name) {
		this();
		this.name = name;
	}

	/**
	 * Disables this plugin for the specified instance. No-op if the plugin is already {@link #isEnabledFor(Instance) disabled for this instance}.
	 *
	 * @param instance The instance to disable the plugin for
	 * @since 1.1.0
	 */
	public void disableFor(@NotNull Instance instance) {
		if(instances == null) {
			instances = Instance.getInstances().stream().map(Instance::getInternalName).collect(Collectors.toCollection(HashSet::new));
		}
		instances.remove(instance.getInternalName());
	}

	/**
	 * Enables this plugin for the specified instance. No-op if the plugin is already {@link #isEnabledFor(Instance) enabled for the instance}.
	 *
	 * @param instance The instance to enable the plugin for
	 * @since 1.1.0
	 */
	public void enableFor(@NotNull Instance instance) {
		if(instances != null) {
			instances.add(instance.getInternalName());
		}
	}

	/**
	 * Enables this plugin for all current and future instances.
	 *
	 * @since 1.1.0
	 */
	public void enableForAll() {
		instances = null;
	}

	/**
	 * Attempts to find the remote plugin matching this plugin.
	 *
	 * @return The remote plugin, or null if not found
	 * @since 0.0.1
	 */
	public @Nullable RemotePlugin findRemote() {
		return PluginManager.findRemote(name);
	}

	/**
	 * Gets the directory the plugin is installed within. This directory usually contains a single directory representing the installed version of the plugin.
	 *
	 * @return The installation location
	 * @since 0.0.1
	 */
	public @NotNull File getInstallLocation() {
		return new File(new File(AppConfiguration.DATA_HOME, "plugins"), name);
	}

	/**
	 * Gets the name of this plugin. Never null after the plugin is installed.
	 *
	 * @return {@link #name}
	 * @since 0.0.1
	 */
	public @NotNull String getName() {
		return name;
	}

	/**
	 * Gets the version of the plugin. This is the same as the version of the {@link RemotePlugin} in the plugin index when this plugin was last updated.
	 *
	 * @see #version
	 * @since 0.0.1
	 */
	public @NotNull String getVersion() {
		return version;
	}

	/**
	 * Sets the version of the plugin. This is the same as the version of the {@link RemotePlugin} in the plugin index when this plugin was last updated.
	 *
	 * @param version {@link #version}
	 * @since 0.0.1
	 */
	public void setVersion(@NotNull String version) {
		this.version = version;
	}

	/**
	 * Checks whether the plugin is enabled for the specified instance.
	 *
	 * @param instance The instance
	 * @return True if enabled
	 * @since 1.1.0
	 */
	public boolean isEnabledFor(@NotNull Instance instance) {
		return instances == null || instances.contains(instance.getInternalName());
	}

	/**
	 * Checks whether this plugin is enabled for all current and future instances.
	 *
	 * @return True if enabled for all instances
	 * @since 1.1.0
	 */
	public boolean isEnabledForAll() {
		return instances == null;
	}

	/**
	 * Deletes this installation of the plugin. This method deletes the local files and removes the reference to this plugin from the list of installed plugins.
	 *
	 * @since 0.0.1
	 */
	public void remove() throws IOException {
		File symlink = getSymlink();
		if(Files.isSymbolicLink(symlink.toPath())) {
			Files.delete(symlink.toPath());
		}
		FileUtils.deleteDirectory(getInstallLocation());
		PluginManager.getManager().getInstalledPlugins().remove(this);
		AppConfiguration.savePluginConfiguration();
		RemotePlugin r = findRemote();
		if(r != null) {
			r.updateInstalledStatus();
		}
		AppConfiguration.savePluginConfiguration();
	}

	/**
	 * Creates a symbolic link to this plugin in Endless Sky's data directory, allowing the game to load this plugin.
	 *
	 * @since 0.0.1
	 */
	public void symlinkPlugin() {
		File symlinkDir = getSymlink().getParentFile();
		if(!symlinkDir.exists()) {
			symlinkDir.mkdirs();
		}
		try {
			if(getSymlink().exists()) {
				if(Files.isSymbolicLink(getSymlink().toPath()) || !PluginManager.getManager().getPreservePlugins().get()) {
					FileUtils.forceDelete(getSymlink());
				} else {
					log.warn(localize("log.plugin.symlink.regular", name));
					return;
				}
			}
			Files.createSymbolicLink(getSymlink().toPath(), getInstallLocation().listFiles()[0].toPath());
		} catch(IOException e) {
			log.error(localize("log.plugin.symlink.fail", e.getMessage(), name), e);
		}
	}

	/**
	 * Updates this plugin, if necessary. This method uses {@link RemotePlugin#needsUpdate()} to check for updates.
	 *
	 * @since 0.0.1
	 */
	public void updateIfRequired() {
		log.info(localize("log.plugin.update", name));
		RemotePlugin remote = findRemote();
		if(remote == null) {
			log.warn(localize("log.plugin.update.remote.missing", name));
		} else {
			try {
				if(remote.needsUpdate()) {
					remote.update();
					log.info(localize("log.plugin.update.done", name));
				} else {
					log.info(localize("log.plugin.update.skip", name));
				}
			} catch(Exception e) {
				log.error(localize("log.plugin.update.fail", e.getMessage(), name), e);
			}
		}
	}

	/**
	 * Gets where the symbolic link to this plugin should be located in Endless Sky's data directory.
	 *
	 * @return The location of the symbolic link; might not exist
	 * @since 0.0.1
	 */
	protected @NotNull File getSymlink() {
		return new File(new File(AppConfiguration.ES_DATA_HOME, "plugins"), name);
	}
}