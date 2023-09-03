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

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import tibetiroka.esmanager.config.Launcher;
import tibetiroka.esmanager.instance.Instance;
import tibetiroka.esmanager.plugin.LocalPlugin;
import tibetiroka.esmanager.plugin.PluginManager;

import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;

import static tibetiroka.esmanager.config.Launcher.localize;
import static tibetiroka.esmanager.utils.Statistics.GlobalStatistics.getGlobalStatistics;

public class StatisticsController {
	private static StatisticsController CONTROLLER;
	private @FXML Label active;
	private @FXML Label bestInstanceActive;
	private @FXML Label bestInstanceLaunches;
	private @FXML Label bestPluginActive;
	private @FXML Label bestPluginLaunches;
	private @FXML Label creation;
	private @FXML Label instanceActive;
	private @FXML Label instanceCreation;
	private @FXML Label instanceLaunches;
	private @FXML Label launches;
	private @FXML Label pluginActive;
	private @FXML Label pluginDownload;
	private @FXML Label pluginLaunches;
	private @FXML VBox values;

	public StatisticsController() {
		CONTROLLER = this;
	}

	public static void bind() {
		CONTROLLER.init();
	}

	protected void init() {
		active.textProperty().bind(Bindings.createStringBinding(() -> formatDuration(getGlobalStatistics().getTimeActive().get()), Launcher.getLauncher().localeProperty(), getGlobalStatistics().getTimeActive()));
		bestInstanceActive.textProperty().bind(Bindings.createStringBinding(() -> {
			Optional<Instance> best = Instance.getInstances().stream().max(Comparator.comparingLong((Instance i) -> i.getStatistics().getTimeActive().get()));
			String key = "statistics.global.instance.best.active.value";
			if(best.isPresent()) {
				return localize(key, best.get().getPublicName(), formatDuration(best.get().getStatistics().getTimeActive().get()));
			}
			return localize(key + ".none");
		}, Launcher.getLauncher().localeProperty(), getGlobalStatistics().getInstanceStatistics().getTimeActive(), getGlobalStatistics().getTimeActive()));
		bestInstanceLaunches.textProperty().bind(Bindings.createStringBinding(() -> {
			Optional<Instance> best = Instance.getInstances().stream().max(Comparator.comparingLong((Instance i) -> i.getStatistics().getLaunches().get()));
			String key = "statistics.global.instance.best.launches.value";
			if(best.isPresent()) {
				return localize(key, best.get().getPublicName(), best.get().getStatistics().getLaunches().get());
			}
			return localize(key + ".none");
		}, Launcher.getLauncher().localeProperty(), getGlobalStatistics().getInstanceStatistics().getLaunches(), getGlobalStatistics().getTimeActive()));
		bestPluginActive.textProperty().bind(Bindings.createStringBinding(() -> {
			Optional<LocalPlugin> best = PluginManager.getManager().getInstalledPlugins().stream().max(Comparator.comparingLong((LocalPlugin p) -> p.getStatistics().getTimeActive().get()));
			String key = "statistics.global.plugin.best.active.value";
			if(best.isPresent()) {
				return localize(key, best.get().getName(), formatDuration(best.get().getStatistics().getTimeActive().get()));
			}
			return localize(key + ".none");
		}, Launcher.getLauncher().localeProperty(), getGlobalStatistics().getPluginStatistics().getTimeActive(), getGlobalStatistics().getTimeActive()));
		bestPluginLaunches.textProperty().bind(Bindings.createStringBinding(() -> {
			Optional<LocalPlugin> best = PluginManager.getManager().getInstalledPlugins().stream().max(Comparator.comparingLong((LocalPlugin p) -> p.getStatistics().getLaunches().get()));
			String key = "statistics.global.plugin.best.launches.value";
			if(best.isPresent()) {
				return localize(key, best.get().getName(), best.get().getStatistics().getLaunches().get());
			}
			return localize(key + ".none");
		}, Launcher.getLauncher().localeProperty(), getGlobalStatistics().getPluginStatistics().getLaunches(), getGlobalStatistics().getTimeActive()));
		creation.textProperty().bind(Bindings.createStringBinding(() -> localize("statistics.global.creation.value", getGlobalStatistics().getCreationTime()), Launcher.getLauncher().localeProperty()));
		instanceActive.textProperty().bind(Bindings.createStringBinding(() -> formatDuration(getGlobalStatistics().getInstanceStatistics().getTimeActive().get()), Launcher.getLauncher().localeProperty(), getGlobalStatistics().getInstanceStatistics().getTimeActive()));
		instanceCreation.textProperty().bind(Bindings.createStringBinding(() -> String.valueOf(getGlobalStatistics().getInstanceCreations().get()), Launcher.getLauncher().localeProperty(), getGlobalStatistics().getInstanceCreations()));
		instanceLaunches.textProperty().bind(Bindings.createStringBinding(() -> String.valueOf(getGlobalStatistics().getInstanceStatistics().getLaunches().get()), Launcher.getLauncher().localeProperty(), getGlobalStatistics().getInstanceStatistics().getLaunches()));
		launches.textProperty().bind(Bindings.createStringBinding(() -> String.valueOf(getGlobalStatistics().getLaunches().get()), Launcher.getLauncher().localeProperty(), getGlobalStatistics().getLaunches()));
		pluginActive.textProperty().bind(Bindings.createStringBinding(() -> formatDuration(getGlobalStatistics().getPluginStatistics().getTimeActive().get()), Launcher.getLauncher().localeProperty(), getGlobalStatistics().getPluginStatistics().getTimeActive()));
		pluginDownload.textProperty().bind(Bindings.createStringBinding(() -> String.valueOf(getGlobalStatistics().getPluginDownloads().get()), Launcher.getLauncher().localeProperty(), getGlobalStatistics().getPluginDownloads()));
		pluginLaunches.textProperty().bind(Bindings.createStringBinding(() -> String.valueOf(getGlobalStatistics().getPluginStatistics().getLaunches().get()), Launcher.getLauncher().localeProperty(), getGlobalStatistics().getPluginStatistics().getLaunches()));
	}

	@FXML
	protected void initialize() {
		Launcher.getLauncher().disableChildrenLocalization(values);
	}

	private @NotNull String formatDuration(long value) {
		String format = localize("statistics.duration.format");
		Duration d = Duration.ofMillis(value);
		return String.format(format, d.toDays(), d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart());
	}
}