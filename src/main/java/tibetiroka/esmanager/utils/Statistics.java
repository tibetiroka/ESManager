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

import com.owlike.genson.annotation.JsonIgnore;
import javafx.beans.property.SimpleLongProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tibetiroka.esmanager.instance.Instance;
import tibetiroka.esmanager.plugin.LocalPlugin;
import tibetiroka.esmanager.plugin.PluginManager;

import java.util.Date;

import static tibetiroka.esmanager.utils.FxUtils.runOnPlatform;

/**
 * Tracks various statistics and data about the user.
 * <em>
 * <b>This is not 'data collection'. This is used to track playtime and other interesting things displayed in the statistics window. None of this data leaves the users' computers.</b>
 * </em>
 * <p>
 * All instances and plugins have their own instance of this class that tracks their progress individually, and there is also a global object that track everything across instances.
 * </p>
 */
public class Statistics {
	/**
	 * The time this tracker was created at
	 *
	 * @since 1.1.0
	 */
	private @NotNull Date creationTime = new Date();
	/**
	 * The number of times the tracked object was used.
	 *
	 * @since 1.1.0
	 */
	private @NotNull SimpleLongProperty launches = new SimpleLongProperty(0);
	/**
	 * The amount of time the tracked object was active for, measured in milliseconds.
	 *
	 * @since 1.1.0
	 */
	private @NotNull SimpleLongProperty timeActive = new SimpleLongProperty(0);

	/**
	 * Adds the specified number of milliseconds to the active time counter.
	 *
	 * @param amount The change in active time
	 * @since 1.1.0
	 */
	public void advanceActiveTimeCounter(long amount) {
		runOnPlatform(() -> getTimeActive().set(getTimeActive().get() + amount));
	}

	/**
	 * Increments the counter for the number of launches.
	 *
	 * @since 1.1.0
	 */
	public void advanceLaunchCounter() {
		runOnPlatform(() -> getLaunches().set(getLaunches().get() + 1));
	}

	/**
	 * The time this tracker was created at
	 *
	 * @since 1.1.0
	 */
	public @NotNull Date getCreationTime() {
		return creationTime;
	}

	/**
	 * The number of times the tracked object was used.
	 *
	 * @since 1.1.0
	 */
	public @NotNull SimpleLongProperty getLaunches() {
		return launches;
	}

	/**
	 * The amount of time the tracked object was active for, measured in milliseconds.
	 *
	 * @since 1.1.0
	 */
	public @NotNull SimpleLongProperty getTimeActive() {
		return timeActive;
	}

	/**
	 * Global statistics counter, effectively singleton. It tracks the usage data of the launcher itself, and the sum of all instance and plugin usage statistics.
	 *
	 * @since 1.1.0
	 */
	public static class GlobalStatistics extends Statistics {
		/**
		 * The global statistics counter, as a singleton.
		 *
		 * @since 1.1.0
		 */
		private static @NotNull GlobalStatistics STATISTICS;
		/**
		 * The number of (successful) instance creations.
		 *
		 * @since 1.1.0
		 */
		private @NotNull SimpleLongProperty instanceCreations = new SimpleLongProperty(0);
		/**
		 * The sum of all other instance statistics.
		 *
		 * @since 1.1.0
		 */
		private @NotNull Statistics instanceStatistics = new Statistics();
		/**
		 * The number of (successful) plugin downloads.
		 *
		 * @since 1.1.0
		 */
		private @NotNull SimpleLongProperty pluginDownloads = new SimpleLongProperty(0);
		/**
		 * The sum of all other plugin statistics.
		 */
		private @NotNull Statistics pluginStatistics = new Statistics();

		public GlobalStatistics() {
			STATISTICS = this;
		}

		/**
		 * Gets the global statistics tracker.
		 *
		 * @return The global tracker
		 * @since 1.1.0
		 */
		public static @Nullable GlobalStatistics getGlobalStatistics() {
			return STATISTICS;
		}

		/**
		 * Increments the instance creation counter.
		 *
		 * @since 1.1.0
		 */
		public void advanceInstanceCreationCounter() {
			runOnPlatform(() -> instanceCreations.set(instanceCreations.get() + 1));
		}

		/**
		 * Increments the plugin download counter.
		 *
		 * @since 1.1.0
		 */
		public void advancePluginDownloadCounter() {
			runOnPlatform(() -> pluginDownloads.set(pluginDownloads.get() + 1));
		}

		/**
		 * The number of (successful) instance creations.
		 *
		 * @since 1.1.0
		 */
		public @NotNull SimpleLongProperty getInstanceCreations() {
			return instanceCreations;
		}

		/**
		 * The sum of all other instance statistics.
		 *
		 * @since 1.1.0
		 */
		public @NotNull Statistics getInstanceStatistics() {
			return instanceStatistics;
		}

		/**
		 * The number of (successful) plugin downloads.
		 *
		 * @since 1.1.0
		 */
		public @NotNull SimpleLongProperty getPluginDownloads() {
			return pluginDownloads;
		}

		/**
		 * The sum of all other plugin statistics.
		 */
		public @NotNull Statistics getPluginStatistics() {
			return pluginStatistics;
		}
	}

	/**
	 * A statistics tracker for instances.
	 *
	 * @since 1.1.0
	 */
	public static class InstanceStatistics extends Statistics {
		/**
		 * The tracked instance
		 *
		 * @since 1.1.0
		 */
		@JsonIgnore
		private @NotNull Instance instance;

		public InstanceStatistics() {
		}

		public InstanceStatistics(@NotNull Instance instance) {
			this.instance = instance;
		}

		@Override
		public void advanceActiveTimeCounter(long amount) {
			FxUtils.runOnPlatform(() -> {
				super.advanceActiveTimeCounter(amount);
				GlobalStatistics.getGlobalStatistics().getInstanceStatistics().advanceActiveTimeCounter(amount);
				for(LocalPlugin plugin : PluginManager.getManager().getInstalledPlugins()) {
					if(plugin.isEnabledFor(instance)) {
						plugin.getStatistics().advanceActiveTimeCounter(amount);
					}
				}
			});
		}

		@Override
		public void advanceLaunchCounter() {
			runOnPlatform(() -> {
				super.advanceLaunchCounter();
				GlobalStatistics.getGlobalStatistics().getInstanceStatistics().advanceLaunchCounter();
				for(LocalPlugin plugin : PluginManager.getManager().getInstalledPlugins()) {
					if(plugin.isEnabledFor(instance)) {
						plugin.getStatistics().advanceLaunchCounter();
					}
				}
			});
		}

		/**
		 * Sets the instance to track.
		 *
		 * @param instance The new instance
		 * @since 1.1.0
		 */
		public void setInstance(@NotNull Instance instance) {
			this.instance = instance;
		}
	}

	/**
	 * A statistics tracker for plugins.
	 *
	 * @since 1.1.0
	 */
	public static class PluginStatistics extends Statistics {
		@Override
		public void advanceActiveTimeCounter(long amount) {
			runOnPlatform(() -> {
				super.advanceActiveTimeCounter(amount);
				GlobalStatistics.getGlobalStatistics().getPluginStatistics().advanceActiveTimeCounter(amount);
			});
		}

		@Override
		public void advanceLaunchCounter() {
			runOnPlatform(() -> {
				super.advanceLaunchCounter();
				GlobalStatistics.getGlobalStatistics().getPluginStatistics().advanceLaunchCounter();
			});
		}
	}
}