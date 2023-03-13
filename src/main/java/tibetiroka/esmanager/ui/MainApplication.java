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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import tibetiroka.esmanager.Main;
import tibetiroka.esmanager.audio.AudioPlayer;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.config.Launcher;
import tibetiroka.esmanager.instance.Instance;
import tibetiroka.esmanager.instance.InstanceUtils;
import tibetiroka.esmanager.launcher.SelfUpdater;
import tibetiroka.esmanager.plugin.PluginManager;
import tibetiroka.esmanager.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;
import static tibetiroka.esmanager.config.Launcher.localize;

public class MainApplication extends Application {
	public static final ArrayList<ObservableList<String>> STYLE_SHEET_LISTS = new ArrayList<>();
	public static final PseudoClass TEXT_ERROR_CLASS = PseudoClass.getPseudoClass("error");
	private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(MainApplication.class);
	protected static Stage PRIMARY_STAGE;

	public static boolean createDialog(String message, boolean isYesNo) {
		Phaser p = new Phaser(2);
		AtomicBoolean value = new AtomicBoolean();
		Platform.runLater(()->{
			Dialog<ButtonType> dialog = new Dialog<>();
			MainApplication.STYLE_SHEET_LISTS.add(dialog.getDialogPane().getStylesheets());
			dialog.setTitle(localize(message + ".title"));
			dialog.setContentText(localize(message));
			if(isYesNo) {
				dialog.getDialogPane().getButtonTypes().add(new ButtonType(localize(message + ".yes"), ButtonData.YES));
				dialog.getDialogPane().getButtonTypes().add(new ButtonType(localize(message + ".no"), ButtonData.NO));
			} else {
				dialog.getDialogPane().getButtonTypes().add(new ButtonType(localize(message + ".ok"), ButtonData.OK_DONE));
			}
			Optional<ButtonType> result = dialog.showAndWait();
			value.set(result.isPresent() && result.get() == ButtonType.YES);
			MainApplication.STYLE_SHEET_LISTS.remove(dialog.getDialogPane().getStylesheets());
			p.arriveAndDeregister();
		});
		p.arriveAndAwaitAdvance();
		return value.get();
	}

	public static Set<String> getThemeNames() {
		HashSet<String> themeNames = new HashSet<>();
		try {
			themeNames.addAll(FileUtils.walk(MainApplication.class.getResource("themes/").toURI())
			                           .stream()
			                           .filter(path -> path.getFileName().toString().endsWith(".css"))
			                           .map(path1 -> path1.getFileName().toString())
			                           .map(s -> s.substring(0, s.length() - ".css".length()))
			                           .toList()
			);
		} catch(IOException | URISyntaxException e) {
			throw new RuntimeException(e);
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
		ObservableList<String> list = STYLE_SHEET_LISTS.get(0);
		STYLE_SHEET_LISTS.clear();
		STYLE_SHEET_LISTS.add(list);
	}

	public static void setTheme(String name) {
		for(ObservableList<String> styleSheets : STYLE_SHEET_LISTS) {
			styleSheets.clear();
			File customDir = new File(AppConfiguration.CONFIG_HOME, "themes");
			File custom = new File(customDir, name + ".css");
			if(custom.isFile()) {
				try {
					styleSheets.add(custom.toURI().toURL().toString());
				} catch(MalformedURLException e) {
					throw new RuntimeException(e);
				}
			} else {
				styleSheets.add(MainApplication.class.getResource("themes/" + name + ".css").toString());
			}
		}
		log.debug(localize("log.launcher.theme.change", name));
	}

	public static void switchScene(Stage stage, Scene newScene) {
		if(newScene != null) {
			MainApplication.STYLE_SHEET_LISTS.remove(stage.getScene().getStylesheets());
			MainApplication.STYLE_SHEET_LISTS.add(newScene.getRoot().getStylesheets());
			MainApplication.refreshTheme();
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
			LAUNCHER.localizeNode(primaryStage);
			primaryStage.getIcons().add(new Image(MainApplication.class.getResourceAsStream("icon.png")));
			primaryStage.setScene(scene);
			primaryStage.setMaximized(true);
			primaryStage.setMinHeight(200);
			primaryStage.setMinWidth(200);
			primaryStage.setOnCloseRequest(event -> {
				AppConfiguration.saveAll();
				//Delay closing the application while work is in progress
				AtomicInteger count = new AtomicInteger();
				synchronized(count) {
					for(Instance instance : Instance.getInstances()) {
						//not technically correct, but close enough
						if(instance.getTracker().isWorkingProperty().get()) {
							count.incrementAndGet();
						}
						instance.getTracker().isWorkingProperty().addListener((observable, oldValue, newValue) -> {
							if(newValue) {
								count.incrementAndGet();
							} else {
								if(count.decrementAndGet() == 0) {
									synchronized(count) {
										count.notifyAll();
									}
								}
							}
						});
					}
					boolean any = count.get() > 0;
					if(any) {
						try {
							count.wait();
						} catch(InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
				}
				primaryStage.close();
			});
			STYLE_SHEET_LISTS.add(scene.getRoot().getStylesheets());
			Launcher.getLauncher().themeProperty().addListener((observable, oldValue, newValue) -> setTheme(newValue));
			refreshTheme();
			//
			primaryStage.show();

			//background tasks: plugin loading, audio, updating
			//Launcher updates are always done before instance updates to prevent messing up the launcher in the middle of an instance update
			MainController.getController().getPluginListBox().setDisable(true);
			new Thread(() -> {
				Main.configureThread(Thread.currentThread(), false);
				try {
					if(SelfUpdater.needsUpdate() && createDialog("launcher.update.ask", true)) {
						try {
							SelfUpdater.update();
							createDialog("launcher.update.done", false);
						}catch(Exception e){
							createDialog("launcher.update.fail", false);
							throw e;
						}
					}
				} catch(Exception e) {
					log.warn(localize("log.launcher.update.fail", e.getMessage()), e);
				}
				if(LAUNCHER.AutoUpdateInstancesProperty().get()) {
					Platform.runLater(() -> {
						for(Instance instance : Instance.getInstances()) {
							new Thread(() -> {
								log.info(localize("log.launcher.autoupdate.instance.begin", instance.getName()));
								InstanceUtils.update(instance);
								log.info(localize("log.launcher.autoupdate.instance.end", instance.getName()));
							}, "Updater thread for " + instance.getName()).start();
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