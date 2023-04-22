/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.instance.source;

import com.owlike.genson.annotation.JsonIgnore;
import javafx.beans.property.SimpleStringProperty;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.instance.BuildHelper;
import tibetiroka.esmanager.instance.Instance;
import tibetiroka.esmanager.utils.ProgressUtils;
import tibetiroka.esmanager.utils.ProgressUtils.FakeTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Random;

import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * Sources are used to download, update or combine different versions of Endless Sky. Each {@link Instance} has exactly one source. Sources are serialized with their instances and written into the configuration files.
 *
 * @since 0.0.1
 */
public abstract class Source {
	private static final Logger log = LoggerFactory.getLogger(Source.class);
	/**
	 * The global {@link Git} instance. This {@link Git} instance is managing the local clone of Endless Sky.
	 *
	 * @since 0.0.1
	 */
	protected static @Nullable Git GIT;

	static {
		try {
			if(getRepository().isDirectory()) {
				GIT = Git.open(getRepository());
			}
			getRepository().getParentFile().mkdirs();
		} catch(IOException ignored) {
		}
	}

	/**
	 * Stores whether the source is initialized.
	 *
	 * @since 0.0.1
	 */
	protected boolean initialized;
	/**
	 * The time this source was last updated.
	 *
	 * @since 0.0.1
	 */
	protected @Nullable Date lastUpdated;
	/**
	 * The name of this source.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	protected String name;
	/**
	 * The type of this source.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	protected SourceType type;
	/**
	 * The name of the branch of this source in the local repository, if any. Only {@link GitSource git} and {@link MultiSource multi} sources have branches.
	 *
	 * @since 0.0.1
	 */
	@Nullable
	private String branchName;
	/**
	 * The {@link Instance} this {@link Source} is used in.
	 *
	 * @since 0.0.1
	 */
	@JsonIgnore
	private transient @NotNull Instance instance;
	/**
	 * The unique identifier used for this source.
	 *
	 * @since 0.0.1
	 */
	private @NotNull String internalID = new Random().nextLong() + "_" + new Random().nextLong() + "_" + new Random().nextLong();
	/**
	 * The property describing the version of this source. This is not localized text, just the textual representation of the version (such as a release number or a commit hash).
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private SimpleStringProperty version = new SimpleStringProperty("...");

	public Source() {
	}

	/**
	 * Creates a new source with the specified name and type.
	 *
	 * @param name The name of this source
	 * @param type The type of this source
	 * @see #name
	 * @see #type
	 * @since 0.0.1
	 */
	public Source(@NotNull String name, @NotNull SourceType type) {
		this();
		this.name = name;
		this.type = type;
	}

	/**
	 * Gets the local clone of the Endless Sky repository.
	 *
	 * @return The {@link File} the repository is cloned within
	 * @since 0.0.1
	 */
	public static @NotNull File getRepository() {
		return new File(AppConfiguration.DATA_HOME + "/endless-sky/");
	}

	/**
	 * Calculates the SHA-256 hash of the specified file
	 *
	 * @param file The file to calculate the hash for
	 * @return The hash
	 */
	protected static @NotNull String hash(@NotNull File file) {
		try {
			byte[] buffer = new byte[8192];
			int count;
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
			while((count = bis.read(buffer)) > 0) {
				digest.update(buffer, 0, count);
			}
			bis.close();

			byte[] hash = digest.digest();
			return new String(Base64.getEncoder().encode(hash));
		} catch(NoSuchAlgorithmException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Builds the executable of this source, if possible. Has no effect if {@link #canBeBuilt()} is false.
	 *
	 * @since 0.0.1
	 */
	public void build() {
		if(canBeBuilt()) {
			localize("log.source.build", name, branchName, version.getName(), BuildHelper.getBuilder().buildSystemProperty().get().name().toLowerCase());
			try {
				instance.getTracker().beginTask(0.5);
				GIT.checkout().setName(branchName).setCreateBranch(false).call();
				instance.getTracker().endTask();
				File executable = BuildHelper.getBuilder().buildSystemProperty().get().build(this);
				String name = executable.getName();
				instance.getTracker().beginTask(0.25);
				File copy = new File(getDirectory(), name);
				Files.copy(executable.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
				instance.getTracker().endTask();
				instance.getTracker().beginTask(0.25);
				String[] files = new String[]{"data", "images", "resources", "sounds", "icons", "credits.txt"};
				for(String file : files) {
					instance.getTracker().beginTask(1. / files.length);
					File source = new File(getRepository(), file);
					File destination = new File(getDirectory(), file);
					if(destination.exists()) {
						destination.delete();
					}
					if(source.isFile()) {
						FileUtils.copyFile(source, destination);
					} else {
						FileUtils.copyDirectory(source, destination);
					}
					instance.getTracker().endTask();
				}
				symlinkExecutable(copy);
				instance.getTracker().endTask();
			} catch(GitAPIException | IOException e) {
				localize("log.source.build.fail", name, branchName, version.getName(), e.getMessage());
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Checks whether the specified source can be built.
	 *
	 * @return True if build is possible
	 * @since 0.0.1
	 */
	public boolean canBeBuilt() {
		return switch(type) {
			case RELEASE, PULL_REQUEST, BRANCH, LATEST_RELEASE, MULTIPLE_SOURCES -> true;
			default -> false;
		};
	}

	/**
	 * Creates and initializes this source. After a successful execution of this method, {@link #initialized} should be true.
	 *
	 * @since 0.0.1
	 */
	public abstract void create();

	/**
	 * Deletes the branch of this source in the local repository, if any.
	 *
	 * @since 0.0.1
	 */
	public void deleteBranch() {
		if(branchName == null) {
			return;
		}
		try {
			GIT.checkout().setName("master").setCreateBranch(false).call();
			GIT.branchDelete().setForce(true).setBranchNames(branchName).call();
		} catch(GitAPIException e) {
			log.error(localize("log.git.branch.delete.fail", branchName, e.getMessage()));
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the name of the branch of this source in the local repository.
	 *
	 * @return {@link #branchName}
	 * @since 0.0.1
	 */
	public @Nullable String getBranchName() {
		return branchName;
	}

	/**
	 * Gets the directory this source is managed within.
	 *
	 * @return The directory of this source
	 * @since 0.0.1
	 */
	public @NotNull File getDirectory() {
		return new File(AppConfiguration.DATA_HOME + "/instances/" + instance.getName() + "/sources/" + internalID);
	}

	/**
	 * Gets the Endless Sky executable of this source. The file may not exist.
	 *
	 * @return The executable
	 * @since 0.0.1
	 */
	public @NotNull File getExecutable() {
		return new File(getDirectory(), "executable");
	}

	/**
	 * Gets the instance this source is used within.
	 *
	 * @return {@link #initialized}
	 * @since 0.0.1
	 */
	public @NotNull Instance getInstance() {
		return instance;
	}

	/**
	 * Sets the instance this source is used within.
	 *
	 * @see #initialized
	 * @since 0.0.1
	 */
	public void setInstance(@NotNull Instance instance) {
		this.instance = instance;
	}

	/**
	 * Gets the name of this source.
	 *
	 * @return {@link #name}
	 * @since 0.0.1
	 */
	public @NotNull String getName() {
		return name;
	}

	/**
	 * Sets the name of this source.
	 *
	 * @see #name
	 * @since 0.0.1
	 */
	public void setName(@NotNull String name) {
		this.name = name;
	}

	/**
	 * Gets a publicly presentable name for this source. The returned name is already localized.
	 *
	 * @return The public name of this source
	 * @since 0.0.1
	 */
	public abstract @NotNull String getPublicName();

	/**
	 * Gets a publicly presentable version for this source. The returned version text is already localized.
	 *
	 * @return The public version of this source
	 * @since 0.0.1
	 */
	public abstract @NotNull String getPublicVersion();

	/**
	 * Gets the type of this source.
	 *
	 * @return {@link #type}
	 * @since 0.0.1
	 */
	public @NotNull SourceType getType() {
		return type;
	}

	/**
	 * Sets the type of this source.
	 *
	 * @param type The new type
	 * @since 0.0.1
	 */
	public void setType(@NotNull SourceType type) {
		this.type = type;
	}

	/**
	 * Gets the version of this source.
	 *
	 * @return {@link #version}
	 * @since 0.0.1
	 */
	public @NotNull SimpleStringProperty getVersion() {
		return version;
	}

	/**
	 * Gets whether this source uses git. Git sources have a branch in the local fork.
	 *
	 * @return True if git
	 * @see #branchName
	 * @since 0.0.1
	 */
	public abstract boolean isGit();

	/**
	 * Gets whether this source is single, or is composed of multiple sources.
	 *
	 * @return True if single
	 * @see MultiSource
	 * @since 0.0.1
	 */
	public abstract boolean isSingle();

	/**
	 * Checks whether this source requires an update. This call might take several seconds to complete.
	 *
	 * @return True if an update is required
	 * @since 0.0.1
	 */
	public abstract boolean needsUpdate();

	/**
	 * Updates this source. This call might take several seconds to complete.
	 *
	 * @since 0.0.1
	 */
	public abstract void update();

	/**
	 * Clones the main repository. This method has no effect if the repository is already cloned.
	 *
	 * @since 0.0.1
	 */
	protected void cloneMainRepo() {
		if(GIT == null) {
			String repo = (String) AppConfiguration.DEFAULT_CONFIGURATION.get("source.instance.remoteRepository");
			try {
				log.info(localize("log.git.clone", repo, name));
				FakeTask task = ProgressUtils.startFakeTimeTask(getInstance().getTracker());
				task.start(1);
				GIT = Git.cloneRepository().setDirectory(getRepository()).setURI(new URL(repo).toURI().toString()).call();
				task.end();
				log.info(localize("log.git.clone.done", repo, name));
			} catch(URISyntaxException | MalformedURLException | GitAPIException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Creates a new branch for this source in the local repository. The repository is cloned if necessary.
	 *
	 * @since 0.0.1
	 */
	protected void createBranch() {
		instance.getTracker().beginTask(0.3);
		if(GIT == null) {
			cloneMainRepo();
		}
		instance.getTracker().endTask();
		instance.getTracker().beginTask(0.7);
		String branch = new Base32().encodeAsString(name.getBytes(StandardCharsets.UTF_8));
		instance.getTracker().beginTask(0.33);
		while(true) {
			final String b = branch;
			try {
				if(GIT.branchList().call().stream().anyMatch(ref -> ref.getName().split("/")[2].equalsIgnoreCase(b))) {
					branch += "_" + (int) (Math.random() * 1000);
				} else {
					break;
				}
			} catch(GitAPIException e) {
				throw new RuntimeException(e);
			}
		}
		instance.getTracker().endTask();
		branchName = branch;
		log.debug(localize("log.git.branch.create", branchName));
		try {
			instance.getTracker().beginTask(0.33);
			GIT.checkout().setOrphan(true).setName(branchName).setCreateBranch(true).call();
			instance.getTracker().endTask();
			instance.getTracker().beginTask(0.33);
			try {
				GIT.close();
				File[] files = getRepository().listFiles();
				for(File file : files) {
					instance.getTracker().beginTask(1. / files.length);
					if(!file.getName().equals(".git")) {
						FileUtils.forceDelete(file);
					}
					instance.getTracker().endTask();
				}
				instance.getTracker().beginTask(1. / files.length);
				File index = new File(new File(getRepository(), ".git"), "index");
				if(index.exists()) {
					FileUtils.forceDelete(index);
				}
				instance.getTracker().endTask();
				GIT = Git.open(getRepository());
			} catch(IOException e) {
				log.debug(localize("log.git.branch.create.clear.fail", branchName, e.getMessage()));
				throw new RuntimeException(e);
			}
			instance.getTracker().endTask();
		} catch(GitAPIException e) {
			log.debug(localize("log.git.branch.create.fail", branchName, e.getMessage()));
			throw new RuntimeException(e);
		}
		instance.getTracker().endTask();
	}

	/**
	 * Creates a symbolic link ({@link #getExecutable()}) to the specified target. Both files are given executable permissions.
	 *
	 * @param target The target file
	 */
	protected void symlinkExecutable(File target) {
		try {
			if(getExecutable().exists()) {
				if(Files.isSymbolicLink(getExecutable().toPath())) {
					getExecutable().delete();
				} else {
					log.warn(localize("log.source.symlink.regular", name, instance.getName(), getExecutable().getAbsolutePath()));
					return;
				}
			}
			Files.createSymbolicLink(getExecutable().toPath(), target.toPath());
			if(!target.setExecutable(true) || !getExecutable().setExecutable(true)){
				log.warn(localize("log.source.symlink.executable.fail"));
			}
		} catch(IOException e) {
			log.error(localize("log.source.symlink.fail", name, instance.getName(), getExecutable().getAbsolutePath(), target.getAbsolutePath()));
			throw new RuntimeException(e);
		}
	}
}