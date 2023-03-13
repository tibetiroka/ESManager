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
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tibetiroka.esmanager.instance.InstanceUtils.InstanceBuilder;
import tibetiroka.esmanager.ui.MainApplication;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class DownloadChooserController {
	public Stage stage;
	@FXML
	protected Label errorLabel;
	@FXML
	protected TextField input;

	@FXML
	protected void initialize() {
	}

	@FXML
	protected void next() {
		try {
			String executable = input.getText();
			URL url = new URL(executable);
			if(executable.isBlank()) {
				input.pseudoClassStateChanged(MainApplication.TEXT_ERROR_CLASS, true);
				return;
			}
			URLConnection connection = url.openConnection();
			connection.connect();
			if(connection instanceof HttpURLConnection c) {
				if(c.getResponseCode() < 200 || c.getResponseCode() >= 300) {
					throw new IllegalArgumentException();
				}
			}
			FXMLLoader loader = new FXMLLoader(InstanceNameController.class.getResource("new-instance-name.fxml"));
			Parent p = loader.load();
			Scene scene = new Scene(p);
			//
			((InstanceNameController) loader.getController()).stage = stage;
			((InstanceNameController) loader.getController()).builder = new InstanceBuilder().withDownloadSource(url.toURI());
			//
			MainApplication.switchScene(stage, scene);
		} catch(Exception e) {
			input.pseudoClassStateChanged(MainApplication.TEXT_ERROR_CLASS, true);
			errorLabel.setVisible(true);
		}
	}
}