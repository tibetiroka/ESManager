/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.audio;

import com.owlike.genson.Genson;
import com.owlike.genson.annotation.JsonIgnore;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.MapChangeListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.instance.SessionHelper;
import tibetiroka.esmanager.utils.FileUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;
import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * The global audio player used in the launcher. This class acts as a singleton, although more than one instance may be created using {@link Genson genson}. The latest instance is the actual audio player, and the only one that should be started.
 *
 * @since 0.0.1
 */
public class AudioPlayer {
	/**
	 * The localized text describing the currently playing audio file. The field itself is never null, but may contain null values. A null value indicates that there is no file being played.
	 *
	 * @since 0.0.1
	 */
	public static final SimpleStringProperty CURRENT_TITLE_TEXT = new SimpleStringProperty(null);
	/**
	 * Stores whether there is an existing audio player. This effectively means whether audio can be played. The value is {@code false} if:
	 * <ul>
	 *     <li>{@link #PLAYER} is null, or</li>
	 *     <li>the {@link #PLAYER} was not started using {@link #autoPlay()}, or</li>
	 *     <li>if there is no audio to play.</li>
	 * </ul>
	 * Please note that this field never changes value from {@code true} to {@code false}, even if the player encountered an error.
	 *
	 * @since 0.0.1
	 */
	public static final SimpleBooleanProperty PLAYER_EXISTS = new SimpleBooleanProperty(false);
	/**
	 * Stores whether the current audio player is playing a song. False if there is no player, or it is paused.
	 *
	 * @since 0.0.1
	 */
	public static final SimpleBooleanProperty PLAYING = new SimpleBooleanProperty(false);
	/**
	 * The active {@link AudioPlayer} instance. After the configuration for the audio player is loaded, this field is effectively final.
	 *
	 * @since 0.0.1
	 */
	private static AudioPlayer PLAYER;
	private static Logger log = LoggerFactory.getLogger(AudioPlayer.class);
	/**
	 * Stores whether the player should pause when an instance is launched. This value is stored in the configuration files.
	 *
	 * @since 0.0.1
	 */
	public SimpleBooleanProperty autoPause = new SimpleBooleanProperty((Boolean) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.audio.autoPause"));
	/**
	 * Stores whether the player should play automatically when the application is launched. This value is stored in the configuration files.
	 *
	 * @since 0.0.1
	 */
	private SimpleBooleanProperty autoPlay = new SimpleBooleanProperty((Boolean) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.audio.autoPlay"));
	/**
	 * Stores whether the player should play built-in music. This value is stored in the configuration files.
	 *
	 * @since 0.0.1
	 */
	private SimpleBooleanProperty builtin = new SimpleBooleanProperty((Boolean) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.audio.builtin"));
	/**
	 * The active media player. The media player changes for every audio file.
	 *
	 * @since 0.0.1
	 */
	@JsonIgnore
	private transient MediaPlayer currentPlayer;
	/**
	 * Stores whether the player should play user-defined music. This value is stored in the configuration files.
	 *
	 * @since 0.0.1
	 */
	private SimpleBooleanProperty custom = new SimpleBooleanProperty((Boolean) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.audio.custom"));
	/**
	 * The list of audio files that were detected by the player.
	 *
	 * @see #builtin
	 * @see #custom
	 * @since 0.0.1
	 */
	@JsonIgnore
	private transient ArrayList<URI> musicPaths;
	/**
	 * Stores whether the active player is muted. This is independent of the {@link #volume}. This value is stored in the configuration files.
	 *
	 * @see #currentPlayer
	 * @see MediaPlayer#muteProperty()
	 * @since 0.0.1
	 */
	private SimpleBooleanProperty muted = new SimpleBooleanProperty((Boolean) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.audio.mute"));
	/**
	 * Stores the volume of the active player as a value between 0 and 1. This value is stored in the configuration files.
	 *
	 * @see #currentPlayer
	 * @see MediaPlayer#volumeProperty()
	 * @since 0.0.1
	 */
	private SimpleDoubleProperty volume = new SimpleDoubleProperty((Double) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.audio.volume"));

	/**
	 * Creates a new {@link AudioPlayer}, and stores it in {@link #PLAYER}. If there is an existing player, its {@link #currentPlayer media player} is properly disposed.
	 *
	 * @since 0.0.1
	 */
	public AudioPlayer() {
		if(PLAYER != null) {
			if(PLAYER.currentPlayer != null) {
				PLAYER.currentPlayer.dispose();
				PLAYER.currentPlayer = null;
			}
		}
		PLAYER = this;
	}

	/**
	 * Signals that the player can play audio, if necessary. Calling this method has no effect if there is no player, or the {@link #autoPlay} property is {@code false}.
	 * <p>If the player has not been initialized, the {@link #initialize()} method is called first.</p>
	 *
	 * @see #PLAYER
	 * @since 0.0.1
	 */
	public static void autoPlay() {
		if(PLAYER != null) {
			if(PLAYER.musicPaths == null) {
				PLAYER.initialize();
			}
			if(PLAYER.autoPlay.get()) {
				PLAYER.start();
			}
		}
	}

	/**
	 * The active {@link AudioPlayer} instance. After the configuration for the audio player is loaded, this field is effectively final.
	 *
	 * @since 0.0.1
	 */
	public static @Nullable AudioPlayer getPlayer() {
		return PLAYER;
	}

	/**
	 * Attempts to toggle the state of the audio player (between playing and pausing). Has no effect if there is no active player.
	 *
	 * @see #PLAYER
	 * @see #PLAYING
	 * @see #currentPlayer
	 * @since 0.0.1
	 */
	public static void toggleState() {
		if(PLAYER == null || PLAYER.currentPlayer == null || (PLAYER.currentPlayer.getStatus() != Status.PLAYING && PLAYER.currentPlayer.getStatus() != Status.PAUSED)) {
			if(PLAYER == null) {
				new AudioPlayer().initialize();
			}
			if(PLAYER.currentPlayer == null) {
				PLAYER.start();
			}
			return;
		}
		if(PLAYER.currentPlayer.getStatus() == Status.PLAYING) {
			PLAYER.currentPlayer.pause();
		} else {
			PLAYER.currentPlayer.play();
		}
	}

	/**
	 * Gets whether this player should start when the application is launched.
	 *
	 * @return {@link #autoPlay}
	 * @since 0.0.1
	 */
	public @NotNull SimpleBooleanProperty autoPlayProperty() {
		return autoPlay;
	}

	/**
	 * Gets whether this player can use built-in music.
	 *
	 * @return {@link #builtin}
	 * @since 0.0.1
	 */
	public @NotNull SimpleBooleanProperty builtinMusicProperty() {
		return builtin;
	}

	/**
	 * Gets whether this player can use user-defined music.
	 *
	 * @return {@link #custom}
	 * @since 0.0.1
	 */
	public @NotNull SimpleBooleanProperty customMusicProperty() {
		return custom;
	}

	/**
	 * Gets the volume of the audio player.
	 *
	 * @return {@link #volume}
	 * @since 0.0.1
	 */
	public @NotNull SimpleDoubleProperty getVolume() {
		return volume;
	}

	/**
	 * Gets whether this player is muted. This is independent of the {@link #getVolume() volume} of the player.
	 *
	 * @return {@link #muted}
	 * @since 0.0.1
	 */
	public @NotNull SimpleBooleanProperty isMuted() {
		return muted;
	}

	/**
	 * Gets the file name of a URI. Might return bullshit values.
	 *
	 * @return The name of the resource
	 * @since 0.0.1
	 */
	private @NotNull String getUriFileName(@NotNull URI uri) {
		String[] parts = URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8).split("/");
		return parts[parts.length - 1];
	}

	/**
	 * Initializes the audio player by listing the available audio files and registering a listener for {@link SessionHelper#ANY_RUNNING}.
	 *
	 * @since 0.0.1
	 */
	private void initialize() {
		//adding audio files for background music
		//audio files are either packaged, or in the data folder
		try {
			musicPaths = new ArrayList<>();
			if(builtin.get()) {
				try {
					musicPaths.addAll(FileUtils.walk(AudioPlayer.class.getResource("").toURI()).stream().map(Path::toUri).toList());
				} catch(Exception e) {
					log.debug(localize("log.audio.query.error", e.getMessage()));
					for(String s : (ArrayList<String>) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.themes")) {
						musicPaths.add(URI.create(AudioPlayer.class.getResource("").toURI() + "/" + s));
					}
				}
			}
			if(custom.get()) {
				File file = new File(AppConfiguration.DATA_HOME, "music");
				if(file.isDirectory()) {
					musicPaths.addAll(Arrays.stream(file.listFiles()).map(File::toURI).toList());
				}
			}
			musicPaths.removeIf(o -> {
				try {
					return Files.isDirectory(Path.of(o));
				} catch(Exception e) {
					return false;
				}
			});
			musicPaths.removeIf(path -> path.toString().endsWith(".class"));
			SessionHelper.ANY_RUNNING.addListener((observable, oldValue, anyRunning) -> {
				if(oldValue != anyRunning) {
					boolean previousState = oldValue ? !autoPause.get() : autoPlay.get();
					boolean manual = PLAYING.get() != previousState;
					if(manual) {
						return;
					}
					if(anyRunning) {
						if(autoPause.get()) {
							if(currentPlayer != null) {
								currentPlayer.pause();
							}
						}
					} else {
						if(autoPlay.get()) {
							if(currentPlayer == null) {
								start();
							} else {
								currentPlayer.play();
							}
						}
					}
				}
			});
		} catch(URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Starts playback of an audio file. If there is an active player, it is started. Otherwise, a new player is created and started.
	 *
	 * @see #currentPlayer
	 * @since 0.0.1
	 */
	private void start() {
		if(currentPlayer == null) {
			if(!musicPaths.isEmpty()) {
				URI path = musicPaths.get((int) (Math.random() * musicPaths.size()));
				try {
					Media media = new Media(path.toString());
					MapChangeListener<String, Object> listener = change -> {
						String artist = (String) media.getMetadata().get("artist");
						String title = (String) media.getMetadata().get("title");
						CURRENT_TITLE_TEXT.unbind();
						if(artist == null) {
							if(title == null) {
								CURRENT_TITLE_TEXT.bind(Bindings.createStringBinding(() -> localize("audio.display.neither", getUriFileName(path)), LAUNCHER.localeProperty()));
							} else {
								CURRENT_TITLE_TEXT.bind(Bindings.createStringBinding(() -> localize("audio.display.title", getUriFileName(path), title), LAUNCHER.localeProperty()));
							}
						} else {
							if(title == null) {
								CURRENT_TITLE_TEXT.bind(Bindings.createStringBinding(() -> localize("audio.display.artist", getUriFileName(path), artist), LAUNCHER.localeProperty()));
							} else {
								CURRENT_TITLE_TEXT.bind(Bindings.createStringBinding(() -> localize("audio.display.both", getUriFileName(path), artist, title), LAUNCHER.localeProperty()));
							}
						}
					};
					CURRENT_TITLE_TEXT.bind(Bindings.createStringBinding(() -> localize("audio.display.neither", getUriFileName(path)), LAUNCHER.localeProperty()));
					media.getMetadata().addListener(listener);
					MediaPlayer player = new MediaPlayer(media);
					player.setAutoPlay(false);
					player.volumeProperty().bind(volume);
					player.setOnEndOfMedia(() -> {
						currentPlayer = null;
						player.dispose();
						media.getMetadata().removeListener(listener);
						PLAYING.unbind();
						Platform.runLater(AudioPlayer.PLAYER::start);
					});
					player.play();
					currentPlayer = player;
					currentPlayer.muteProperty().bind(muted);
					PLAYING.bind(Bindings.createBooleanBinding(() -> currentPlayer != null && currentPlayer.getStatus() == Status.PLAYING, currentPlayer.statusProperty()));
					PLAYER_EXISTS.set(true);
				} catch(Exception e) {
					log.error(localize("log.audio.error", e.getMessage(), getUriFileName(path)));
					musicPaths.remove(path);
					start();
				}
			} else {
				log.warn(localize("log.audio.missing"));
			}
		} else {
			currentPlayer.play();
		}
	}
}