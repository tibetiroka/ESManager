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
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.apache.logging.log4j.LogManager;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.instance.Instance;
import tibetiroka.esmanager.instance.InstanceUtils;
import tibetiroka.esmanager.instance.SessionHelper;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.IOException;

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;
import static tibetiroka.esmanager.config.Launcher.localize;

public class InstanceController {
	private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(InstanceController.class);
	@FXML
	protected HBox container;
	@FXML
	protected Label nameField;
	@FXML
	protected SplitMenuButton playButton;
	@FXML
	protected ProgressIndicator progressIndicator;
	@FXML
	protected Label sourceField;
	@FXML
	protected Label versionField;
	@FXML
	protected BorderPane warningNode;
	private Instance instance;

	public void delete() {
		//Removing this visual element will cause the instance.remove() method to be called.
		//See initialize().
		((Pane) container.getParent()).getChildren().remove(container);
	}

	public void initialize(Instance instance) {
		this.instance = instance;
		//
		//Adding listener for instance deletion
		container.parentProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue == null) {
				new Thread(() -> {
					log.warn(localize("log.instance.delete.start", instance.getName()));
					instance.remove();
					log.warn(localize("log.instance.delete.end", instance.getName()));
				}, "Remover thread for " + instance.getName()).start();
			}
		});
		container.disableProperty().bind(instance.getTracker().isWorkingProperty());
		nameField.textProperty().set(instance.getName());
		LAUNCHER.disableLocalization(nameField);
		sourceField.textProperty().bind(instance.createSourceStringBinding());
		LAUNCHER.disableLocalization(sourceField);
		versionField.textProperty().bind(instance.createVersionStringBinding());
		LAUNCHER.disableLocalization(versionField);
		Tooltip tooltip = new Tooltip("instance.error.tooltip");
		tooltip.getStyleClass().setAll("instance-warning-pane-tooltip");
		Tooltip.install(warningNode, tooltip);
		LAUNCHER.localizeNode(tooltip);
		warningNode.visibleProperty().bind(instance.getTracker().failedUpdateProperty());
		progressIndicator.visibleProperty().bind(instance.getTracker().hasUpdatedProperty());
		progressIndicator.progressProperty().bind(instance.getTracker().updateProgressProperty());
		LAUNCHER.disableLocalization(progressIndicator);
		playButton.disableProperty().bind(SessionHelper.ANY_RUNNING);
	}

	@FXML
	public void openFolder() {
		if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.OPEN) && Desktop.getDesktop().isSupported(Action.BROWSE) && Desktop.getDesktop().isSupported(Action.BROWSE_FILE_DIR)) {
			try {
				log.info(localize("log.instance.open", instance.getName()));
				Desktop.getDesktop().open(instance.getDirectory());
			} catch(IOException e) {
				log.error(localize("log.instance.open.fail", instance.getName(), e.getMessage()));
				throw new RuntimeException(e);
			}
		} else if(isGioSupported()) {
			ProcessBuilder builder = new ProcessBuilder("gio", "open", instance.getDirectory().getAbsolutePath());
			try {
				builder.start();
			} catch(IOException e) {
				log.error(localize("log.instance.open.fail", instance.getName(), e.getMessage()));
				throw new RuntimeException(e);
			}
		} else {
			log.error(localize("log.instance.open.unsupported", instance.getName()));
		}
	}

	@FXML
	public void play() {
		if(LAUNCHER.debugByDefaultProperty().get()) {
			playDebug();
		} else {
			playRegular();
		}
	}

	@FXML
	public void playDebug() {
		SessionHelper.start(instance, true);
	}

	@FXML
	public void playRegular() {
		SessionHelper.start(instance, false);
	}

	@FXML
	public void update() {
		log.info(localize("log.instance.update.manual", instance.getName()));
		new Thread(() -> {
			InstanceUtils.update(instance);
			log.info(localize("log.instance.update.manual.done", instance.getName()));
		}, "Force-updater thread for " + instance.getName()).start();
	}

	private boolean isGioSupported() {
		if(AppConfiguration.isWindows()) {
			return false;
		}
		try {
			Process p = Runtime.getRuntime().exec(new String[]{"which", "gio"});
			int result = p.waitFor();
			return result == 0;
		} catch(Exception e) {
			return false;
		}
	}
}