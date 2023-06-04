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

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;
import static tibetiroka.esmanager.plugin.PluginManager.MANAGER;

public class GameSettingsController {
	private static GameSettingsController CONTROLLER;
	@FXML
	protected CheckBox debug;
	@FXML
	protected CheckBox preservePlugins;

	public static void bind() {
		CONTROLLER.debug.selectedProperty().bindBidirectional(LAUNCHER.debugByDefaultProperty());
		CONTROLLER.preservePlugins.selectedProperty().bindBidirectional(MANAGER.getPreservePlugins());
	}

	@FXML
	public void initialize() {
		CONTROLLER = this;
	}
}