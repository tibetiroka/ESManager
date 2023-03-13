/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.ui.instance;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import tibetiroka.esmanager.instance.Instance;
import tibetiroka.esmanager.instance.InstanceUtils;
import tibetiroka.esmanager.instance.InstanceUtils.InstanceBuilder;
import tibetiroka.esmanager.ui.MainApplication;
import tibetiroka.esmanager.ui.MainController;

public class InstanceNameController {
	private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(InstanceNameController.class);
	public InstanceBuilder builder;
	public Stage stage;
	@FXML
	protected Label errorLabel;
	@FXML
	protected Button finishButton;
	@FXML
	protected TextField name;

	@FXML
	public void finish() {
		String name = this.name.getText().trim();
		if(name.isEmpty()) {
			this.name.pseudoClassStateChanged(MainApplication.TEXT_ERROR_CLASS, true);
		} else {
			for(Instance instance : Instance.getInstances()) {
				if(instance.getName().equalsIgnoreCase(name)) {
					this.name.pseudoClassStateChanged(MainApplication.TEXT_ERROR_CLASS, true);
					errorLabel.setVisible(true);
					return;
				}
			}
			stage.close();
			new Thread(() -> {
				Platform.runLater(() -> MainController.getController().newInstanceButton.setDisable(true));
				try {
					InstanceUtils.create(builder.name(name));
				} catch(Exception e) {
					log.error("Could not crete new instance", e);
				} finally {
					Platform.runLater(() -> MainController.getController().newInstanceButton.setDisable(false));
				}
			}, "Instance creator thread for " + name).start();
		}
	}

	@FXML
	public void initialize() {
		errorLabel.setVisible(false);
		name.setTextFormatter(new TextFormatter<String>(new StringConverter<>() {
			@Override
			public String toString(String object) {
				if(object == null) {
					return null;
				}
				String sanitized = object.replaceAll("([/\\\\\\n^#$%&]|[^\\x20-\\x7D])", "");
				return sanitized.substring(0, Math.min(sanitized.length(), 50));
			}

			@Override
			public String fromString(String string) {
				return string;
			}
		}));
	}
}