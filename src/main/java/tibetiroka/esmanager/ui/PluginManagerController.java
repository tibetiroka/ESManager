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
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.util.StringConverter;
import org.controlsfx.control.CheckComboBox;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.config.Launcher;
import tibetiroka.esmanager.instance.Instance;
import tibetiroka.esmanager.plugin.LocalPlugin;

public class PluginManagerController {
	@FXML
	protected RadioButton all;
	@FXML
	protected CheckComboBox<Instance> chooser;
	protected LocalPlugin plugin;
	@FXML
	protected RadioButton specific;
	@FXML
	protected ToggleGroup toggleGroup;

	@FXML
	public void apply() {
		if(all.isSelected()) {
			plugin.enableForAll();
		} else {
			//make sure it doesn't stay in "enable for all" state
			if(!chooser.getItems().isEmpty()) {
				plugin.disableFor(chooser.getItems().get(0));
			}
			for(Instance item : chooser.getItems()) {
				if(chooser.getCheckModel().isChecked(item)) {
					plugin.enableFor(item);
				} else {
					plugin.disableFor(item);
				}
			}
		}
		AppConfiguration.savePluginConfiguration();
	}

	@FXML
	protected void initialize(LocalPlugin plugin) {
		this.plugin = plugin;
		Launcher.getLauncher().disableSelfLocalization(chooser);
		Launcher.getLauncher().disableChildrenLocalization(chooser);
		chooser.disableProperty().bind(specific.selectedProperty().map(b -> !b));
		chooser.setConverter(new StringConverter<>() {
			@Override
			public String toString(Instance object) {
				if(object == null) {
					return null;
				}
				return object.getPublicName();
			}

			@Override
			public Instance fromString(String string) {
				if(string == null) {
					return null;
				}
				for(Instance item : chooser.getItems()) {
					if(item.getPublicName().equals(string)) {
						return item;
					}
				}
				return null;
			}
		});
		chooser.getItems().setAll(Instance.getInstances());
		if(plugin.isEnabledForAll()) {
			all.setSelected(true);
			chooser.getCheckModel().checkAll();
		} else {
			specific.setSelected(true);
			for(Instance item : chooser.getItems()) {
				if(plugin.isEnabledFor(item)) {
					chooser.getCheckModel().check(item);
				} else {
					chooser.getCheckModel().clearCheck(item);
				}
			}
		}
	}
}