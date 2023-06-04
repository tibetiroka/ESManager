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
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.instance.source.*;
import tibetiroka.esmanager.ui.InstanceController;
import tibetiroka.esmanager.ui.MainController;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * Utility class for creating, displaying and updating instances.
 *
 * @since 0.0.1
 */
public class InstanceUtils {
	/**
	 * The semaphore used to control access to the git repository. Only one task can access it at a time.
	 *
	 * @since 0.0.1
	 */
	private static final @NotNull Semaphore GIT_SEMAPHORE = new Semaphore(1);

	/**
	 * Creates a new instance using the specified builder.
	 *
	 * @param builder The parameterized instance builder
	 * @return The created instance
	 * @since 0.0.1
	 */
	public static @NotNull Instance create(@NotNull InstanceBuilder builder) {
		if(!builder.isValid()) {
			throw new IllegalArgumentException(localize("log.instance.builder.invalid"));
		}
		Instance instance = builder.build();
		instance.getTracker().reset();
		instance.getTracker().setUpdated(true);
		instance.getTracker().setWorking(true);
		//
		try {
			if(instance.getSource().isGit()) {
				GIT_SEMAPHORE.acquire();
			}
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
		try {
			instance.getTracker().beginTask(0.05);
			InstanceUtils.createDisplay(instance);
			instance.getTracker().endTask();
			//
			instance.getTracker().beginTask(0.95);
			instance.create();
			instance.getTracker().endTask();
			//
			instance.getTracker().endAll();
			//
			AppConfiguration.saveInstances();
			//
			return instance;
		} catch(Exception e) {
			instance.getTracker().reset();
			instance.getTracker().setFailedUpdate(true);
			throw e;
		} finally {
			if(instance.getSource().isGit()) {
				GIT_SEMAPHORE.release();
			}
		}
	}

	/**
	 * Adds the specified instance to the GUI.
	 *
	 * @param instance The instance to add
	 * @since 0.0.1
	 */
	public static void createDisplay(@NotNull Instance instance) {
		Platform.runLater(() -> {
			FXMLLoader fxmlLoader = new FXMLLoader(MainController.class.getResource("instance.fxml"));
			try {
				HBox box = fxmlLoader.load();
				((InstanceController) fxmlLoader.getController()).initialize(instance);
				MainController.getController().getInstanceListBox().getChildren().add(box);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public static void remove(@NotNull Instance instance) {
		try {
			if(instance.getSource().isGit()) {
				GIT_SEMAPHORE.acquire();
			}
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
		try {
			instance.remove();
			AppConfiguration.saveInstances();
		} finally {
			if(instance.getSource().isGit()) {
				GIT_SEMAPHORE.release();
			}
		}
	}

	/**
	 * Creates a shortened and sanitized version of the specified string that is safe to use as an instance name.
	 *
	 * @param name The name to sanitize
	 * @return The safe name, possibly empty
	 * @since 0.0.6
	 */
	public static @NotNull String sanitizeInstanceName(@NotNull String name) {
		String sanitized = name.replaceAll("([/\\\\\\n^#$%&]|[^\\x20-\\x7D]|^..?$|\\x00)", "").trim();
		return sanitized.substring(0, Math.min(sanitized.length(), 50));
	}

	/**
	 * Updates the specified instance, if necessary.
	 *
	 * @param instance The instance to update
	 * @since 0.0.1
	 */
	public static void update(@NotNull Instance instance) {
		instance.getTracker().reset();
		instance.getTracker().setUpdated(true);
		instance.getTracker().setWorking(true);
		try {
			if(instance.getSource().isGit()) {
				GIT_SEMAPHORE.acquire();
			}
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
		try {
			instance.update();
			instance.getTracker().endAll();
			//
			AppConfiguration.saveInstances();
		} catch(Exception e) {
			instance.getTracker().reset();
			instance.getTracker().setFailedUpdate(true);
			throw new RuntimeException(e);
		} finally {
			if(instance.getSource().isGit()) {
				GIT_SEMAPHORE.release();
			}
		}
	}

	/**
	 * Builder class used to create parameterized instances.
	 *
	 * @since 0.0.1
	 */
	public static class InstanceBuilder {
		/**
		 * The name of the instance; must be specified
		 *
		 * @since 0.0.1
		 */
		protected @Nullable String name;
		/**
		 * The list of sources used by the instance; multiple sources are condensed into a single {@link MultiSource} during {@link #build()}.
		 *
		 * @since 0.0.1
		 */
		protected @NotNull ArrayList<@NotNull Source> sources = new ArrayList<>();

		public InstanceBuilder() {
		}

		/**
		 * Creates a new instance using the previously specified parameters.
		 *
		 * @return The new instance
		 * @since 0.0.1
		 */
		public @NotNull Instance build() {
			if(!isValid()) {
				throw new IllegalStateException(localize("log.instance.builder.build.invalid"));
			}
			String internalName = sanitizeInstanceName(name).trim();
			while(internalName.isBlank() || internalName.length() < 5 || Instance.getInstances().stream().anyMatch(p -> p.getInternalName().equalsIgnoreCase(this.name))) {
				internalName += "_" + (int) (Math.random() * 1000);
			}
			return new Instance(name, internalName, switch(sources.size()) {
				case 1 -> sources.get(0);
				default -> new MultiSource("Generated MultiSource for " + name, sources);
			});
		}

		/**
		 * Gets the list of sources added to this builder.
		 *
		 * @return The list of sources
		 * @since 0.0.6
		 */
		public @NotNull List<Source> getSources() {
			return sources;
		}

		/**
		 * Checks if this is a composite instance. Composite instances have more than one source (that will be joined into a single {@link MultiSource}).
		 *
		 * @return True if composite
		 * @since 0.0.1
		 */
		public boolean isComposite() {
			return sources.size() > 1;
		}

		/**
		 * Checks whether the builder can produce a valid {@link Instance}. Valid builders must have a unique name and at least one source specified.
		 *
		 * @return True if valid
		 * @since 0.0.1
		 */
		public boolean isValid() {
			if(sources.isEmpty()) {
				return false;
			} else if(sources.size() > 1) {
				for(Source source : sources) {
					if(!source.isGit()) {
						return false;
					}
				}
			}
			if(name == null || name.isBlank()) {
				return false;
			}
			for(Instance instance : Instance.getInstances()) {
				if(name.equalsIgnoreCase(instance.getPublicName())) {
					return false;
				}
			}
			return true;
		}

		/**
		 * Sets the name for the instance. Each instance must have a unique name.
		 *
		 * @param name The name
		 * @return This builder
		 * @since 0.0.1
		 */
		public @NotNull InstanceBuilder name(@NotNull String name) {
			this.name = name.trim();
			return this;
		}

		/**
		 * Adds a {@link FileSource} with {@link SourceType#DIRECT_DOWNLOAD} to the {@link #sources list of sources}.
		 *
		 * @param target The download target
		 * @return This builder
		 * @since 0.0.1
		 */
		public @NotNull InstanceBuilder withDownloadSource(@NotNull URI target) {
			FileSource source = new FileSource(target.getPath(), SourceType.DIRECT_DOWNLOAD, target.toString());
			return withSource(source);
		}

		/**
		 * Adds a {@link DynamicRefSource} to the {@link #sources list of sources}.
		 *
		 * @param repo    The repository to choose refs from
		 * @param pattern The pattern for selecting refs
		 * @return This builder
		 * @since 0.0.6
		 */
		public @NotNull InstanceBuilder withDynamicRefSource(@NotNull URI repo, @NotNull Pattern pattern) {
			DynamicRefSource source = new DynamicRefSource("Pattern " + pattern.pattern() + " over " + repo, repo.toString(), pattern.pattern());
			return withSource(source);
		}

		/**
		 * Adds a {@link FileSource} with {@link SourceType#LOCAL_EXECUTABLE} to the {@link #sources list of sources}.
		 *
		 * @param target The local file
		 * @return This builder
		 * @since 0.0.1
		 */
		public @NotNull InstanceBuilder withLocalFileSource(@NotNull File target) {
			if(!target.isFile()) {
				throw new IllegalArgumentException(localize("log.instance.builder.source.file.invalid", target.getAbsolutePath()));
			}
			FileSource source = new FileSource("File " + target.getName(), SourceType.LOCAL_EXECUTABLE, target.toURI().toString());
			return withSource(source);
		}

		/**
		 * Adds a {@link GitSource} with {@link SourceType#BRANCH} to the {@link #sources list of sources}.
		 *
		 * @param repo   The local git repository
		 * @param branch The branch of the repository to use
		 * @return This builder
		 * @since 0.0.1
		 */
		public @NotNull InstanceBuilder withLocalGitSource(@NotNull File repo, @NotNull String branch) {
			if(!repo.isDirectory()) {
				throw new IllegalArgumentException(localize("log.instance.builder.source.git.local.invalid", repo.getAbsolutePath(), branch));
			}
			GitSource source = new GitSource("Branch " + branch, SourceType.BRANCH, repo.toURI().toString(), branch);
			return withSource(source);
		}

		/**
		 * Adds a {@link ReleaseSource} with the specified type to the {@link #sources list of sources}. The source uses the default remote repository.
		 *
		 * @param type   The type of the source; must be {@link SourceType#LATEST_RELEASE} or {@link SourceType#RELEASE}
		 * @param target The release to target; ignored for {@link SourceType#LATEST_RELEASE}
		 * @return This builder
		 * @since 0.0.1
		 */
		public @NotNull InstanceBuilder withOfficialSource(@NotNull SourceType type, @Nullable String target) {
			ReleaseSource source = new ReleaseSource();
			source.setType(type);
			switch(type) {
				case LATEST_RELEASE -> {
					source.setTargetName(null);
					source.setName("Latest official release");
				}
				case RELEASE -> {
					source.setTargetName(target);
					source.setName(target);
				}
				default -> throw new IllegalArgumentException(localize("log.instance.builder.source.official.invalid", type, target));
			}
			return withSource(source);
		}

		/**
		 * Adds a {@link PullRequestSource} with the specified type to the {@link #sources list of sources}. The type of the source is always {@link SourceType#PULL_REQUEST}.
		 *
		 * @param remoteURI The {@link URI} of the git repository
		 * @param target    The number of the pull request to target
		 * @return This builder
		 * @since 0.0.1
		 */
		public @NotNull InstanceBuilder withPullRequestSource(@NotNull URI remoteURI, @NotNull String target) {
			PullRequestSource source = new PullRequestSource(target, remoteURI.toString(), target);
			return withSource(source);
		}

		/**
		 * Adds a {@link ReleaseSource} with the specified type to the {@link #sources list of sources}.
		 *
		 * @param type      The type of the source; must be {@link SourceType#LATEST_RELEASE} or {@link SourceType#RELEASE}
		 * @param remoteURI The {@link URI} of the git repository
		 * @param target    The release to target; ignored for {@link SourceType#LATEST_RELEASE}
		 * @return This builder
		 * @since 0.0.1
		 */
		public @NotNull InstanceBuilder withReleaseSource(@NotNull SourceType type, @NotNull URI remoteURI, @Nullable String target) {
			ReleaseSource source = new ReleaseSource();
			source.setType(type);
			source.setRemoteURI(remoteURI.toString());
			switch(type) {
				case LATEST_RELEASE -> {
					source.setTargetName(null);
					source.setName("Latest release");
				}
				case RELEASE -> {
					source.setTargetName(target);
					source.setName(target);
				}
				default -> throw new IllegalArgumentException(localize("log.instance.builder.source.release.invalid", type, target, remoteURI.toString()));
			}
			return withSource(source);
		}

		/**
		 * Adds a {@link GitSource} with the specified type to the {@link #sources list of sources}.
		 *
		 * @param repo   The {@link URI} of the remote repository
		 * @param type   The type of the source; must be supported by {@link GitSource}
		 * @param target The release to target; ignored for {@link SourceType#LATEST_RELEASE}
		 * @return This builder
		 * @since 0.0.1
		 */
		public @NotNull InstanceBuilder withRemoteGitSource(@NotNull URI repo, @NotNull SourceType type, @Nullable String target) {
			GitSource source = new GitSource();
			source.setType(type);
			source.setRemoteURI(repo.toString());
			switch(type) {
				case RELEASE, BRANCH, COMMIT, PULL_REQUEST -> {
					source.setName(target + " of " + repo);
					source.setTargetName(target);
				}
				case LATEST_RELEASE -> source.setName("Latest release of " + repo);
				default -> throw new IllegalArgumentException(localize("log.instance.builder.source.git.remote.invalid", type, target, repo.toString()));
			}
			return withSource(source);
		}

		/**
		 * Adds the specified source to the {@link #sources list of sources}.
		 *
		 * @param source The source to add
		 * @return This builder
		 * @since 0.0.1
		 */
		public @NotNull InstanceBuilder withSource(@NotNull Source source) {
			sources.add(source);
			return this;
		}
	}
}