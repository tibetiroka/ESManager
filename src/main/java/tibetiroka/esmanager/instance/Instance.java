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

import com.owlike.genson.annotation.JsonIgnore;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.config.Launcher;
import tibetiroka.esmanager.instance.source.Source;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * Each instance is an independently managed installation of Endless Sky. The list of instances is tracked in the {@link #INSTANCES} list, that is saved to the configuration files.
 *
 * @since 0.0.1
 */
public class Instance {
	/**
	 * The list of managed instances.
	 *
	 * @since 0.0.1
	 */
	private static final @NotNull ArrayList<@NotNull Instance> INSTANCES = new ArrayList<>();
	private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(Instance.class);
	/**
	 * The progress tracker that is used to visually display the progress of a task.
	 *
	 * @since 0.0.1
	 */
	@JsonIgnore
	private transient final @NotNull UpdateProgressTracker tracker = new UpdateProgressTracker();
	/**
	 * The Endless Sky executable. Never null after the instance has been created.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private File executable;
	/**
	 * The last time this instance was updated
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private Date lastUpdated = Date.from(Instant.now());
	/**
	 * The name of this instance. This is displayed in the GUI.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private String name;
	/**
	 * The source of this instance. Each instance has exactly one source.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private Source source;

	public Instance() {
	}

	/**
	 * Creates a new instance with the specified name and source. The source's instance is set to be this instance.
	 *
	 * @param name   The name of the instance
	 * @param source The source of this instance
	 * @see #name
	 * @see #source
	 * @since 0.0.1
	 */
	public Instance(@NotNull String name, @NotNull Source source) {
		this();
		this.name = name;
		this.source = source;
		source.setInstance(this);
	}

	/**
	 * Gets the list of managed instances.
	 *
	 * @since 0.0.1
	 */
	public static @NotNull ArrayList<@NotNull Instance> getInstances() {
		return INSTANCES;
	}

	/**
	 * Creates this instance. This method should be called first after an instance was set up with the necessary data.
	 *
	 * @since 0.0.1
	 */
	public void create() {
		log.info(localize("log.instance.create", name, source.getName(), source.getType()));
		remove();
		tracker.beginTask(0.66);
		tracker.beginTask(0.5);
		source.getDirectory().mkdirs();
		log.info(localize("log.instance.create.source", name, source.getName(), source.getType()));
		source.create();
		tracker.endTask();
		tracker.beginTask(0.5);
		if(source.canBeBuilt()) {
			source.build();
		}
		tracker.endTask();
		executable = source.getExecutable();
		tracker.endTask();
		tracker.beginTask(0.33);
		log.info(localize("log.instance.create.update", name, source.getName(), source.getType()));
		update();
		tracker.endTask();
		INSTANCES.add(this);
	}

	/**
	 * Creates a new, localized {@link StringBinding} for the source's public name.
	 *
	 * @return The localized source name
	 * @see Source#getPublicName()
	 * @since 0.0.1
	 */
	public @NotNull StringBinding createSourceStringBinding() {
		return Bindings.createStringBinding(source::getPublicName, source.getVersion(), Launcher.getLauncher().localeProperty(), tracker.isWorkingProperty(), tracker.updateProgressProperty());
	}

	/**
	 * Creates a new, localized {@link StringBinding} for the source's public version.
	 *
	 * @return The localized source version
	 * @see Source#getPublicVersion()
	 * @since 0.0.1
	 */
	public @NotNull StringBinding createVersionStringBinding() {
		return Bindings.createStringBinding(source::getPublicVersion, source.getVersion(), Launcher.getLauncher().localeProperty(), tracker.isWorkingProperty(), tracker.updateProgressProperty());
	}

	/**
	 * Gets the directory this instance is located within.
	 *
	 * @return The location of this instance
	 * @since 0.0.1
	 */
	public @NotNull File getDirectory() {
		return new File(AppConfiguration.DATA_HOME + "/instances/" + name);
	}

	/**
	 * Gets the Endless Sky executable for this instance. Never null after the instance has been created.
	 *
	 * @since 0.0.1
	 */
	public @NotNull File getExecutable() {
		return executable;
	}

	/**
	 * Gets the last time this instance was updated
	 *
	 * @since 0.0.1
	 */
	public @NotNull Date getLastUpdated() {
		return lastUpdated;
	}

	/**
	 * Gets the name of this instance. This is displayed in the GUI.
	 *
	 * @since 0.0.1
	 */
	public @NotNull String getName() {
		return name;
	}

	/**
	 * Gets the source of this instance. Each instance has exactly one source.
	 *
	 * @since 0.0.1
	 */
	public @NotNull Source getSource() {
		return source;
	}

	/**
	 * Gets the progress tracker that is used to visually display the progress of a task.
	 *
	 * @since 0.0.1
	 */
	public @NotNull UpdateProgressTracker getTracker() {
		return tracker;
	}

	/**
	 * Removes this instance from the list of tracked instances and deletes all files associated with it.
	 *
	 * @see #INSTANCES
	 * @since 0.0.1
	 */
	public void remove() {
		INSTANCES.remove(this);
		//deleting directory (with all the sources)
		if(getDirectory().exists()) {
			log.info(localize("log.instance.delete.files", name, source.getName(), source.getVersion()));
			try(var dirStream = Files.walk(getDirectory().toPath())) {
				dirStream
						.map(Path::toFile)
						.sorted(Comparator.reverseOrder())
						.forEach(File::delete);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		//deleting git branches
		if(source.getBranchName() != null) {
			log.info(localize("log.instance.delete.branch", name, source.getName(), source.getVersion(), source.getBranchName()));
			source.deleteBranch();
		}
	}

	/**
	 * Updates this instance, if necessary.
	 *
	 * @see Source#needsUpdate()
	 * @see Source#update()
	 * @since 0.0.1
	 */
	public void update() {
		try {
			log.info(localize("log.instance.update", name));
			tracker.beginTask(0.1);
			boolean needsUpdate = source.needsUpdate();
			tracker.endTask();
			if(needsUpdate) {
				tracker.beginTask(0.4);
				source.update();
				tracker.endTask();
				if(source.canBeBuilt()) {
					tracker.beginTask(0.5);
					source.build();
					tracker.endTask();
				}
				executable = source.getExecutable();
				lastUpdated = Date.from(Instant.now());
				log.info(localize("log.instance.update.done", name));
			} else {
				log.info(localize("log.instance.update.skip", name));
			}
		} catch(Exception e) {
			log.error(e.getMessage(), e);
			log.error(localize("log.instance.update.fail", name, e.getMessage()));
			throw e;
		}
	}
}