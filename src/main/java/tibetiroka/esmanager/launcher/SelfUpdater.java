/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.launcher;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.config.GensonFactory;
import tibetiroka.esmanager.launcher.UpdateConfiguration.Migration;
import tibetiroka.esmanager.utils.VersioningUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * Utility class managing launcher updates.
 *
 * @since 0.0.1
 */
public class SelfUpdater {
	private static final Logger log = LoggerFactory.getLogger(SelfUpdater.class);

	/**
	 * Checks whether updates are supported on the current operating system and executable.
	 *
	 * @return True if supported
	 */
	public static boolean areUpdatesSupported() {
		if(AppConfiguration.isLinux()) {
			return true;
		}
		try {
			if(AppConfiguration.isWindows() && getExecutable().isFile() && getExecutable().getName().equals("ESManager.jar")) {
				return true;
			}
		} catch(URISyntaxException e) {
		}
		return false;
	}

	/**
	 * Checks whether the launcher needs an update.
	 *
	 * @return True if an update is required
	 * @since 0.0.1
	 */
	public static boolean needsUpdate() throws GitAPIException, URISyntaxException {
		return findLatest()
				.filter(s -> !VersioningUtils.isSameRelease(s, (String) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.version")))
				.isPresent();
	}

	/**
	 * Updates the launcher. This method assumes that the launcher {@link #needsUpdate() has an avilabe update} to download.
	 *
	 * @since 0.0.1
	 */
	public static void update() throws GitAPIException, URISyntaxException, IOException {
		Optional<String> opt = findLatest();
		if(opt.isPresent()) {
			String target = opt.get();
			log.warn(localize("log.launcher.update", target, AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.version")));
			//
			UpdateConfiguration[] configs = null;
			{
				try {
					configs = GensonFactory.createGenson().deserialize(new URL((String) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.autoupdate.config.remote")).openStream(), UpdateConfiguration[].class);
				} catch(Exception e) {
					log.warn(localize("log.launcher.update.config.remote.error", e.getMessage()));
				}
			}
			//
			String downloadPath = (String) AppConfiguration.DEFAULT_CONFIGURATION.get("source.launcher.remoteRepositoryDownload");
			downloadPath += target + "/";
			//
			File currentExec = getExecutable();
			File downloadedExec = currentExec;
			//
			if(configs != null) {
				boolean found = false;
				for(UpdateConfiguration config : configs) {
					if(target.equals(config.getVersion())) {
						Migration[] migrations = config.getMigrations();
						if(migrations != null) {
							for(Migration migration : migrations) {
								if(AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.version").equals(migration.getVersion())) {
									if(currentExec.getName().equals(migration.getSource())) {
										if(migration.getOperatingSystem().isCurrentOs()) {
											downloadPath += migration.getTarget();
											downloadedExec = new File(currentExec.getParentFile(), migration.getTarget());
											found = true;
											break;
										}
									}
								}
							}
						}
						break;
					}
				}
				if(!found) {
					downloadPath += currentExec.getName();
				}
			} else {
				downloadPath += currentExec.getName();
			}
			//
			Path temp = Files.createTempFile("launcherDownload", null);
			Files.copy(new URL(downloadPath).openStream(), temp, StandardCopyOption.REPLACE_EXISTING);
			//
			for(int i = 0; i < 5; i++) {
				try {
					log.warn(localize("log.launcher.update.replace.attempt", i + 1, 5));
					Files.move(temp, downloadedExec.toPath(), StandardCopyOption.REPLACE_EXISTING);
					downloadedExec.setExecutable(true);
					if(!currentExec.equals(downloadedExec)) {
						currentExec.delete();
					}
					return;
				} catch(Exception e) {
					log.debug(localize("log.launcher.update.replace.attempt.fail", e.getMessage(), i + 1, 5), e);
					try {
						Thread.sleep(1000);
					} catch(InterruptedException ex) {
						throw new RuntimeException(ex);
					}
				}
			}
			Files.delete(temp);
			throw new IOException(localize("log.launcher.update.replace.fail"));
		}
	}

	/**
	 * Finds the latest available release for this application.
	 *
	 * @return The release number of the latest release, or an empty value if not found
	 * @since 0.0.1
	 */
	private static @NotNull Optional<@NotNull String> findLatest() throws GitAPIException {
		return Git.lsRemoteRepository()
		          .setHeads(false)
		          .setTags(true)
		          .setRemote((String) AppConfiguration.DEFAULT_CONFIGURATION.get("source.launcher.remoteRepository"))
		          .callAsMap()
		          .keySet()
		          .stream()
		          .map(s -> s.substring("refs/tags/".length()))
		          .min(VersioningUtils.semVerComparator());
	}

	/**
	 * Gets the launcher executable that is currently in use.
	 *
	 * @return The running executable
	 * @since 0.0.1
	 */
	private static @NotNull File getExecutable() throws URISyntaxException {
		if(AppConfiguration.isLinux() && System.getenv().containsKey("APPIMAGE")) {
			return new File(System.getenv("APPIMAGE"));
		}
		return new File(SelfUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	}
}