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

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * A {@link Source} that uses a single file as its source. Supported types: {@link SourceType#LOCAL_EXECUTABLE LOCAL_EXECUTABLE}, {@link SourceType#DIRECT_DOWNLOAD DIRECT_DOWNLOAD}.
 *
 * @since 0.0.1
 */
public class FileSource extends Source {
	private static final Logger log = LoggerFactory.getLogger(FileSource.class);
	/**
	 * The SHA-256 hash of the target file
	 *
	 * @since 0.0.1
	 */
	private String hash;
	/**
	 * The {@link String} representation of the {@link URI} of the target file.
	 *
	 * @since 0.0.1
	 */
	private String remoteURI;

	public FileSource() {
		super();
	}

	/**
	 * Creates a new file source with the specified name, type, and target.
	 *
	 * @param name      The name of this source
	 * @param type      The type of this source
	 * @param remoteURI The remote target
	 * @see #name
	 * @see #type
	 * @see #remoteURI
	 * @since 0.0.1
	 */
	public FileSource(@NotNull String name, @NotNull SourceType type, @NotNull String remoteURI) {
		super(name, type);
		this.remoteURI = remoteURI;
	}

	@Override
	public void create() {
		try {
			switch(type) {
				case LOCAL_EXECUTABLE -> {
					if(!getExecutable().exists()) {
						getInstance().getTracker().beginTask(1);
						symlinkExecutable(new File(URI.create(remoteURI)));
						getInstance().getTracker().endTask();
					}
				}
				case DIRECT_DOWNLOAD -> {
					String name = FilenameUtils.getName(URI.create(remoteURI).toURL().getPath());
					if(name.isBlank()) {
						name = "endless-sky";
					}
					File downloaded = new File(getDirectory(), name);
					log.debug(localize("log.source.update.download.direct", remoteURI, downloaded.getName()));
					getInstance().getTracker().beginTask(0.9);
					tibetiroka.esmanager.utils.FileUtils.copyTracked(URI.create(remoteURI).toURL(), downloaded, getInstance().getTracker());
					getInstance().getTracker().endTask();
					getInstance().getTracker().beginTask(0.1);
					symlinkExecutable(downloaded);
					getInstance().getTracker().endTask();
					log.debug(localize("log.source.update.download.direct.done", remoteURI, downloaded.getName()));
				}
			}
			hash = hash(getExecutable());
			lastUpdated = Date.from(Instant.now());
			initialized = true;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public @NotNull String getPublicName() {
		return switch(type) {
			case LOCAL_EXECUTABLE -> localize("instance.source.file.local.text", getName(), type.name(), remoteURI, hash);
			case DIRECT_DOWNLOAD -> localize("instance.source.file.remote.text", getName(), type.name(), remoteURI, hash);
			default -> throw new UnsupportedOperationException(localize("log.source.type.unsupported", getName(), type.name()));
		};
	}

	@Override
	public @NotNull String getPublicVersion() {
		return switch(type) {
			case LOCAL_EXECUTABLE -> localize("instance.version.file.local.text", getName(), type.name(), remoteURI, hash, lastUpdated, hash == null ? null : hash.substring(0, 7));
			case DIRECT_DOWNLOAD -> localize("instance.version.file.remote.text", getName(), type.name(), remoteURI, hash, lastUpdated, hash == null ? null : hash.substring(0, 7));
			default -> throw new UnsupportedOperationException(localize("log.source.type.unsupported", getName(), type.name()));
		};
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
			case LOCAL_EXECUTABLE, DIRECT_DOWNLOAD -> !Objects.equals(hash, hash(getExecutable()));
			default -> throw new UnsupportedOperationException(localize("log.source.update.type.unsupported", getName(), type.name()));
		};
	}

	@Override
	public void update() {
		create();
	}

	/**
	 * Gets the stored SHA-256 hash of the file.
	 *
	 * @return {@link #hash}
	 * @since 0.0.1
	 */
	public @Nullable String getHash() {
		return hash;
	}

	/**
	 * Gets the {@link String} representation of the {@link URI} of the remote repository.
	 *
	 * @return {@link #remoteURI}
	 * @since 0.0.1
	 */
	public @NotNull String getRemoteURI() {
		return remoteURI;
	}

	/**
	 * Sets the remote repository using the {@link String} representation of its {@link URI}.
	 *
	 * @since 0.0.1
	 */
	public void setRemoteURI(@NotNull String remoteURI) {
		this.remoteURI = remoteURI;
	}
}