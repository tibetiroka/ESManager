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
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.Main;
import tibetiroka.esmanager.instance.Instance;
import tibetiroka.esmanager.instance.InstanceUtils;
import tibetiroka.esmanager.instance.SessionHelper;
import tibetiroka.esmanager.instance.SystemUtils;
import tibetiroka.esmanager.instance.annotation.EditableSource;

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;
import static tibetiroka.esmanager.config.Launcher.localize;

public class InstanceController {
	private static final Logger log = LoggerFactory.getLogger(InstanceController.class);
	@FXML
	public ContextMenu contextMenu;
	@FXML
	protected HBox container;
	@FXML
	protected Label nameField;
	@FXML
	protected SplitMenuButton playButton;
	@FXML
	protected ProgressIndicator progressIndicator;
	@FXML
	protected MenuItem sourceEdit;
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

	@FXML
	public void editSource() {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(SourceEditorController.class.getResource(instance.getSource().getClass().getAnnotation(EditableSource.class).value()));
			SplitPane pane = fxmlLoader.load();
			((SourceEditorController) fxmlLoader.getController()).initialize(instance.getSource());
			Scene scene = new Scene(pane);
			MainApplication.createBlockingStage(scene, "source.editor.title", null, null);
		} catch(Exception e) {
			log.error(localize("log.source.editor.fail", instance.getPublicName(), e.getMessage()), e);
		}
	}

	public void initialize(Instance instance) {
		this.instance = instance;
		//
		//Adding listener for instance deletion
		container.parentProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue == null) {
				new Thread(() -> {
					Main.configureThread(Thread.currentThread(), false);
					log.warn(localize("log.instance.delete.start", instance.getPublicName()));
					InstanceUtils.remove(instance);
					log.warn(localize("log.instance.delete.end", instance.getPublicName()));
				}, "Remover thread for " + instance.getPublicName()).start();
			}
		});
		container.disableProperty().bind(instance.getTracker().isWorkingProperty());
		nameField.textProperty().bind(instance.createNameStringBinding());
		LAUNCHER.disableLocalization(nameField);
		sourceField.textProperty().bind(instance.createSourceStringBinding());
		LAUNCHER.disableLocalization(sourceField);
		versionField.textProperty().bind(instance.createVersionStringBinding());
		LAUNCHER.disableLocalization(versionField);
		Tooltip tooltip = new Tooltip("instance.error.tooltip");
		tooltip.getStyleClass().setAll("instance-warning-pane-tooltip");
		Tooltip.install(warningNode, tooltip);
		LAUNCHER.localizeNode(tooltip, LAUNCHER.localeProperty());
		warningNode.visibleProperty().bind(instance.getTracker().failedUpdateProperty());
		progressIndicator.visibleProperty().bind(instance.getTracker().hasUpdatedProperty());
		progressIndicator.progressProperty().bind(instance.getTracker().updateProgressProperty());
		LAUNCHER.disableLocalization(progressIndicator);
		playButton.disableProperty().bind(SessionHelper.ANY_RUNNING);
		MainApplication.setContextMenu(container, contextMenu);
		sourceEdit.setDisable(!instance.getSource().getClass().isAnnotationPresent(EditableSource.class));
	}

	@FXML
	public void openDirectory() {
		SystemUtils.openDirectory(instance.getDirectory());
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
	public void rename() {
		new Thread(() -> {
			Main.configureThread(Thread.currentThread(), false);
			instance.setPublicName(MainApplication.createTextInputDialog("instance.rename", instance.getPublicName()));
		}, "Instance renaming thread").start();
	}

	@FXML
	public void update() {
		log.info(localize("log.instance.update.manual", instance.getPublicName()));
		new Thread(() -> {
			Main.configureThread(Thread.currentThread(), false);
			InstanceUtils.update(instance);
			log.info(localize("log.instance.update.manual.done", instance.getPublicName()));
		}, "Force-updater thread for " + instance.getPublicName()).start();
	}
}