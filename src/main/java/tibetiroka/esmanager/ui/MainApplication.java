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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.Main;
import tibetiroka.esmanager.audio.AudioPlayer;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.config.Launcher;
import tibetiroka.esmanager.instance.Instance;
import tibetiroka.esmanager.instance.InstanceUtils;
import tibetiroka.esmanager.launcher.SelfUpdater;
import tibetiroka.esmanager.plugin.PluginManager;
import tibetiroka.esmanager.utils.FileUtils;
import tibetiroka.esmanager.utils.Statistics.GlobalStatistics;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;
import static tibetiroka.esmanager.config.Launcher.localize;

public class MainApplication extends Application {
	public static final ArrayList<ObservableList<String>> STYLE_SHEET_LISTS = new ArrayList<>();
	public static final PseudoClass TEXT_ERROR_CLASS = PseudoClass.getPseudoClass("error");
	private static final Logger log = LoggerFactory.getLogger(MainApplication.class);
	public static ObservableList<String> MAIN_WINDOW_STYLESHEETS;
	protected static Stage PRIMARY_STAGE;

	public static void createBlockingStage(Scene scene, String titleKey, Consumer<Stage> beforeShowAction, Consumer<Stage> afterHideAction) {
		Stage dialog = new Stage();
		//
		STYLE_SHEET_LISTS.add(scene.getStylesheets());
		refreshTheme();
		dialog.setScene(scene);
		dialog.initOwner(PRIMARY_STAGE);
		dialog.initModality(Modality.WINDOW_MODAL);
		dialog.setTitle(titleKey);
		LAUNCHER.localizeNode(dialog, LAUNCHER.localeProperty());
		//
		if(beforeShowAction != null) {
			beforeShowAction.accept(dialog);
		}
		//
		GaussianBlur blurEffect = new GaussianBlur(5);
		PRIMARY_STAGE.getScene().getRoot().setEffect(blurEffect);
		//
		dialog.showAndWait();
		//
		PRIMARY_STAGE.getScene().getRoot().setEffect(null);
		removeExtraStyles();
		if(afterHideAction != null) {
			afterHideAction.accept(dialog);
		}
	}

	public static boolean createDialog(String message, boolean isYesNo) {
		Phaser p = new Phaser(2);
		AtomicBoolean value = new AtomicBoolean();
		Platform.runLater(() -> {
			Dialog<ButtonType> dialog = new Dialog<>();
			STYLE_SHEET_LISTS.add(dialog.getDialogPane().getStylesheets());
			dialog.setTitle(localize(message + ".title"));
			dialog.setContentText(localize(message));
			if(isYesNo) {
				dialog.getDialogPane().getButtonTypes().add(new ButtonType(localize(message + ".yes"), ButtonData.YES));
				dialog.getDialogPane().getButtonTypes().add(new ButtonType(localize(message + ".no"), ButtonData.NO));
			} else {
				dialog.getDialogPane().getButtonTypes().add(new ButtonType(localize(message + ".ok"), ButtonData.OK_DONE));
			}
			refreshTheme();
			//
			GaussianBlur blurEffect = new GaussianBlur(5);
			PRIMARY_STAGE.getScene().getRoot().setEffect(blurEffect);
			//
			Optional<ButtonType> result = dialog.showAndWait();
			value.set(result.isPresent() && result.get().getButtonData() == ButtonData.YES);
			STYLE_SHEET_LISTS.remove(dialog.getDialogPane().getStylesheets());
			//
			PRIMARY_STAGE.getScene().getRoot().setEffect(null);
			//
			p.arriveAndDeregister();
		});
		p.arriveAndAwaitAdvance();
		return value.get();
	}

	public static String createTextInputDialog(String message, String defaultValue) {
		Phaser p = new Phaser(2);
		AtomicReference<String> value = new AtomicReference<>();
		Platform.runLater(() -> {
			TextInputDialog dialog = new TextInputDialog(defaultValue);
			STYLE_SHEET_LISTS.add(dialog.getDialogPane().getStylesheets());
			dialog.setTitle(localize(message + ".title"));
			dialog.setContentText(localize(message));
			dialog.getDialogPane().getButtonTypes().setAll(new ButtonType(localize(message + ".ok"), ButtonData.OK_DONE), new ButtonType(localize(message + ".cancel"), ButtonData.CANCEL_CLOSE));
			dialog.setGraphic(null);
			dialog.setHeaderText(null);
			refreshTheme();
			//

			//
			GaussianBlur blurEffect = new GaussianBlur(5);
			PRIMARY_STAGE.getScene().getRoot().setEffect(blurEffect);
			//
			value.set(dialog.showAndWait().orElse(defaultValue));
			STYLE_SHEET_LISTS.remove(dialog.getDialogPane().getStylesheets());
			PRIMARY_STAGE.getScene().getRoot().setEffect(null);
			//
			p.arriveAndDeregister();
		});
		p.arriveAndAwaitAdvance();
		return value.get();
	}

	public static Set<String> getThemeNames() {
		HashSet<String> themeNames = new HashSet<>();
		if(AppConfiguration.isNativeImage()) {
			for(String s : (ArrayList<String>) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.themes")) {
				themeNames.add(s);
			}
		} else {
			try {
				themeNames.addAll(FileUtils.walk(MainApplication.class.getResource("themes/").toURI())
				                           .stream()
				                           .filter(path -> path.getFileName().toString().endsWith(".css"))
				                           .map(path1 -> path1.getFileName().toString())
				                           .map(s -> s.substring(0, s.length() - ".css".length()))
				                           .toList()
				);
			} catch(IOException | URISyntaxException | FileSystemNotFoundException e) {
				log.debug(localize("log.launcher.theme.query.error", e.getMessage()));
			}
		}
		File user = new File(AppConfiguration.CONFIG_HOME, "themes");
		if(user.isDirectory()) {
			for(File file : user.listFiles()) {
				if(file.isFile() && file.getName().endsWith(".css")) {
					themeNames.add(file.getName().substring(0, file.getName().length() - ".css".length()));
				}
			}
		}
		return themeNames;
	}

	public static void refreshTheme() {
		setTheme(Launcher.getLauncher().themeProperty().get());
		log.debug(localize("log.launcher.theme.refresh"));
	}

	public static void removeExtraStyles() {
		STYLE_SHEET_LISTS.clear();
		STYLE_SHEET_LISTS.add(MAIN_WINDOW_STYLESHEETS);
	}

	public static void setContextMenu(Node node, ContextMenu menu) {
		LAUNCHER.localizeNode(menu, LAUNCHER.localeProperty());
		node.setOnContextMenuRequested(e -> menu.show(node, e.getScreenX(), e.getScreenY()));
		node.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> menu.hide());
	}

	public static void setTheme(String name) {
		String sheet;
		File customDir = new File(AppConfiguration.CONFIG_HOME, "themes");
		File custom = new File(customDir, name + ".css");
		if(custom.isFile()) {
			try {
				sheet = custom.toURI().toURL().toString();
			} catch(MalformedURLException e) {
				throw new RuntimeException(e);
			}
		} else {
			sheet = MainApplication.class.getResource("themes/" + name + ".css").toString();
		}
		for(ObservableList<String> styleSheets : STYLE_SHEET_LISTS) {
			styleSheets.setAll(sheet);
		}
		log.debug(localize("log.launcher.theme.change", name));
	}

	public static void switchScene(Stage stage, Scene newScene) {
		if(newScene != null) {
			STYLE_SHEET_LISTS.remove(stage.getScene().getStylesheets());
			STYLE_SHEET_LISTS.add(newScene.getRoot().getStylesheets());
			refreshTheme();
			double prevH = stage.getHeight();
			double prevW = stage.getWidth();
			stage.setScene(newScene);
			stage.setWidth(Math.max(stage.getWidth(), prevW));
			stage.setHeight(Math.max(stage.getHeight(), prevH));
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			Main.configureThread(Thread.currentThread(), true);
			PRIMARY_STAGE = primaryStage;
			FXMLLoader fxmlLoader = new FXMLLoader(MainController.class.getResource("main-view.fxml"));
			Scene scene = new Scene(fxmlLoader.load());
			configureGlobalKeyBinds(scene);
			primaryStage.setTitle("title");
			LAUNCHER.localizeNode(primaryStage, LAUNCHER.localeProperty());
			primaryStage.getIcons().add(new Image(MainApplication.class.getResourceAsStream("icon.png")));
			primaryStage.setScene(scene);
			primaryStage.setMaximized(true);
			primaryStage.setMinHeight(200);
			primaryStage.setMinWidth(200);
			primaryStage.setOnCloseRequest(event -> {
				event.consume();
				//prevent exiting if updates are in progress
				if(PluginManager.updateInProgressProperty().get()) {
					return;
				}
				for(Instance instance : Instance.getInstances()) {
					if(instance.getTracker().isWorkingProperty().get()) {
						return;
					}
				}
				AppConfiguration.saveAll();
				primaryStage.close();
			});
			MAIN_WINDOW_STYLESHEETS = scene.getRoot().getStylesheets();
			STYLE_SHEET_LISTS.add(MAIN_WINDOW_STYLESHEETS);
			Launcher.getLauncher().themeProperty().addListener((observable, oldValue, newValue) -> setTheme(newValue));
			refreshTheme();
			//
			primaryStage.show();
			//
			MainController.getController().getPluginListBox().setDisable(true);
			//loading data
			AppConfiguration.discoverInstances();
			AppConfiguration.loadPluginConfiguration();
			AppConfiguration.loadStatisticsConfiguration();
			StatisticsController.bind();
			GlobalStatistics.getGlobalStatistics().advanceLaunchCounter();
			GameSettingsController.bind();
			UpdateSettingsController.bind();
			AppConfiguration.loadBuildConfiguration();
			BuildSettingsController.bind();
			AppConfiguration.loadGitConfiguration();
			GitSettingsController.bind();
			AppConfiguration.loadAudioPlayer();
			AudioSettingsController.bind();
			//background tasks: plugin loading, audio, updating
			//Launcher updates are always done before instance updates to prevent messing up the launcher in the middle of an instance update
			new Thread(() -> {
				Main.configureThread(Thread.currentThread(), false);
				try {
					if(SelfUpdater.areUpdatesSupported()) {
						if(SelfUpdater.needsUpdate() && createDialog("launcher.update.ask", true)) {
							try {
								SelfUpdater.update();
								createDialog("launcher.update.done", false);
							} catch(Exception e) {
								createDialog("launcher.update.fail", false);
								throw e;
							}
						}
					} else {
						log.info(localize("log.launcher.update.unsupported"));
					}
				} catch(Exception e) {
					log.warn(localize("log.launcher.update.fail", e.getMessage()), e);
				}
				if(LAUNCHER.AutoUpdateInstancesProperty().get()) {
					Platform.runLater(() -> {
						for(Instance instance : Instance.getInstances()) {
							new Thread(() -> {
								log.info(localize("log.launcher.autoupdate.instance.begin", instance.getPublicName()));
								InstanceUtils.update(instance);
								log.info(localize("log.launcher.autoupdate.instance.end", instance.getPublicName()));
							}, "Updater thread for " + instance.getPublicName()).start();
						}
					});
				}
			}, "Launcher Updater Thread").start();
			Platform.runLater(AudioPlayer::autoPlay);
			new Thread(() -> {
				Main.configureThread(Thread.currentThread(), false);
				try {
					AppConfiguration.discoverPlugins();
				} catch(Exception e) {
					log.warn(localize("plugin.discover.fail", e.getMessage()), e);
				}
				if(PluginManager.getManager().getAutoUpdatePlugins().get()) {
					PluginManager.updatePlugins();
				}
				Platform.runLater(() -> MainController.getController().getPluginListBox().setDisable(false));
			}, "Plugin Query Thread").start();
			Timeline timer = new Timeline(new KeyFrame(Duration.ZERO, new EventHandler<>() {
				Instant last = Instant.now();

				@Override
				public void handle(ActionEvent event) {
					Instant now = Instant.now();
					GlobalStatistics.getGlobalStatistics().advanceActiveTimeCounter(now.toEpochMilli() - last.toEpochMilli());
					last = now;
				}
			}), new KeyFrame(new Duration(100)));
			timer.setCycleCount(Timeline.INDEFINITE);
			timer.play();
		} catch(Exception e) {
			e.printStackTrace();
			throw new Error(e);
		}
	}

	@Override
	public void stop() throws Exception {
		AppConfiguration.saveAll();
		System.exit(0);
	}

	private void configureGlobalKeyBinds(Scene scene) {
		scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			switch(event.getCode()) {
				case SPACE -> MainController.getController().audioButton.fire();
			}
		});
	}
}