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

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.eclipse.jgit.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import tibetiroka.esmanager.audio.AudioPlayer;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.instance.SystemUtils;
import tibetiroka.esmanager.plugin.PluginManager;
import tibetiroka.esmanager.ui.instance.NewInstanceController;

import java.io.File;
import java.io.IOException;

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;

public class MainController {
	private static MainController CONTROLLER;
	@FXML
	public Button newInstanceButton;
	@FXML
	protected TitledPane audio;
	@FXML
	protected HBox audioBox;
	@FXML
	protected Button audioButton;
	@FXML
	protected Label audioLabel;
	@FXML
	protected AnchorPane audioSettings;
	@FXML
	protected TitledPane build;
	@FXML
	protected AnchorPane buildSettings;
	@FXML
	protected TitledPane game;
	@FXML
	protected AnchorPane gameSettings;
	@FXML
	protected TitledPane git;
	@FXML
	protected AnchorPane gitSettings;
	@FXML
	protected VBox instanceListBox;
	@FXML
	protected TitledPane launcher;
	@FXML
	protected AnchorPane launcherSettings;
	@FXML
	protected TextFlow logArea;
	@FXML
	protected ScrollPane logScroll;
	@FXML
	protected VBox pluginListBox;
	@FXML
	protected TabPane tabs;
	@FXML
	protected TitledPane update;
	@FXML
	protected AnchorPane updateSettings;

	public MainController() {
		if(CONTROLLER != null) {
			throw new IllegalStateException("There is already a main controller!");
		}
		CONTROLLER = this;
	}

	public static @Nullable MainController getController() {
		return CONTROLLER;
	}

	public VBox getInstanceListBox() {
		return instanceListBox;
	}

	public TextFlow getLogArea() {
		return logArea;
	}

	public VBox getPluginListBox() {
		return pluginListBox;
	}

	public void log(Text text) {
		Platform.runLater(() -> {
			logArea.getChildren().add(text);
			logArea.applyCss();
		});
	}

	public void openLogFile() {
		SystemUtils.openFile(new File(AppConfiguration.LOG_HOME, "latest.log"));
	}

	@FXML
	protected void initialize() {
		//logger config
		logScroll.vvalueProperty().bind(logArea.heightProperty());
		//loading data
		AppConfiguration.discoverInstances();
		AppConfiguration.loadPluginConfiguration();
		AppConfiguration.loadBuildConfiguration();
		AppConfiguration.loadGitConfiguration();
		AppConfiguration.loadAudioPlayer();
		//settings panel
		try {
			audioSettings.getChildren().setAll((Node) new FXMLLoader(AudioSettingsController.class.getResource("audio-settings.fxml")).load());
			updateSettings.getChildren().setAll((Node) new FXMLLoader(UpdateSettingsController.class.getResource("update-settings.fxml")).load());
			gitSettings.getChildren().setAll((Node) new FXMLLoader(UpdateSettingsController.class.getResource("git-settings.fxml")).load());
			gameSettings.getChildren().setAll((Node) new FXMLLoader(GameSettingsController.class.getResource("game-settings.fxml")).load());
			buildSettings.getChildren().setAll((Node) new FXMLLoader(BuildSettingsController.class.getResource("build-settings.fxml")).load());
			launcherSettings.getChildren().setAll((Node) new FXMLLoader(LauncherSettingsController.class.getResource("launcher-settings.fxml")).load());
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		instanceListBox.disableProperty().bind(PluginManager.updateInProgressProperty());
		LAUNCHER.disableLocalization(audioBox);
		AudioPlayer.PLAYING.addListener((observable, oldValue, newValue) -> {
			audioButton.setGraphic(getAudioButtonIcon(newValue));
		});
		audioButton.setGraphic(getAudioButtonIcon(AudioPlayer.PLAYING.get()));
		audioLabel.textProperty().bind(AudioPlayer.CURRENT_TITLE_TEXT);
	}

	@FXML
	protected void onAudioButtonClick() {
		AudioPlayer.toggleState();
	}

	@FXML
	protected void onNewInstanceButtonClick() {
		try {
			FXMLLoader loader = new FXMLLoader(NewInstanceController.class.getResource("new-instance.fxml"));
			Parent p = loader.load();
			Scene scene = new Scene(p);
			MainApplication.createBlockingStage(scene, "instance.add.title", s -> ((NewInstanceController) loader.getController()).stage = s, null);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Node getAudioButtonIcon(boolean playing) {
		FontIcon icon = new FontIcon();
		icon.getStyleClass().add("audio-button-graphics");
		if(playing) {
			icon.setIconLiteral("fas-pause");
		} else {
			icon.setIconLiteral("fas-play");
		}
		return icon;
	}
}