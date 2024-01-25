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

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.util.StringConverter;
import tibetiroka.esmanager.config.Launcher;
import tibetiroka.esmanager.instance.BuildHelper;
import tibetiroka.esmanager.instance.BuildHelper.BuildSystem;

import java.util.Arrays;

public class BuildSettingsController {
	private static BuildSettingsController CONTROLLER;
	@FXML
	protected ChoiceBox<BuildSystem> buildSystem;
	@FXML
	protected Label buildSystemLabel;
	@FXML
	protected CheckBox optimize;

	public static void bind() {
		CONTROLLER.buildSystem.valueProperty().bindBidirectional(BuildHelper.getBuilder().buildSystemProperty());
		CONTROLLER.optimize.selectedProperty().bindBidirectional(BuildHelper.getBuilder().optimizeProperty());
	}

	@FXML
	public void initialize() {
		CONTROLLER = this;
		buildSystem.setItems(FXCollections.observableList(Arrays.stream(BuildSystem.values()).toList()));
		Launcher.getLauncher().disableSelfLocalization(buildSystem);
		Launcher.getLauncher().disableChildrenLocalization(buildSystem);
		buildSystem.setConverter(new StringConverter<>() {
			@Override
			public String toString(BuildSystem object) {
				if(object == null) {
					return null;
				}
				return object.name().toLowerCase();
			}

			@Override
			public BuildSystem fromString(String string) {
				if(string == null) {
					return null;
				}
				return BuildSystem.valueOf(string.toUpperCase());
			}
		});
	}
}