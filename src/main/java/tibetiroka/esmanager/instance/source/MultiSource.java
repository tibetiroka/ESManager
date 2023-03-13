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

import org.apache.logging.log4j.LogManager;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.NoSuchElementException;

import static tibetiroka.esmanager.config.Launcher.localize;
import static tibetiroka.esmanager.instance.GitSettings.SETTINGS;

/**
 * A {@link Source} combining multiple {@link GitSource git sources} into one source. Supported types: {@link SourceType#MULTIPLE_SOURCES MULTIPLE_SOURCES}.
 *
 * @since 0.0.1
 */
public class MultiSource extends Source {
	private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(MultiSource.class);
	/**
	 * The set of git sources used. These sources should not be used in any other instance or source.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private HashSet<@NotNull GitSource> sources = new HashSet<>();

	public MultiSource() {
		super();
	}

	/**
	 * Creates a new git source with the specified name and sources. Each source must be a {@link GitSource git source}.
	 *
	 * @param name    The name of this source
	 * @param sources The sources to use
	 * @see #name
	 * @see #type
	 * @since 0.0.1
	 */
	public MultiSource(@NotNull String name, @NotNull Collection<Source> sources) {
		super(name, SourceType.MULTIPLE_SOURCES);
		for(Source source : sources) {
			this.sources.add((GitSource) source);
		}
	}

	@Override
	public void create() {
		if(initialized) {
			return;
		}
		getInstance().getTracker().beginTask(1. / (sources.size() + 1));
		createBranch();
		getInstance().getTracker().endTask();
		for(GitSource source : sources) {
			getInstance().getTracker().beginTask(1. / (sources.size() + 1));
			getInstance().getTracker().beginTask(0.33);
			if(!source.initialized) {
				source.setInstance(getInstance());
				source.getDirectory().mkdirs();
				source.create();
			}
			getInstance().getTracker().endTask();
			getInstance().getTracker().beginTask(0.33);
			source.update();
			getInstance().getTracker().endTask();
			getInstance().getTracker().beginTask(0.33);
			merge(source);
			getInstance().getTracker().endTask();
			getInstance().getTracker().endTask();
		}
		initialized = true;
	}

	@Override
	public void deleteBranch() {
		super.deleteBranch();
		for(GitSource source : sources) {
			source.deleteBranch();
		}
	}

	@Override
	public @NotNull String getPublicName() {
		return switch(type) {
			case MULTIPLE_SOURCES -> localize("instance.source.multi.text", getName(), type.name());
			default -> throw new UnsupportedOperationException(localize("log.source.type.unsupported", getName(), type.name()));
		};
	}

	@Override
	public @NotNull String getPublicVersion() {
		return switch(type) {
			case MULTIPLE_SOURCES -> localize("instance.version.multi.text", getName(), type.name(), lastUpdated);
			default -> throw new UnsupportedOperationException(localize("log.source.type.unsupported", getName(), type.name()));
		};
	}

	@Override
	public boolean isGit() {
		return true;
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public boolean needsUpdate() {
		for(GitSource source : sources) {
			source.setInstance(getInstance());
			getInstance().getTracker().beginTask(1. / sources.size());
			if(source.needsUpdate()) {
				getInstance().getTracker().endTask();
				return true;
			} else {
				getInstance().getTracker().endTask();
			}
		}
		return false;
	}

	@Override
	public void update() {
		for(GitSource source : sources) {
			getInstance().getTracker().beginTask(1. / sources.size());
			getInstance().getTracker().beginTask(0.33);
			if(source.needsUpdate()) {
				getInstance().getTracker().endTask();
				getInstance().getTracker().beginTask(0.33);
				source.update();
				getInstance().getTracker().endTask();
				getInstance().getTracker().beginTask(0.33);
				merge(source);
				getInstance().getTracker().endTask();
			} else {
				getInstance().getTracker().endTask();
			}
			getInstance().getTracker().endTask();
		}
	}

	/**
	 * Gets the set of git sources used. These sources should not be used in any other instance or source. Any changes made to the returned set will reflect on this source.
	 *
	 * @since 0.0.1
	 */
	public @NotNull HashSet<@NotNull GitSource> getSources() {
		return sources;
	}

	/**
	 * Merges the branch of the specified source into the branch of this source.
	 *
	 * @param source The source to merge
	 * @since 0.0.1
	 */
	private void merge(@NotNull GitSource source) {
		log.debug(localize("log.source.update.multi.merge", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName()));
		try {
			Ref other = GIT.branchList().call().stream().filter(ref -> ref.getName().equals("refs/heads/" + source.getBranchName())).findAny().get();
			getInstance().getTracker().beginTask(0.5);
			GIT.checkout().setName(getBranchName()).setCreateBranch(false).call();
			getInstance().getTracker().endTask();
			getInstance().getTracker().beginTask(0.5);
			MergeResult result = GIT.merge().setStrategy(SETTINGS.mergeStrategyProperty().get()).setContentMergeStrategy(SETTINGS.contentMergeStrategyProperty().get()).setCommit(true).include(other).call();
			switch(result.getMergeStatus()) {
				case CHECKOUT_CONFLICT ->
						log.error(localize("log.source.update.multi.merge.fail.conflict.checkout", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName()));
				case CONFLICTING -> log.error(localize("log.source.update.multi.merge.fail.conflict.merge", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName()));
				case ALREADY_UP_TO_DATE, FAST_FORWARD, FAST_FORWARD_SQUASHED, MERGED, MERGED_NOT_COMMITTED, MERGED_SQUASHED, MERGED_SQUASHED_NOT_COMMITTED -> {
					RevCommit commit = GIT.log().setMaxCount(1).call().iterator().next();
					log.debug(localize("log.source.update.multi.merge.done", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName(), result.getMergedCommits().length, commit.getName(), commit.getShortMessage(), commit.getFullMessage(), commit.getAuthorIdent().getName()));
					lastUpdated = Date.from(Instant.now());
				}
				default ->
						log.error(localize("log.source.update.multi.merge.fail.merge.unknown", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName(), result.getMergeStatus().name()));
			}
			getInstance().getTracker().endTask();
		} catch(GitAPIException | NoSuchElementException e) {
			log.error(localize("log.source.update.multi.merge.fail", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName(), e.getMessage()));
			throw new RuntimeException(e);
		}
	}
}