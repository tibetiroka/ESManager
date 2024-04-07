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

import com.owlike.genson.Genson;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.instance.annotation.Editable;
import tibetiroka.esmanager.instance.annotation.EditableSource;
import tibetiroka.esmanager.instance.annotation.NonEditable;
import tibetiroka.esmanager.instance.annotation.Validator;
import tibetiroka.esmanager.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static tibetiroka.esmanager.config.Launcher.localize;
import static tibetiroka.esmanager.instance.annotation.Validator.NOT_BLANK_STRING;

/**
 * A {@link Source} that uses a pull request's workflow artifacts as its source. Only {@link SourceType#PULL_REQUEST} is supported.
 *
 * @since 0.0.1
 */
@EditableSource
public class PullRequestSource extends Source {
	private static final Logger log = LoggerFactory.getLogger(GitSource.class);
	/**
	 * The hash of the commit in the current version of this source.
	 *
	 * @since 0.0.1
	 */
	@NonEditable
	private String lastCommit;
	/**
	 * The {@link String} representation of the {@link URI} of the source Git repository.
	 *
	 * @since 0.0.1
	 */
	@Editable(Validator.URI)
	private String remoteURI;
	/**
	 * The name of the PR, branch, or release targeted. Ignored for {@link SourceType#LATEST_RELEASE}.
	 *
	 * @since 0.0.1
	 */
	@Editable(NOT_BLANK_STRING)
	private String targetName;

	public PullRequestSource() {
		super();
		setType(SourceType.PULL_REQUEST);
	}

	/**
	 * Creates a new pull request source with the specified name, remote and target.
	 *
	 * @param name      The name of this source
	 * @param remoteURI The remote repository
	 * @param target    The target's name
	 * @see #name
	 * @see #type
	 * @see #remoteURI
	 * @see #targetName
	 * @since 0.0.1
	 */
	public PullRequestSource(@NotNull String name, @NotNull String remoteURI, @Nullable String target) {
		super(name, SourceType.PULL_REQUEST);
		this.remoteURI = remoteURI;
		this.targetName = target;
	}

	@Override
	public boolean canBeBuilt() {
		return false;
	}

	@Override
	public void create() {
		URL downloadUrl = null;
		String owner = null, repo = null;
		long artifactId = -1;
		long workflowId = -1;
		try {
			getInstance().getTracker().beginTask(0.5);
			//getting repository info for github api
			{
				String s = remoteURI.substring(remoteURI.indexOf("github.com/") + "github.com/".length());
				String[] parts = s.split("[/.]");
				owner = parts[0];
				repo = parts[1];
			}
			//get the sha of the head
			getInstance().getTracker().beginTask(0.25);
			String hash = Git.lsRemoteRepository().setRemote(remoteURI).setHeads(false).setTags(false).call().stream().filter(r -> r.getName().equals("refs/pull/" + targetName + "/head")).findAny().get().getObjectId().getName();
			getInstance().getTracker().endTask();
			//query workflows for pr
			getInstance().getTracker().beginTask(0.25);
			{
				String query = ((String) AppConfiguration.DEFAULT_CONFIGURATION.get("source.github.workflow.query")).replace("${OWNER}", owner).replace("${REPO}", repo).replace("${HASH}", hash);
				URL url = new URL(query);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setRequestProperty("accept", "application/vnd.github+json");
				connection.connect();
				if(connection.getResponseCode() != 200) {
					throw new IOException(localize("log.github.response.error", connection.getResponseCode(), new String(connection.getInputStream().readAllBytes())));
				}
				HashMap<?, ?> map = new Genson().deserialize(connection.getInputStream(), HashMap.class);
				for(Map<?, ?> run : ((Iterable<Map<?, ?>>) map.get("workflow_runs"))) {
					if(run.get("name").equals("CD")) {
						workflowId = (long) run.get("id");
					}
				}
			}
			getInstance().getTracker().endTask();
			//query workflow artifacts
			getInstance().getTracker().beginTask(0.5);
			String fileName = getFileName();
			{
				String query = ((String) AppConfiguration.DEFAULT_CONFIGURATION.get("source.github.workflow.artifact.list")).replace("${OWNER}", owner).replace("${REPO}", repo).replace("${ID}", String.valueOf(workflowId));
				URL url = new URL(query);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setRequestProperty("accept", "application/vnd.github+json");
				connection.connect();
				if(connection.getResponseCode() != 200) {
					throw new IOException(localize("log.github.response.error", connection.getResponseCode(), new String(connection.getInputStream().readAllBytes())));
				}
				HashMap<?, ?> map = new Genson().deserialize(connection.getInputStream(), HashMap.class);
				for(Map<?, ?> run : ((Iterable<Map<?, ?>>) map.get("artifacts"))) {
					if(run.get("name").equals(fileName)) {
						artifactId = (long) run.get("id");
					}
				}
			}
			getInstance().getTracker().endTask();
			//download workflow artifact
			String query = ((String) AppConfiguration.DEFAULT_CONFIGURATION.get("source.github.workflow.artifact.download"));
			query = query.replace("${OWNER}", owner).replace("${REPO}", repo).replace("${ID}", String.valueOf(artifactId));
			downloadUrl = new URL(query);
			//
			getInstance().getTracker().endTask();
			//
			getInstance().getTracker().beginTask(0.5);
			File downloaded = new File(getDirectory(), fileName);
			{
				getInstance().getTracker().beginTask(0.8);
				FileUtils.unpackZipTracked(downloadUrl, downloaded.getParentFile(), getInstance().getTracker());
				getInstance().getTracker().endTask();
				if(!downloaded.exists()) {
					throw new RuntimeException();
				}
				getInstance().getTracker().beginTask(0.2);
				if(AppConfiguration.isWindows()) {
					FileUtils.unpackZipTracked(downloaded.toURI().toURL(), downloaded.getParentFile(), getInstance().getTracker());
					downloaded = new File(downloaded.getParentFile(), "Endless Sky.exe");
				}
				symlinkExecutable(downloaded);
				getInstance().getTracker().endTask();
			}
			getInstance().getTracker().endTask();
			lastCommit = hash;
		} catch(Exception e) {
			log.error(localize("log.git.create.pr.fail", getName(), e.getMessage(), targetName));
			log.debug("\nOwner: {} | Repository: {} | Workflow ID: {} | Artifact ID: {} | Download URL: {}", owner, repo, workflowId, artifactId, downloadUrl);
			throw new RuntimeException(e);
		}
	}

	@Override
	public @NotNull String getPublicName() {
		return localize("instance.source.git.pr.text", getName(), type.name(), remoteURI, lastCommit, targetName);
	}

	@Override
	public @NotNull String getPublicVersion() {
		return localize("instance.version.git.pr.text", getName(), type.name(), remoteURI, lastCommit, targetName, lastCommit == null ? null : lastCommit.substring(0, 7));
	}

	@Override
	public void setType(@NotNull SourceType type) {
		if(type != SourceType.PULL_REQUEST) {
			throw new IllegalArgumentException("Only pull request is supported in a pull request source!");
		}
		super.setType(type);
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
		try {
			return !Objects.equals(lastCommit, Git.lsRemoteRepository().setRemote(remoteURI).setHeads(false).setTags(false).call().stream().filter(r -> r.getName().equals("refs/pull/" + targetName + "/head")).findAny().get().getObjectId().getName());
		} catch(GitAPIException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void update() {
		create();
	}

	/**
	 * Gets the name of the workflow artifact file.
	 *
	 * @return The name of the file
	 * @since 0.0.1
	 */
	private String getFileName() {
		if(AppConfiguration.isWindows()) {
			return "EndlessSky-win64-continuous.zip";
		} else if(AppConfiguration.isLinux()) {
			return "Endless_Sky-continuous-x86_64.AppImage";
		} else {
			return "EndlessSky-macOS-continuous.zip";
		}
	}
}