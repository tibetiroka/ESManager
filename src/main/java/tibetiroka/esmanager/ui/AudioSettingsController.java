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

	@FXML
	public void initialize() {
		autoplay.selectedProperty().bindBidirectional(AudioPlayer.getPlayer().autoPlayProperty());
		autopause.selectedProperty().bindBidirectional(AudioPlayer.getPlayer().autoPause);
		builtin.selectedProperty().bindBidirectional(AudioPlayer.getPlayer().builtinMusicProperty());
		custom.selectedProperty().bindBidirectional(AudioPlayer.getPlayer().customMusicProperty());
		mute.selectedProperty().bindBidirectional(AudioPlayer.getPlayer().isMuted());
		volume.valueProperty().bindBidirectional(AudioPlayer.getPlayer().getVolume());
		//
		LAUNCHER.disableSelfLocalization(volume);
		LAUNCHER.disableChildrenLocalization(volume);
	}
}