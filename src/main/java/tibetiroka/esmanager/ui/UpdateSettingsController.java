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
import tibetiroka.esmanager.config.Launcher;
import tibetiroka.esmanager.plugin.PluginManager;

public class UpdateSettingsController {
	private static UpdateSettingsController CONTROLLER;
	@FXML
	protected CheckBox instanceUpdate;
	@FXML
	protected CheckBox launcherUpdate;
	@FXML
	protected CheckBox pluginUpdate;

	public static void bind() {
		CONTROLLER.instanceUpdate.selectedProperty().bindBidirectional(Launcher.getLauncher().autoUpdateInstancesProperty());
		CONTROLLER.pluginUpdate.selectedProperty().bindBidirectional(PluginManager.getManager().getAutoUpdatePlugins());
		CONTROLLER.launcherUpdate.selectedProperty().bindBidirectional(Launcher.getLauncher().autoUpdateLauncherProperty());
	}

	@FXML
	public void initialize() {
		CONTROLLER = this;
	}
}