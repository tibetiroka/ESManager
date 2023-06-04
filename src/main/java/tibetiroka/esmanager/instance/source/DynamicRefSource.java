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

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.instance.annotation.Editable;
import tibetiroka.esmanager.instance.annotation.EditableSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static tibetiroka.esmanager.config.Launcher.localize;
import static tibetiroka.esmanager.instance.annotation.Validator.PATTERN;
import static tibetiroka.esmanager.instance.annotation.Validator.URI;

/**
 * A special MultiSource for handling {@link SourceType#DYNAMIC_REFS}. The list of git sources is changed depending on what is available on the remote. This source only uses {@link GitSource} internally, and doesn't support adding different sources to the list of sources.
 *
 * @since 0.0.6
 */
@EditableSource
public class DynamicRefSource extends MultiSource {
	private static final Logger log = LoggerFactory.getLogger(DynamicRefSource.class);
	/**
	 * The regex pattern used to select refs
	 *
	 * @since 0.0.6
	 */
	@Editable(PATTERN)
	protected String pattern;
	/**
	 * The URI of the remote repository.
	 *
	 * @since 0.0.6
	 */
	@Editable(URI)
	protected String remoteURI;

	public DynamicRefSource() {
		super();
	}

	/**
	 * Creates a new dynamic ref source with the specified name and pattern.
	 *
	 * @param name      The name of the source
	 * @param remoteURI The URI of the remote repository
	 * @param pattern   The pattern used to select refs
	 * @since 0.0.6
	 */
	public DynamicRefSource(@NotNull String name, @NotNull String remoteURI, @NotNull String pattern) {
		super(name, SourceType.DYNAMIC_REFS);
		this.remoteURI = remoteURI;
		this.pattern = pattern;
	}

	@Override
	public void create() {
		try {
			for(String ref : listMatchingRefs()) {
				sources.add(createSource(ref));
			}
		} catch(GitAPIException e) {
			log.error(localize("log.dynamic.create.list.fail", name, e.getMessage(), remoteURI, pattern));
			throw new RuntimeException(e);
		}
		super.create();
	}

	@Override
	public @NotNull String getPublicName() {
		if(type == SourceType.DYNAMIC_REFS) {
			return localize("instance.source.dynamic.text", name, type.name());
		}
		return super.getPublicName();
	}

	@Override
	public @NotNull String getPublicVersion() {
		if(type == SourceType.DYNAMIC_REFS) {
			return localize("instance.version.dynamic.text", name, type.name(), lastUpdated);
		}
		return super.getPublicName();
	}

	@Override
	public boolean needsUpdate() {
		try {
			Set<String> matchingRefs = listMatchingRefs();
			Set<String> currentRefs = sources.stream().map(s -> (GitSource) s).map(GitSource::getRemoteRefName).map(s -> s.startsWith("refs/pull/") ? s.replaceAll("/head$", "/merge") : s).collect(Collectors.toSet());
			return !(matchingRefs.containsAll(currentRefs) && currentRefs.containsAll(matchingRefs)) || super.needsUpdate();
		} catch(GitAPIException e) {
			log.error(localize("log.dynamic.needsupdate.list.fail", name, e.getMessage(), remoteURI, pattern));
			throw new RuntimeException(e);
		}
	}

	@Override
	public void update() {
		try {
			Set<String> matchingRefs = listMatchingRefs();
			Map<GitSource, String> currentRefs = sources.stream().map(s -> (GitSource) s).map(s -> Map.entry(s.getRemoteRefName(), s)).map(e -> {
				if(e.getKey().startsWith("refs/pull/")) {
					return Map.entry(e.getKey().replaceAll("/head$", "/merge"), e.getValue());
				}
				return e;
			}).collect(Collectors.toMap(Entry::getValue, Entry::getKey));
			if(!matchingRefs.containsAll(currentRefs.values())) {
				//Some branches have to be removed.
				//To remove all commits from those branches that have already been merged into the main branch,
				//We will delete the main branch and add all other commits to it again.
				deleteBranch();
				createBranch();
				ArrayList<Source> deleted = new ArrayList<>();
				for(Source source : sources) {
					if(!matchingRefs.contains(currentRefs.get((GitSource) source))) {
						deleted.add(source);
					}
				}
				deleted.forEach(sources::remove);
				deleted.forEach(Source::deleteBranch);
				deleted.forEach(s -> {
					try {
						if(s.getDirectory().exists()) {
							FileUtils.forceDelete(s.getDirectory());
						}
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
				});
			}
			for(String matchingRef : matchingRefs) {
				if(!currentRefs.containsValue(matchingRef)) {
					sources.add(createSource(matchingRef));
				}
			}
			super.update();
		} catch(GitAPIException e) {
			log.error(localize("log.dynamic.update.fail", name, e.getMessage(), remoteURI, pattern));
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new git source for the specified ref.
	 *
	 * @param ref The ref
	 * @return The source
	 * @since 0.0.6
	 */
	protected @NotNull GitSource createSource(@NotNull String ref) {
		String name = "Dynamic source for " + ref + " of " + remoteURI;
		String target = ref.split("/")[2];
		SourceType type = switch(ref.split("/")[1]) {
			case "heads" -> SourceType.BRANCH;
			case "tags" -> SourceType.RELEASE;
			case "pull" -> SourceType.PULL_REQUEST;
			default -> throw new IllegalArgumentException("Unsupported ref: " + ref);
		};
		GitSource source = new GitSource(name, type, remoteURI, target);
		source.setInstance(getInstance());
		return source;
	}

	/**
	 * List the refs that match the pattern of this source.
	 *
	 * @return The set of refs
	 * @throws GitAPIException if the remote cannot be listed
	 * @since 0.0.6
	 */
	protected @NotNull Set<@NotNull String> listMatchingRefs() throws GitAPIException {
		HashSet<String> refs = new HashSet<>(Git.lsRemoteRepository().setRemote(remoteURI).setHeads(true).setTags(true).callAsMap().keySet());
		refs.addAll(Git.lsRemoteRepository().setRemote(remoteURI).setHeads(false).setTags(false).callAsMap().keySet());
		Predicate<String> p = Pattern.compile(pattern).asMatchPredicate();
		return refs.stream().filter(p).collect(Collectors.toSet());
	}
}