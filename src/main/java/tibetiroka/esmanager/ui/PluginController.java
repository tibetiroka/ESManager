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
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.CacheHint;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.Main;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.config.Launcher;
import tibetiroka.esmanager.instance.SessionHelper;
import tibetiroka.esmanager.instance.SystemUtils;
import tibetiroka.esmanager.plugin.LocalPlugin;
import tibetiroka.esmanager.plugin.PluginManager;
import tibetiroka.esmanager.plugin.RemotePlugin;

import java.io.IOException;

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;
import static tibetiroka.esmanager.config.Launcher.localize;

public class PluginController {
	private static final Logger log = LoggerFactory.getLogger(PluginManager.class);
	@FXML
	protected Label authors;
	@FXML
	protected ContextMenu contextMenu;
	@FXML
	protected MenuItem delete;
	@FXML
	protected Button deleteButton;
	@FXML
	protected Label description;
	@FXML
	protected Button downloadButton;
	@FXML
	protected BorderPane imagePane;
	@FXML
	protected MenuItem manage;
	@FXML
	protected Label name;
	@FXML
	protected MenuItem open;
	protected RemotePlugin plugin;
	@FXML
	protected HBox pluginBox;
	@FXML
	protected ProgressIndicator progressIndicator;

	@FXML
	public void initialize(@NotNull RemotePlugin plugin) {
		this.plugin = plugin;
		deleteButton.setDisable(plugin.findLocal() == null);
		pluginBox.disableProperty().bind(SessionHelper.ANY_RUNNING);
		name.setText(plugin.getName());
		progressIndicator.progressProperty().bind(plugin.getProgressTracker().updateProgressProperty());
		progressIndicator.visibleProperty().bind(plugin.downloadInProgressProperty());
		LAUNCHER.disableLocalization(progressIndicator);
		if(plugin.getShortDescription() == null) {
			description.textProperty().bind(Bindings.createStringBinding(() -> localize("plugin.description.text.missing")));
			Font f = Font.font(description.getFont().getFamily(), FontWeight.NORMAL, FontPosture.ITALIC, description.getFont().getSize());
			description.setFont(f);
		} else {
			description.setText(plugin.getShortDescription());
		}
		if(plugin.getAuthors() == null) {
			authors.textProperty().bind(Bindings.createStringBinding(() -> localize("plugin.author.text.missing")));
			Font f = Font.font(authors.getFont().getFamily(), FontWeight.NORMAL, FontPosture.ITALIC, authors.getFont().getSize());
			authors.setFont(f);
		} else {
			authors.textProperty().bind(Bindings.createStringBinding(() -> localize("plugin.author.text", plugin.getAuthors())));
		}
		if(plugin.getIconUrl() != null) {
			new Thread(() -> {
				Main.configureThread(Thread.currentThread(), false);
				ImageView pluginImage = new ImageView(new Image(plugin.getIconUrl().toExternalForm()));
				pluginImage.setCache(true);
				pluginImage.setCacheHint(CacheHint.SPEED);
				pluginImage.setSmooth(true);
				pluginImage.setPreserveRatio(true);
				pluginImage.fitHeightProperty().bind(imagePane.heightProperty());
				pluginImage.fitWidthProperty().bind(imagePane.widthProperty());
				Platform.runLater(() -> imagePane.setCenter(pluginImage));
			}).start();
		}
		open.disableProperty().bind(plugin.installedProperty().map(o -> !o));
		delete.disableProperty().bind(plugin.installedProperty().map(o -> !o));
		manage.disableProperty().bind(plugin.installedProperty().map(o -> !o));
	}

	@FXML
	public void openDirectory() {
		SystemUtils.openDirectory(plugin.findLocal().getInstallLocation());
	}

	@FXML
	protected void delete() {
		deleteButton.setDisable(true);
		LocalPlugin local = plugin.findLocal();
		if(local != null) {
			new Thread(() -> {
				log.warn(localize("log.plugin.delete", local.getName()));
				try {
					local.remove();
					log.info(localize("log.plugin.delete.end", local.getName()));
				} catch(IOException e) {
					deleteButton.setDisable(false);
					log.error(localize("log.plugin.delete.fail", e.getMessage(), local.getName()), e);
				}
			}, "Plugin deleter thread for " + plugin.getName()).start();
		}
	}

	@FXML
	protected void download() {
		PluginManager.updateInProgressProperty().set(true);
		downloadButton.setDisable(true);
		deleteButton.setDisable(true);
		new Thread(() -> {
			if(plugin.findLocal() == null) {
				log.info(localize("log.plugin.download", plugin.getName()));
				try {
					PluginManager.getManager().getInstalledPlugins().add(plugin.install());
					AppConfiguration.savePluginConfiguration();
					deleteButton.setDisable(false);
					log.info(localize("log.plugin.download.done", plugin.getName()));
				} catch(IOException e) {
					log.error(localize("log.plugin.download.fail", e.getMessage(), plugin.getName()), e);
				} finally {
					downloadButton.setDisable(false);
					PluginManager.updateInProgressProperty().set(false);
				}
			} else {
				plugin.findLocal().updateIfRequired();
				deleteButton.setDisable(false);
				downloadButton.setDisable(false);
				PluginManager.updateInProgressProperty().set(false);
			}
		}, "Plugin downloader thread for " + plugin.getName()).start();
	}

	@FXML
	protected void initialize() {
		Launcher.getLauncher().disableLocalization(name);
		Launcher.getLauncher().disableLocalization(description);
		Launcher.getLauncher().disableLocalization(authors);
		MainApplication.setContextMenu(pluginBox, contextMenu);
	}

	@FXML
	protected void manage() {
		try {
			FXMLLoader loader = new FXMLLoader(PluginManagerController.class.getResource("plugin-manager.fxml"));
			Parent p = loader.load();
			Scene scene = new Scene(p);
			((PluginManagerController) loader.getController()).initialize(PluginManager.findLocal(plugin.getName()));
			MainApplication.createBlockingStage(scene, "plugin.manage.title", null, null);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}