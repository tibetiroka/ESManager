/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utilities related to {@link File files}, {@link URI URIs} and {@link ZipInputStream zip files}.
 *
 * @since 0.0.1
 */
public class FileUtils {
	/**
	 * A file system that mounts the executable's jar file. Might be null if the file has not been accessed from this class yet.
	 *
	 * @since 0.0.1
	 */
	private static @Nullable FileSystem JAR_FILE_SYSTEM;

	/**
	 * Unpacks a zip file into the specified directory.
	 *
	 * @param input   The zip file
	 * @param baseDir The base directory
	 * @since 0.0.1
	 */
	public static void unpackZip(@NotNull InputStream input, @NotNull File baseDir) throws IOException {
		ZipInputStream zip = new ZipInputStream(input);
		String baseDirPath = baseDir.getCanonicalPath();
		byte[] buffer = new byte[16777216];//16MB
		//
		for(ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
			File destFile = new File(baseDir, entry.getName());
			String destFilePath = destFile.getCanonicalPath();
			if(!destFilePath.startsWith(baseDirPath + File.separator)) {
				throw new IOException("Entry is outside of the target directory: " + entry.getName());
			}
			//
			if(entry.isDirectory()) {
				if(!destFile.isDirectory() && !destFile.mkdirs()) {
					throw new IOException("Failed to create directory " + destFile.getAbsolutePath());
				}
			} else {
				//make sure the parent exists
				File parent = destFile.getParentFile();
				if(!parent.isDirectory() && !parent.mkdirs()) {
					throw new IOException("Failed to create directory " + parent);
				}
				//create file
				FileOutputStream fos = new FileOutputStream(destFile);
				int len;
				while((len = zip.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
			}
			zip.closeEntry();
		}
		zip.close();
	}

	/**
	 * Walks the file tree of depth 1 in the resource directory.
	 *
	 * @param uri The URI of the directory to walk; must be in the resource directory
	 * @return The list of {@link Path paths} in the directory
	 * @since 0.0.1
	 */
	public static @NotNull List<@NotNull Path> walk(@NotNull URI uri) throws IOException {
		Path myPath;
		if(uri.getScheme().equals("jar")) {
			if(JAR_FILE_SYSTEM == null) {
				JAR_FILE_SYSTEM = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
			}
			myPath = JAR_FILE_SYSTEM.getPath(uri.toString().split("!")[1]);
		} else {
			myPath = Paths.get(uri);
		}
		List<Path> paths = new java.util.ArrayList<>(Files.walk(myPath, 1).toList());
		paths.removeIf(p -> p.toUri().equals(uri));
		return paths;
	}
}