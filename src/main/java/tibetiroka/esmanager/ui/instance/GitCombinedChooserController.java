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

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tibetiroka.esmanager.instance.InstanceUtils.InstanceBuilder;
import tibetiroka.esmanager.ui.MainApplication;

import java.util.ArrayList;

public class GitCombinedChooserController {
	private final ArrayList<CustomSourceController> controllers = new ArrayList<>();
	public InstanceBuilder builder;
	public Stage stage;
	public boolean stop = false;
	@FXML
	protected Label errorLabel;
	@FXML
	protected ScrollPane scrollPane;
	@FXML
	protected VBox sources;

	@FXML
	protected void addSource() {
		try {
			FXMLLoader loader = new FXMLLoader(CustomSourceController.class.getResource("custom-source.fxml"));
			Parent p = loader.load();
			sources.getChildren().add(p);
			controllers.add(loader.getController());
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@FXML
	protected void next() {
		try {
			boolean error = false;
			int count = 0;
			for(CustomSourceController controller : controllers) {
				if(!controller.isEmpty()) {
					count++;
					error |= !controller.isValid();
				}
			}
			errorLabel.setVisible(error);
			if(error || count == 0) {
				return;
			}
			builder = new InstanceBuilder();
			for(CustomSourceController controller : controllers) {
				if(!controller.isEmpty()) {
					controller.addToBuilder(builder);
				}
			}
			//
			if(stop) {
				stage.close();
			} else {
				FXMLLoader loader = new FXMLLoader(InstanceNameController.class.getResource("new-instance-name.fxml"));
				Parent p = loader.load();
				Scene scene = new Scene(p);
				//
				((InstanceNameController) loader.getController()).stage = stage;
				((InstanceNameController) loader.getController()).builder = builder;
				//
				MainApplication.switchScene(stage, scene);
			}
		} catch(Exception e) {
			errorLabel.setVisible(true);
		}
	}
}