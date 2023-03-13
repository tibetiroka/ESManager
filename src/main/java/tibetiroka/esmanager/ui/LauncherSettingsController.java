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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Locale;

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;

public class LauncherSettingsController {
	@FXML
	protected ChoiceBox<Locale> locale;
	@FXML
	protected Label localeLabel;
	@FXML
	protected ChoiceBox<String> theme;
	@FXML
	protected Label themeLabel;

	@FXML
	public void initialize() {
		locale.setItems(FXCollections.observableList(LAUNCHER.getLocales().stream().toList()));
		locale.valueProperty().bindBidirectional(LAUNCHER.localeProperty());
		locale.setConverter(new StringConverter<>() {
			@Override
			public String toString(Locale object) {
				if(object == null) {
					return null;
				}
				return object.toLanguageTag();
			}

			@Override
			public Locale fromString(String string) {
				if(string == null) {
					return null;
				}
				return Locale.forLanguageTag(string);
			}
		});
		theme.setItems(FXCollections.observableList(new ArrayList<>(MainApplication.getThemeNames())));
		theme.valueProperty().bindBidirectional(LAUNCHER.themeProperty());
		LAUNCHER.disableSelfLocalization(locale);
		LAUNCHER.disableChildrenLocalization(locale);
		LAUNCHER.disableSelfLocalization(theme);
		LAUNCHER.disableChildrenLocalization(theme);
	}
}