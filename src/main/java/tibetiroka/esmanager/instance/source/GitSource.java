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
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.instance.ReleaseUtils;

import java.net.URI;
import java.time.Instant;
import java.util.Date;

import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * A {@link Source} that uses a Git ref as its source. Supported types: {@link SourceType#BRANCH BRANCH}, {@link SourceType#PULL_REQUEST PULL_REQUEST}, {@link SourceType#COMMIT COMMIT}, {@link SourceType#RELEASE RELEASE} and {@link SourceType#LATEST_RELEASE LATEST_RELEASE}.
 *
 * @since 0.0.1
 */
public class GitSource extends Source {
	private static final Logger log = LoggerFactory.getLogger(GitSource.class);
	/**
	 * The hash of the commit in the current version of this source.
	 *
	 * @since 0.0.1
	 */
	private String lastCommit;
	/**
	 * The {@link String} representation of the {@link URI} of the source Git repository.
	 *
	 * @since 0.0.1
	 */
	private String remoteURI;
	/**
	 * The name of the PR, branch, or release targeted. Ignored for {@link SourceType#LATEST_RELEASE}.
	 *
	 * @since 0.0.1
	 */
	private String targetName;

	public GitSource() {
		super();
	}

	/**
	 * Creates a new git source with the specified name, type, remote and target.
	 *
	 * @param name      The name of this source
	 * @param type      The type of this source
	 * @param remoteURI The remote repository
	 * @param target    The target's name
	 * @see #name
	 * @see #type
	 * @see #remoteURI
	 * @see #targetName
	 * @since 0.0.1
	 */
	public GitSource(@NotNull String name, @NotNull SourceType type, @NotNull String remoteURI, @Nullable String target) {
		super(name, type);
		this.remoteURI = remoteURI;
		this.targetName = target;
	}

	@Override
	public void create() {
		getInstance().getTracker().beginTask(0.5);
		if(!initialized) {
			createBranch();
		}
		getInstance().getTracker().endTask();
		getInstance().getTracker().beginTask(0.5);
		try {
			getInstance().getTracker().beginTask(0.3);
			String remote = getRemoteRefName();
			getInstance().getTracker().endTask();
			getInstance().getTracker().beginTask(0.3);
			FetchResult result = fetch(remote, false);
			getInstance().getTracker().endTask();
			getInstance().getTracker().beginTask(0.2);
			GIT.checkout().setName(getBranchName()).setCreateBranch(false).call();
			getInstance().getTracker().endTask();
			getInstance().getTracker().beginTask(0.2);
			RevCommit latest = GIT.log().setMaxCount(1).call().iterator().next();
			lastCommit = latest.getName();
			lastUpdated = Date.from(Instant.now());
			Platform.runLater(() -> getVersion().set(lastCommit.substring(0, 7)));
			getInstance().getTracker().endTask();
			log.debug(localize("log.git.create.fetch.message", getName(), result.getMessages().trim(), remoteURI, targetName));
			initialized = true;
		} catch(GitAPIException e) {
			log.error(localize("log.git.create.fetch.fail", getName(), e.getMessage(), remoteURI, targetName));
			throw new RuntimeException(e);
		}
		getInstance().getTracker().endTask();
	}

	@Override
	public @NotNull String getPublicName() {
		return switch(type) {
			case BRANCH -> localize("instance.source.git.branch.text", getName(), type.name(), remoteURI, lastCommit, targetName);
			case COMMIT -> localize("instance.source.git.commit.text", getName(), type.name(), remoteURI, lastCommit, targetName, targetName.substring(0, 7));
			case RELEASE -> localize("instance.source.git.release.text", getName(), type.name(), remoteURI, lastCommit, targetName);
			case PULL_REQUEST -> localize("instance.source.git.pr.text", getName(), type.name(), remoteURI, lastCommit, targetName);
			case LATEST_RELEASE -> localize("instance.source.git.latest.text", getName(), type.name(), remoteURI, lastCommit, targetName);
			default -> throw new UnsupportedOperationException(localize("log.source.type.unsupported", getName(), type.name()));
		};
	}

	@Override
	public @NotNull String getPublicVersion() {
		return switch(type) {
			case BRANCH -> localize("instance.version.git.branch.text", getName(), type.name(), remoteURI, lastCommit, targetName, lastCommit == null ? null : lastCommit.substring(0, 7));
			case COMMIT -> localize("instance.version.git.commit.text", getName(), type.name(), remoteURI, lastCommit, targetName, lastCommit == null ? null : lastCommit.substring(0, 7));
			case RELEASE -> localize("instance.version.git.release.text", getName(), type.name(), remoteURI, lastCommit, targetName, lastCommit == null ? null : lastCommit.substring(0, 7));
			case PULL_REQUEST -> localize("instance.version.git.pr.text", getName(), type.name(), remoteURI, lastCommit, targetName, lastCommit == null ? null : lastCommit.substring(0, 7));
			case LATEST_RELEASE -> localize("instance.version.git.latest.text", getName(), type.name(), remoteURI, lastCommit, targetName, lastCommit == null ? null : lastCommit.substring(0, 7));
			default -> throw new UnsupportedOperationException(localize("log.source.type.unsupported", getName(), type.name()));
		};
	}

	@Override
	public boolean isGit() {
		return true;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public boolean needsUpdate() {
		if(!initialized) {
			return true;
		}
		if(type == SourceType.COMMIT) {
			return false;
		}
		try {
			localize("log.source.update.fetch", getName(), remoteURI, lastCommit, targetName);
			FetchResult result = fetch(getRemoteRefName(), true);
			TrackingRefUpdate update = result.getTrackingRefUpdates().iterator().next();
			return update.getOldObjectId().getName().equals(update.getNewObjectId().getName());
		} catch(GitAPIException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void update() {
		create();
	}

	/**
	 * Gets the hash of the last commit in this source.
	 *
	 * @return {@link #lastCommit}
	 * @since 0.0.1
	 */
	public @Nullable String getLastCommit() {
		return lastCommit;
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
	public void setTargetName(@NotNull String targetName) {
		this.targetName = targetName;
	}

	/**
	 * Fetches the target branch/remote into the current branch.
	 *
	 * @param target The target to fetch
	 * @param dryRun Whether this fetch is a dry run
	 * @return The result of the fetch
	 * @since 0.0.1
	 */
	protected @NotNull FetchResult fetch(@NotNull String target, boolean dryRun) throws GitAPIException {
		return GIT.fetch().setRemote(remoteURI).setDryRun(dryRun).setRefSpecs(new RefSpec(target + ":" + getBranchName())).call();
	}

	/**
	 * Gets the name of the remote ref based on the {@link #type source type}.
	 *
	 * @return The remote ref's name
	 * @since 0.0.1
	 */
	protected @NotNull String getRemoteRefName() {
		return switch(type) {
			case BRANCH -> "refs/heads/" + targetName;
			case PULL_REQUEST -> "refs/pull/" + targetName + "/head";
			case RELEASE -> "refs/tags/" + targetName;
			case COMMIT -> targetName;
			case LATEST_RELEASE -> {
				try {
					yield GIT.lsRemote().setRemote(remoteURI).setHeads(false).setTags(true).call().stream().map(Ref::getName).filter(r -> r.startsWith("refs/tags/")).min(ReleaseUtils.latestFirst()).get();
				} catch(GitAPIException e) {
					throw new RuntimeException(e);
				}
			}
			default -> throw new UnsupportedOperationException(localize("log.git.remote.branch.fail", getName(), type, targetName, getBranchName()));
		};
	}
}