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

import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ThreeWayMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.instance.Instance;
import tibetiroka.esmanager.instance.annotation.Editable;
import tibetiroka.esmanager.instance.annotation.Validator;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.NoSuchElementException;

import static tibetiroka.esmanager.config.Launcher.localize;
import static tibetiroka.esmanager.instance.GitSettings.SETTINGS;

/**
 * A {@link Source} combining multiple {@link Source#isGit() git-based sources} into one source. Supported types: {@link SourceType#MULTIPLE_SOURCES MULTIPLE_SOURCES}.
 *
 * @since 0.0.1
 */
public class MultiSource extends Source {
	private static final Logger log = LoggerFactory.getLogger(MultiSource.class);
	/**
	 * The set of git sources used. These sources should not be used in any other instance or source.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	@Editable(Validator.NON_NULL)
	protected SourceSet sources = new SourceSet();

	public MultiSource() {
		super();
	}

	/**
	 * Creates a new git source with the specified name and sources. Each source must be a {@link Source#isGit() git-based source}.
	 *
	 * @param name    The name of this source
	 * @param sources The sources to use
	 * @see #name
	 * @see #type
	 * @since 0.0.1
	 */
	public MultiSource(@NotNull String name, @NotNull Collection<Source> sources) {
		super(name, SourceType.MULTIPLE_SOURCES);
		this.sources.addAll(sources);
	}

	/**
	 * Constructor for subclasses that handle other source types.
	 *
	 * @param name The name of the source
	 * @param type The type of the source
	 */
	protected MultiSource(@NotNull String name, @NotNull SourceType type) {
		super(name, type);
	}

	@Override
	public void create() {
		if(initialized) {
			return;
		}
		getInstance().getTracker().beginTask(1. / (sources.size() + 1));
		createBranch();
		getInstance().getTracker().endTask();
		for(Source source : sources) {
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
	public void delete() {
		super.delete();
		for(Source source : sources) {
			source.delete();
		}
	}

	@Override
	public void deleteBranch() {
		super.deleteBranch();
	}

	@Override
	public void setInstance(@NotNull Instance instance) {
		super.setInstance(instance);
		for(Source source : sources) {
			source.setInstance(instance);
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
		for(Source source : sources) {
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
		for(Source source : sources) {
			getInstance().getTracker().beginTask(1. / sources.size());
			getInstance().getTracker().beginTask(0.33);
			if(source.needsUpdate()) {
				getInstance().getTracker().endTask();
				getInstance().getTracker().beginTask(0.33);
				source.update();
				getInstance().getTracker().endTask();
			} else {
				getInstance().getTracker().endTask();
			}
			getInstance().getTracker().beginTask(0.33);
			merge(source);
			getInstance().getTracker().endTask();
			getInstance().getTracker().endTask();
		}
	}

	/**
	 * Gets the set of git sources used. These sources should not be used in any other instance or source. Any changes made to the returned set will reflect on this source.
	 *
	 * @since 0.0.1
	 */
	public @NotNull HashSet<@NotNull Source> getSources() {
		return sources;
	}

	/**
	 * Merges the branch of the specified source into the branch of this source.
	 *
	 * @param source The source to merge
	 * @since 0.0.1
	 */
	private void merge(@NotNull Source source) {
		log.debug(localize("log.source.update.multi.merge", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName()));
		try {
			ObjectId other = GIT.getRepository().resolve("refs/heads/" + source.getBranchName());
			getInstance().getTracker().beginTask(0.5);
			GIT.checkout().setName(getBranchName()).setCreateBranch(false).call();
			getInstance().getTracker().endTask();
			getInstance().getTracker().beginTask(0.5);
			ThreeWayMerger merger = MergeStrategy.RECURSIVE.newMerger(GIT.getRepository(), true);
			boolean canMerge = merger.merge(GIT.log().setMaxCount(1).call().iterator().next(), other);
			if(!canMerge) {
				log.warn(localize("log.source.update.multi.merge.pre.conflict", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName()));
				if(SETTINGS.mergeStrategyProperty().get() == MergeStrategy.RECURSIVE) {
					log.error(localize("log.source.update.multi.merge.pre.fail", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName()));
					return;
				} else {
					log.warn(localize("log.source.update.multi.merge.pre.force", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName()));
				}
			}
			MergeResult result = GIT.merge().setStrategy(SETTINGS.mergeStrategyProperty().get()).setContentMergeStrategy(SETTINGS.contentMergeStrategyProperty().get()).setFastForward(FastForwardMode.NO_FF).setCommit(true).include(other).call();
			switch(result.getMergeStatus()) {
				case CHECKOUT_CONFLICT ->
						log.error(localize("log.source.update.multi.merge.fail.conflict.checkout", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName()));
				case CONFLICTING -> {
					log.error(localize("log.source.update.multi.merge.fail.conflict.merge", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName()));
					// clear the merge state
					GIT.getRepository().writeMergeCommitMsg(null);
					GIT.getRepository().writeMergeHeads(null);
					// reset the index and work directory to HEAD
					GIT.reset().setMode(ResetType.HARD).call();
				}
				case ALREADY_UP_TO_DATE, FAST_FORWARD, FAST_FORWARD_SQUASHED, MERGED, MERGED_NOT_COMMITTED, MERGED_SQUASHED, MERGED_SQUASHED_NOT_COMMITTED -> {
					RevCommit commit = GIT.log().setMaxCount(1).call().iterator().next();
					log.debug(localize("log.source.update.multi.merge.done", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName(), result.getMergedCommits().length, commit.getName(), commit.getShortMessage(), commit.getFullMessage(), commit.getAuthorIdent().getName()));
					lastUpdated = Date.from(Instant.now());
				}
				default ->
						log.error(localize("log.source.update.multi.merge.fail.merge.unknown", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName(), result.getMergeStatus().name()));
			}
			getInstance().getTracker().endTask();
		} catch(GitAPIException | NoSuchElementException | IOException e) {
			log.error(localize("log.source.update.multi.merge.fail", getName(), type, getBranchName(), source.getName(), source.type, source.getBranchName(), e.getMessage()));
			throw new RuntimeException(e);
		}
	}

	public static class SourceSet extends HashSet<@NotNull Source> {
	}
}