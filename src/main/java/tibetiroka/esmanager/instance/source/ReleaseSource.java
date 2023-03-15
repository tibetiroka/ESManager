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

import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.instance.ReleaseUtils;
import tibetiroka.esmanager.utils.FileUtils;
import tibetiroka.esmanager.utils.VersioningUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * A {@link Source} that uses an official release of Endless Sky as its source. Limited support is available for forks using the same naming scheme for their releases. Supported types: {@link SourceType#LATEST_RELEASE LATEST_RELEASE}, {@link SourceType#RELEASE RELEASE}.
 *
 * @since 0.0.1
 */
public class ReleaseSource extends Source {
	/**
	 * The {@link String} representation of the {@link URI} of the official repository.
	 *
	 * @since 0.0.1
	 */
	private static final String OFFICIAL_REMOTE_URI;
	private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(ReleaseSource.class);

	static {
		try {
			OFFICIAL_REMOTE_URI = new URL((String) AppConfiguration.DEFAULT_CONFIGURATION.get("source.instance.remoteRepository")).toURI().toString();
		} catch(URISyntaxException | MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * The hash of the commit in the current version of this source.
	 *
	 * @since 0.0.1
	 */
	private String commitHash;
	/**
	 * The {@link String} representation of the {@link URI} of the remote repository. Defaults to {@link #OFFICIAL_REMOTE_URI}.
	 *
	 * @since 0.0.1
	 */
	private String remoteURI = OFFICIAL_REMOTE_URI;
	/**
	 * The name of the PR, branch, or release targeted.
	 *
	 * @since 0.0.1
	 */
	private String targetName;

	public ReleaseSource() {
		super();
	}

	/**
	 * Creates a new igt source with the specified name, type, remote and target.
	 *
	 * @param name      The name of this source
	 * @param type      The type of this source
	 * @param remoteURI The remote repository, or null for default
	 * @param target    The target's name
	 * @see #name
	 * @see #type
	 * @see #remoteURI
	 * @see #targetName
	 * @since 0.0.1
	 */
	public ReleaseSource(@NotNull String name, @NotNull SourceType type, @Nullable String remoteURI, @Nullable String target) {
		super(name, type);
		if(remoteURI != null) {
			this.remoteURI = remoteURI;
		}
		this.targetName = target;
	}

	@Override
	public boolean canBeBuilt() {
		return false;
	}

	@Override
	public void create() {
		switch(type) {
			case LATEST_RELEASE -> {
				try {
					getInstance().getTracker().beginTask(0.33);
					Optional<String> branch = Git.lsRemoteRepository().setRemote(remoteURI).setHeads(false).setTags(true).call().stream().map(Ref::getName).filter(r -> r.startsWith("refs/tags/")).min(ReleaseUtils.latestFirst());
					getInstance().getTracker().endTask();
					if(branch.isPresent()) {
						String b = branch.get();
						String release = b.substring("refs/tags/".length());
						//
						getInstance().getTracker().beginTask(0.33);
						File downloaded = new File(getDirectory(), getFileName(release));
						if(AppConfiguration.isWindows()) {
							FileUtils.unpackZip(new URL(getDownloadURL(release)).openStream(), downloaded.getParentFile());
							downloaded = new File(downloaded.getParentFile(), "Endless Sky.exe");
						} else {
							Files.copy(new URL(getDownloadURL(release)).openStream(), downloaded.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
						getInstance().getTracker().endTask();
						symlinkExecutable(downloaded);
						getInstance().getTracker().beginTask(0.33);
						Ref ref = Git.lsRemoteRepository().setRemote(remoteURI).setHeads(false).setTags(true).call().stream().filter(r -> r.getName().equals(b)).findAny().get();
						targetName = ref.getName().substring("refs/tags/".length());
						commitHash = ref.getObjectId().getName();
						getInstance().getTracker().endTask();
						Platform.runLater(() -> getVersion().set(release));
					} else {
						throw new IllegalStateException(localize(isOfficial() ? "log.git.create.official.latest.missing" : "log.git.create.release.latest.missing", getName(), remoteURI, targetName));
					}
				} catch(GitAPIException | IOException e) {
					log.error(localize(isOfficial() ? "log.git.create.official.fail" : "log.git.create.release.fail", getName(), e.getMessage(), targetName));
					throw new RuntimeException(e);
				}
			}
			case RELEASE -> {
				try {
					getInstance().getTracker().beginTask(0.5);
					File downloaded = new File(getDirectory(), getFileName(targetName));
					if(AppConfiguration.isWindows()) {
						FileUtils.unpackZip(new URL(getDownloadURL(targetName)).openStream(), downloaded.getParentFile());
						downloaded = new File(downloaded.getParentFile(), "Endless Sky.exe");
					} else {
						Files.copy(new URL(getDownloadURL(targetName)).openStream(), downloaded.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
					downloaded.setExecutable(true);
					getInstance().getTracker().endTask();
					symlinkExecutable(downloaded);
					//
					getInstance().getTracker().beginTask(0.5);
					Ref ref = Git.lsRemoteRepository().setRemote(remoteURI).setHeads(false).setTags(true).call().stream().filter(r -> r.getName().equals("refs/tags/" + targetName)).findAny().get();
					getInstance().getTracker().endTask();
					commitHash = ref.getObjectId().getName();
					Platform.runLater(() -> getVersion().set(targetName));
				} catch(IOException | GitAPIException e) {
					log.error(localize(isOfficial() ? "log.git.create.official.fail" : "log.git.create.release.fail", getName(), e.getMessage(), targetName));
					throw new RuntimeException(e);
				}
			}
			default -> throw new UnsupportedOperationException(localize("log.source.update.type.unsupported", getName(), type.name()));
		}
		initialized = true;
	}

	@Override
	public @NotNull String getPublicName() {
		if(isOfficial()) {
			return switch(type) {
				case RELEASE -> localize("instance.source.official.release.text", getName(), type.name(), targetName);
				case LATEST_RELEASE -> localize("instance.source.official.latest.text", getName(), type.name(), targetName);
				default -> throw new UnsupportedOperationException(localize("log.source.type.unsupported", getName(), type.name()));
			};
		} else {
			return switch(type) {
				case RELEASE -> localize("instance.source.unofficial.release.text", getName(), type.name(), targetName);
				case LATEST_RELEASE -> localize("instance.source.unofficial.latest.text", getName(), type.name(), targetName);
				default -> throw new UnsupportedOperationException(localize("log.source.type.unsupported", getName(), type.name()));
			};
		}
	}

	@Override
	public @NotNull String getPublicVersion() {
		if(isOfficial()) {
			return switch(type) {
				case RELEASE -> localize("instance.version.official.release.text", getName(), type.name(), targetName);
				case LATEST_RELEASE -> localize("instance.version.official.latest.text", getName(), type.name(), targetName);
				default -> throw new UnsupportedOperationException(localize("log.source.type.unsupported", getName(), type.name()));
			};
		} else {
			return switch(type) {
				case RELEASE -> localize("instance.version.unofficial.release.text", getName(), type.name(), targetName);
				case LATEST_RELEASE -> localize("instance.version.unofficial.latest.text", getName(), type.name(), targetName);
				default -> throw new UnsupportedOperationException(localize("log.source.type.unsupported", getName(), type.name()));
			};
		}
	}

	@Override
	public boolean isGit() {
		return false;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public boolean needsUpdate() {
		return switch(type) {
			case LATEST_RELEASE -> {
				try {
					String remoteURI = new URL((String) AppConfiguration.DEFAULT_CONFIGURATION.get("source.instance.remoteRepository")).toURI().toString();
					getInstance().getTracker().beginTask(0.5);
					Optional<String> branch = Git.lsRemoteRepository().setRemote(remoteURI).setHeads(false).setTags(true).call().stream().map(Ref::getName).filter(r -> r.startsWith("refs/tags/")).min(ReleaseUtils.latestFirst());
					getInstance().getTracker().endTask();
					if(branch.isPresent()) {
						getInstance().getTracker().beginTask(0.5);
						Ref ref = Git.lsRemoteRepository().setRemote(remoteURI).setHeads(false).setTags(true).call().stream().filter(r -> r.getName().equals(branch.get())).findAny().get();
						getInstance().getTracker().endTask();
						yield !Objects.equals(ref.getObjectId().name(), commitHash);
					} else {
						throw new IllegalStateException(localize("log.git.create.official.latest.missing", getName(), remoteURI, targetName));
					}
				} catch(URISyntaxException | MalformedURLException | GitAPIException e) {
					throw new RuntimeException(e);
				}
			}
			case RELEASE -> {
				try {
					String remoteURI = new URL((String) AppConfiguration.DEFAULT_CONFIGURATION.get("source.instance.remoteRepository")).toURI().toString();
					Ref ref = Git.lsRemoteRepository().setRemote(remoteURI).setHeads(false).setTags(true).call().stream().filter(r -> r.getName().equals("refs/tags/" + targetName)).findAny().get();
					yield !Objects.equals(commitHash, ref.getObjectId().name());
				} catch(MalformedURLException | URISyntaxException | GitAPIException e) {
					throw new RuntimeException(e);
				}
			}
			default -> throw new UnsupportedOperationException(localize("log.source.update.type.unsupported", getName(), type.name()));
		};
	}

	@Override
	public void update() {
		create();
	}

	/**
	 * Gets the hash of the last commit in this source.
	 *
	 * @return {@link #commitHash}
	 * @since 0.0.1
	 */
	public @Nullable String getCommitHash() {
		return commitHash;
	}

	/**
	 * Gets the {@link String} representation of the {@link URI} of the source Git repository.
	 *
	 * @return {@link #remoteURI}
	 * @since 0.0.1
	 */
	public @NotNull String getRemoteURI() {
		return remoteURI;
	}

	/**
	 * Sets the {@link URI} of the source Git repository using its {@link String} representation.
	 *
	 * @see #remoteURI
	 * @since 0.0.1
	 */
	public void setRemoteURI(@NotNull String remoteURI) {
		this.remoteURI = remoteURI;
	}

	/**
	 * Gets the name of the PR, branch, or release targeted.
	 *
	 * @return {@link #targetName}
	 * @since 0.0.1
	 */
	public @Nullable String getTargetName() {
		return targetName;
	}

	/**
	 * Sets the name of the PR, branch, or release targeted.
	 *
	 * @see #targetName
	 * @since 0.0.1
	 */
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	/**
	 * Gets whether this release is an official release, or is from a fork.
	 *
	 * @return True if official
	 * @since 0.0.1
	 */
	protected boolean isOfficial() {
		return Objects.equals(remoteURI, OFFICIAL_REMOTE_URI);
	}

	/**
	 * Gets the {@link String} representation of the URL the release artifact can be downloaded from.
	 *
	 * @param release The release to download
	 * @return The download path
	 * @since 0.0.1
	 */
	private @NotNull String getDownloadURL(@NotNull String release) {
		try {
			String path = new URI(remoteURI).toURL().toString();
			if(path.endsWith("/")) {
				path = path.substring(0, path.length() - "/".length());
			}
			if(path.endsWith(".git")) {
				path = path.substring(0, path.length() - ".git".length());
			}
			path += "/releases/download/" + release + "/" + getFileName(release);
			return path;
		} catch(MalformedURLException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the name of the release artifact.
	 *
	 * @param release The release to get the artifact for
	 * @return The name of the file
	 * @since 0.0.1
	 */
	private @NotNull String getFileName(@NotNull String release) {
		if(isOldRelease(release)) {
			String filename = "endless-sky-";
			if(AppConfiguration.isLinux()) {
				filename += VersioningUtils.isSameRelease("0.9.14", release) ? "amd64" : "x86_64";
			} else if(AppConfiguration.isWindows()) {
				filename += "win";
				filename += switch(System.getProperty("os.arch")) {
					case "amd64" -> "64";
					case "x86" -> "32";
					default -> "x86_64";
				};
			} else {
				filename += "macos";
			}
			filename += "-" + release + ".";
			if(AppConfiguration.isLinux()) {
				filename += "AppImage";
			} else if(AppConfiguration.isLinux()) {
				filename += ".zip";
			} else {
				filename += ".dmg";
			}
			return filename;
		} else if(release.equals("continuous")) {
			if(AppConfiguration.isLinux()) {
				return "Endless_Sky-continuous-x86_64.AppImage";
			} else if(AppConfiguration.isWindows()) {
				return "EndlessSky-win64-continuous.zip";
			} else {
				return "EndlessSky-macOS-continuous.zip";
			}
		} else {
			if(AppConfiguration.isLinux()) {
				return "Endless_Sky-" + release + "-x86_64.AppImage";
			} else if(AppConfiguration.isWindows()) {
				return "Endless_Sky-win" + switch(System.getProperty("os.arch")) {
					case "amd64" -> "64";
					case "x86" -> "32";
					default -> "x86_64";
				} + "-" + release + ".zip";
			} else {
				return "Endless_Sky-" + release + ".dmg";
			}
		}
	}

	/**
	 * Gets whether the specified release uses an old naming scheme.
	 *
	 * @param release The release to check
	 * @return True if the naming scheme is old
	 * @since 0.0.1
	 */
	private boolean isOldRelease(@NotNull String release) {
		if(ReleaseUtils.isStandardTag(release)) {
			if(release.equals("continuous")) {
				return false;
			}
			String[] parts = release.split("\\.");
			parts[0] = parts[0].replace("v", "");
			return Integer.parseInt(parts[0]) == 0 && Integer.parseInt(parts[1]) < 10;
		} else {
			return false;
		}
	}
}