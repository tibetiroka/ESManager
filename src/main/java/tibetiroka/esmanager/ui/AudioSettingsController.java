/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.ui;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import tibetiroka.esmanager.audio.AudioPlayer;

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;

public class AudioSettingsController {
	private static AudioSettingsController CONTROLLER;
	@FXML
	protected CheckBox autopause;
	@FXML
	protected CheckBox autoplay;
	@FXML
	protected CheckBox builtin;
	@FXML
	protected CheckBox custom;
	@FXML
	protected CheckBox mute;
	@FXML
	protected Slider volume;
	@FXML
	protected Label volumeLabel;

	public static void bind() {
		CONTROLLER.autoplay.selectedProperty().bindBidirectional(AudioPlayer.getPlayer().autoPlayProperty());
		CONTROLLER.autopause.selectedProperty().bindBidirectional(AudioPlayer.getPlayer().autoPause);
		CONTROLLER.builtin.selectedProperty().bindBidirectional(AudioPlayer.getPlayer().builtinMusicProperty());
		CONTROLLER.custom.selectedProperty().bindBidirectional(AudioPlayer.getPlayer().customMusicProperty());
		CONTROLLER.mute.selectedProperty().bindBidirectional(AudioPlayer.getPlayer().isMuted());
		CONTROLLER.volume.valueProperty().bindBidirectional(AudioPlayer.getPlayer().getVolume());
	}

	@FXML
	public void initialize() {
		CONTROLLER = this;
		LAUNCHER.disableSelfLocalization(volume);
		LAUNCHER.disableChildrenLocalization(volume);
	}
}