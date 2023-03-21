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
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tibetiroka.esmanager.utils.UpdateProgressTracker;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Representation of an entry in the <a href="https://raw.githubusercontent.com/endless-sky/endless-sky-plugins/master/generated/plugins.json">Endless Sky Plugin Index</a>. Remote plugins are also created for local plugins that are no longer present in the remote - in this case, the instance will have otherwise non-null fields (such as {@link #autoupdate} with null values. Only the {@link #name} and {@link #version} fields are guaranteed to be non-null.
 * <p>For more information about the plugin index's specification, see the <a href="https://github.com/endless-sky/rfcs/blob/main/rfcs/0001-plugin-index.md">RFC</a>.</p>
 *
 * @since 0.0.1
 */
public class RemotePlugin {
	/**
	 * The progress indicator used for update tracking.
	 *
	 * @since 0.0.1
	 */
	@JsonIgnore
	private final transient @NotNull UpdateProgressTracker progressTracker = new UpdateProgressTracker();
	/**
	 * Stores whether this plugin is being updated.
	 *
	 * @since 0.0.1
	 */
	@JsonIgnore
	private final transient @NotNull SimpleBooleanProperty downloadInProgress = new SimpleBooleanProperty();
	/**
	 * The name(s) of the author(s) of this plugin. There is no enforced format on how multiple names are represented.
	 *
	 * @since 0.0.1
	 */
	@Nullable
	private String authors;
	/**
	 * Information regarding the auto-update capabilities of this plugin.
	 *
	 * @since 0.0.1
	 */
	@Nullable
	private PluginAutoUpdate autoupdate;
	/**
	 * The description of this plugin.
	 *
	 * @since 0.0.1
	 */
	@Nullable
	private String description;
	/**
	 * The home page of the plugin that provides more detailed information.
	 *
	 * @since 0.0.1
	 */
	@Nullable
	private URL homepage;
	/**
	 * The URL of the plugin's icon, if any.
	 *
	 * @since 0.0.1
	 */
	@Nullable
	private URL iconUrl;
	/**
	 * The name of the license the plugin is licensed under.
	 *
	 * @since 0.0.1
	 */
	@Nullable
	private String license;
	/**
	 * The name of the plugin. This is never null after the instance has been loaded.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private String name;
	/**
	 * The short description of this plugin. This is never null after the instance has been loaded.
	 *
	 * @since 0.0.1
	 */
	@Nullable
	private String shortDescription;
	/**
	 * The address the latest version of the zipped plugin can be downloaded from.
	 *
	 * @since 0.0.1
	 */
	@Nullable
	private URL url;
	/**
	 * The version of this plugin. This is the latest version available in the plugin index.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private String version;

	public RemotePlugin() {
	}

	/**
	 * Creates a new, 'fake' instance that has a name and version, but isn't queried from the remote plugin index.
	 *
	 * @param name    The name of the plugin
	 * @param version The version of the plugin
	 */
	public RemotePlugin(@NotNull String name, @NotNull String version) {
		this();
		this.name = name;
		this.version = version;
	}

	/**
	 * Finds the locally installed version of this plugin, if any.
	 *
	 * @return The local install or null if not found
	 * @since 0.0.1
	 */
	public @Nullable LocalPlugin findLocal() {
		return PluginManager.findLocal(name);
	}

	/**
	 * Gets the name(s) of the author(s) of this plugin. There is no enforced format on how multiple names are represented.
	 *
	 * @return {@link #authors}
	 * @since 0.0.1
	 */
	public @Nullable String getAuthors() {
		return authors;
	}

	/**
	 * Gets the information regarding the auto-update capabilities of this plugin.
	 *
	 * @return {@link #autoupdate}
	 * @since 0.0.1
	 */
	public @Nullable PluginAutoUpdate getAutoupdate() {
		return autoupdate;
	}

	/**
	 * Gets the description of this plugin.
	 *
	 * @return {@link #description}
	 * @since 0.0.1
	 */
	public @Nullable String getDescription() {
		return description;
	}

	/**
	 * Gets whether this plugin is being updated.
	 *
	 * @since 0.0.1
	 */
	public @NotNull SimpleBooleanProperty downloadInProgressProperty() {
		return downloadInProgress;
	}

	/**
	 * Gets the home page of the plugin.
	 *
	 * @return {@link #homepage}
	 * @since 0.0.1
	 */
	public @Nullable URL getHomepage() {
		return homepage;
	}

	/**
	 * Gets the URL of the plugin's icon, if any.
	 *
	 * @return {@link #iconUrl}
	 * @since 0.0.1
	 */
	public @Nullable URL getIconUrl() {
		return iconUrl;
	}

	/**
	 * Gets the name of the license the plugin is licensed under.
	 *
	 * @return {@link #license}
	 * @since 0.0.1
	 */
	public @Nullable String getLicense() {
		return license;
	}

	/**
	 * Gets the name of the plugin. This is never null after the instance has been loaded.
	 *
	 * @return {@link #name}
	 * @since 0.0.1
	 */
	public @NotNull String getName() {
		return name;
	}

	/**
	 * Gets the progress tracker used for update tracking.
	 *
	 * @return {@link #progressTracker}
	 * @since 0.0.1
	 */
	public @NotNull UpdateProgressTracker getProgressTracker() {
		return progressTracker;
	}

	/**
	 * Gets the short description of this plugin. This is never null after the instance has been loaded.
	 *
	 * @return {@link #shortDescription}
	 * @since 0.0.1
	 */
	public @Nullable String getShortDescription() {
		return shortDescription;
	}

	/**
	 * Gets the address the latest version of the zipped plugin can be downloaded from.
	 *
	 * @return {@link #url}
	 * @since 0.0.1
	 */
	public @Nullable URL getUrl() {
		return url;
	}

	/**
	 * Gets the version of this plugin. This is the latest version available in the plugin index.
	 *
	 * @return {@link #version}
	 * @since 0.0.1
	 */
	public @NotNull String getVersion() {
		return version;
	}

	/**
	 * Installs this plugin. Fails if the plugin is already installed.
	 *
	 * @return The installed plugin
	 * @since 0.0.1
	 */
	public @NotNull LocalPlugin install() throws IOException {
		if(findLocal() != null) {
			throw new IllegalStateException("Plugin already exists!");
		}
		LocalPlugin local = new LocalPlugin(name);
		download(local);
		return local;
	}

	/**
	 * Checks whether the locally installed version of this plugin is out of date. If this plugin is not installed, returns {@code false}.
	 *
	 * @return True if an update is needed
	 * @since 0.0.1
	 */
	public boolean needsUpdate() {
		LocalPlugin local = findLocal();
		return local != null && !Objects.equals(local.getVersion(), version);
	}

	/**
	 * Updates the locally installed version of this plugin. This method assumes the installed version is out of date.
	 *
	 * @since 0.0.1
	 */
	public void update() throws IOException {
		LocalPlugin local = findLocal();
		if(local == null) {
			throw new IllegalStateException("Plugin doesn't exist!");
		}
		FileUtils.deleteDirectory(local.getInstallLocation());
		local.getInstallLocation().mkdirs();
		download(local);
	}

	/**
	 * Downloads this remote plugin into the installation directory of the specified local plugin. The target directory should be empty.
	 *
	 * @param local The local plugin
	 * @throws IOException If the plugin could not be installed
	 * @since 0.0.1
	 */
	private void download(@NotNull LocalPlugin local) throws IOException {
		downloadInProgress.set(true);
		progressTracker.reset();
		tibetiroka.esmanager.utils.FileUtils.unpackZipTracked(url, local.getInstallLocation(), progressTracker);
		local.symlinkPlugin();
		local.setVersion(version);
		downloadInProgress.set(false);
	}

	/**
	 * Represents the {@link #autoupdate} entry of a plugin in the plugin index.
	 *
	 * @since 0.0.1
	 */
	public static class PluginAutoUpdate {
		/**
		 * The name of the branch the commits are queried from. Only used with {@link UpdateType#commit}.
		 *
		 * @since 0.0.1
		 */
		@Nullable
		private String branch;
		/**
		 * The URL of the remote icon, if any. This might use a template with the version of the plugin.
		 *
		 * @since 0.0.1
		 */
		@Nullable
		private String iconUrl;
		/**
		 * The method plugin updates are detected. Never null after the instance has been loaded.
		 *
		 * @since 0.0.1
		 */
		@NotNull
		private UpdateType type;
		/**
		 * The URL from which the updates can be detected. Falls back to {@link RemotePlugin#homepage}.  This might use a template with the version of the plugin.
		 *
		 * @since 0.0.1
		 */
		@Nullable
		private String update_url;
		/**
		 * The URL from which the latest version of the zipped plugin can be downloaded.  This might use a template with the version of the plugin.
		 *
		 * @since 0.0.1
		 */
		@NotNull
		private String url;

		/**
		 * Gets the name of the branch the commits are queried from. Only used with {@link UpdateType#commit}.
		 *
		 * @return {@link #branch}
		 * @since 0.0.1
		 */
		public @Nullable String getBranch() {
			return branch;
		}

		/**
		 * Gets the URL of the remote icon, if any. This might use a template with the version of the plugin.
		 *
		 * @return {@link #iconUrl}
		 * @since 0.0.1
		 */
		public @Nullable String getIconUrl() {
			return iconUrl;
		}

		/**
		 * Gets the method plugin updates are detected. Never null after the instance has been loaded.
		 *
		 * @return {@link #type}
		 * @since 0.0.1
		 */
		public @NotNull UpdateType getType() {
			return type;
		}

		/**
		 * Gets the URL from which the updates can be detected. Falls back to {@link RemotePlugin#homepage}.  This might use a template with the version of the plugin.
		 *
		 * @return {@link #update_url}
		 * @since 0.0.1
		 */
		public @Nullable String getUpdate_url() {
			return update_url;
		}

		/**
		 * Gets the URL from which the latest version of the zipped plugin can be downloaded.  This might use a template with the version of the plugin.
		 *
		 * @return {@link #url}
		 * @since 0.0.1
		 */
		public @NotNull String getUrl() {
			return url;
		}

		/**
		 * The supported methods for distributing plugin updates.
		 *
		 * @since 0.0.1
		 */
		public enum UpdateType {
			/**
			 * Checks {@link #branch} for new commits.
			 *
			 * @since 0.0.1
			 */
			commit,
			/**
			 * Checks {@link #update_url} for new tags.
			 *
			 * @since 0.0.1
			 */
			tag
		}
	}
}