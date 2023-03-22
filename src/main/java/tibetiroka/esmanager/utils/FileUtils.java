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

import org.apache.commons.io.input.CountingInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
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
	 * Copy the file on the specified {@link URL} to the target file. The current task is used to track download progress.
	 *
	 * @param source  The location of the file to copy
	 * @param target  The file to copy into
	 * @param tracker The tracker used to track copy progress
	 * @since 0.0.1
	 */
	public static void copyTracked(@NotNull URL source, @NotNull File target, @NotNull UpdateProgressTracker tracker) throws IOException {
		tracker.beginTask(0.1);
		tracker.beginTask(0.5);
		if(target.exists()) {
			org.apache.commons.io.FileUtils.forceDelete(target);
		}
		tracker.endTask();
		tracker.beginTask(0.5);
		final long length = getFileSize(source);
		long downloaded = 0;
		tracker.endTask();
		tracker.endTask();
		tracker.beginTask(0.9);
		byte[] buffer = new byte[16777216];//16MB
		try(BufferedInputStream bif = new BufferedInputStream(source.openStream())) {
			try(FileOutputStream fos = new FileOutputStream(target)) {
				int amount;
				while((amount = bif.read(buffer)) >= 0) {
					if(length > 0) {
						tracker.beginTask(amount / (double) length);
					} else {
						tracker.beginTask(calculateFakeProgressChange(downloaded, amount));
						downloaded += amount;
					}
					fos.write(buffer, 0, amount);
					tracker.endTask();
				}
			}
		}
		tracker.endTask();
	}

	/**
	 * Unpacks a zip file into the specified directory.
	 *
	 * @param input   The zip file
	 * @param baseDir The base directory to unpack into
	 * @since 0.0.1
	 */
	public static void unpackZip(@NotNull InputStream input, @NotNull File baseDir) throws IOException {
		try(ZipInputStream zip = new ZipInputStream(input)) {
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
					try(FileOutputStream fos = new FileOutputStream(destFile)) {
						int len;
						while((len = zip.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
					}
				}
				zip.closeEntry();
			}
		}
	}

	/**
	 * Unpacks a zip file into the specified directory.  The current task is used to track download progress.
	 *
	 * @param source  The location of the zip file
	 * @param baseDir The base directory to unpack into
	 * @param tracker The tracker used to track unpacking progress
	 * @since 0.0.1
	 */
	public static void unpackZipTracked(@NotNull URL source, @NotNull File baseDir, @NotNull UpdateProgressTracker tracker) throws IOException {
		tracker.beginTask(0.1);
		final long size = getFileSize(source);
		long downloaded = 0;
		tracker.endTask();
		tracker.beginTask(0.9);
		try(CountingInputStream input = new CountingInputStream(source.openStream())) {
			try(ZipInputStream zip = new ZipInputStream(input)) {
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
						try(FileOutputStream fos = new FileOutputStream(destFile)) {
							int len;
							while((len = zip.read(buffer)) > 0) {
								fos.write(buffer, 0, len);
								tracker.progressTask(calculateFakeProgressChange(downloaded, input.getByteCount() - downloaded));//The number of compressed and decompressed bytes might differ
								downloaded = input.getByteCount();
							}
						}
					}
					zip.closeEntry();
				}
			}
		}
		tracker.endTask();
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

	/**
	 * Calculates how much a download progress has advanced when there is no clear size given to the downloaded content.
	 *
	 * @param previous The number of bytes downloaded before the 'delta' is counted
	 * @param delta    The freshly downloaded number of bytes
	 * @return The increase in the tracked progress
	 */
	private static double calculateFakeProgressChange(long previous, long delta) {
		if(delta == 0) {
			return 0;
		}
		double prevMb = previous / 1048576.;
		double currentMb = (previous + delta) / 1048576.;
		double previousValue = 1. - 1. / Math.sqrt(25. / 256. * prevMb); // 1024 mb -> 90%
		double currentValue = 1. - 1. / Math.sqrt(25. / 256. * currentMb);
		if(currentMb < 16) {
			return 0;
		}
		if(previous == 0) {
			return Math.max(0, currentValue);
		}
		return Math.max(0, currentValue - previousValue);
	}

	/**
	 * Gets the size of the file from the specified location.
	 *
	 * @param url The location of the file
	 * @return The size of the file
	 * @see File#length()
	 * @since 0.0.1
	 */
	private static long getFileSize(@NotNull URL url) throws IOException {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("HEAD");
			conn.setRequestProperty("Accept-Encoding", "None");
			conn.connect();
			if(conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new IOException("HTTP connection returned " + conn.getResponseCode());
			}
			if(conn.getContentLengthLong() > 0) {
				return conn.getContentLengthLong();
			} else if(conn.getHeaderField("content-range") != null) {
				String s = conn.getHeaderField("content-range").split("/")[1];
				if(!s.equals("*")) {
					return Long.parseLong(s);
				}
			}
			return conn.getContentLengthLong();
		} finally {
			if(conn != null) {
				conn.disconnect();
			}
		}
	}
}