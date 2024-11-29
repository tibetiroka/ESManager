/*
 * Copyright (c) 2024 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.audio;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A basic ID3v2 metadata reader supporting the artist and title fields and no advanced options.
 *
 * @since 1.3.0
 */
public class ID3Parser {
	/**
	 * The parsed artist name
	 */
	private @Nullable String artist;
	/**
	 * The parsed track title
	 */
	private @Nullable String title;

	/**
	 * Parses ID3v2 metadata from an MP3 input stream. The stream is not closed at the end of operation.
	 * @param input The input data
	 * @throws IOException If the input could not be read
	 */
	public ID3Parser(@NotNull InputStream input) throws IOException {
		// todo: this is unsafe due to unchecked read/skip byte counts.

		InputStream in = new BufferedInputStream(input);
		if(Arrays.equals(in.readNBytes(3), "ID3".getBytes())) {
			int version = in.read();
			if(version < 2)
				return;

			in.read(); //revision number; ignored
			int flags = in.read();
			int size = readSize(in);

			if((flags & 0b01000000) != 0) {
				// extended frame header; ignored
				int extendedSize = readSize(in);
				in.skip(extendedSize);
				size -= extendedSize;
				size -= 4; // size declaration for extended header
			}

			while(size > 0) {
				String frameID = new String(in.readNBytes(4), StandardCharsets.US_ASCII);
				int frameSize = readSize(in);
				in.skip(1); // skip first flag byte; unused
				int frameFlags = in.read();

				if((frameFlags & 0b10000000) != 0) // compression size; unhandled TODO
				{
					in.skip(4);
					size -= 4;
				}
				if((frameFlags & 0b00100000) != 0) // grouping; unused
				{
					in.skip(4);
					size -= 4;
				}

				size -= 10; // header
				size -= frameSize; // contents;

				String frameData = new String(in.readNBytes(frameSize), StandardCharsets.ISO_8859_1);

				switch(frameID) {
					case "TPE1" -> artist = frameData;
					case "TIT2" -> title = frameData;
					// ignoring all other tags
				}
			}
		}
	}

	public @Nullable String getArtist() {
		return artist;
	}

	public @Nullable String getTitle() {
		return title;
	}

	/**
	 * Reads a 4-byte size field from the input stream.
	 * @param in The stream containing the data
	 * @return The parsed size
	 * @throws IOException If the input could not be read
	 */
	private int readSize(@NotNull InputStream in) throws IOException {
		int size = 0;
		for(int i = 0; i < 4; i++) {
			size <<= 7;
			size += in.read();
		}
		return size;
	}
}
