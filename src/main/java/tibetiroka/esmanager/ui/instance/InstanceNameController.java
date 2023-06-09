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
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tibetiroka.esmanager.Main;
import tibetiroka.esmanager.instance.Instance;
import tibetiroka.esmanager.instance.InstanceUtils;
import tibetiroka.esmanager.instance.InstanceUtils.InstanceBuilder;
import tibetiroka.esmanager.ui.MainApplication;
import tibetiroka.esmanager.ui.MainController;
import tibetiroka.esmanager.utils.Statistics.GlobalStatistics;

public class InstanceNameController {
	private static final Logger log = LoggerFactory.getLogger(InstanceNameController.class);
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
				if(instance.getPublicName().equalsIgnoreCase(name)) {
					this.name.pseudoClassStateChanged(MainApplication.TEXT_ERROR_CLASS, true);
					errorLabel.setVisible(true);
					return;
				}
			}
			stage.close();
			new Thread(() -> {
				Main.configureThread(Thread.currentThread(), false);
				Platform.runLater(() -> MainController.getController().newInstanceButton.setDisable(true));
				try {
					InstanceUtils.create(builder.name(name));
					GlobalStatistics.getGlobalStatistics().advanceInstanceCreationCounter();
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
	}
}