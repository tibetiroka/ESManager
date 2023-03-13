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

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tibetiroka.esmanager.config.Launcher;
import tibetiroka.esmanager.instance.InstanceUtils.InstanceBuilder;
import tibetiroka.esmanager.ui.MainApplication;

import java.io.File;
import java.nio.file.Files;

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;

public class LocalChooserController {
	public Stage stage;
	@FXML
	protected Label errorLabel;
	@FXML
	protected TextField input;

	@FXML
	protected void choose() {
		FileChooser chooser = new FileChooser();
		chooser.titleProperty().bind(Bindings.createStringBinding(() -> Launcher.localize("instance.add.local.chooser.title"), LAUNCHER.localeProperty()));
		chooser.setInitialDirectory(new File(System.getProperty("user.home")));
		File file = chooser.showOpenDialog(stage);
		if(file != null) {
			input.setText(file.getAbsolutePath());
		}
	}

	@FXML
	protected void initialize() {
	}

	@FXML
	protected void next() {
		try {
			String executable = input.getText();
			File file = new File(executable);
			if(executable.isBlank()) {
				input.pseudoClassStateChanged(MainApplication.TEXT_ERROR_CLASS, true);
			} else if(!file.exists() || file.isDirectory() || !Files.isExecutable(file.toPath())) {
				errorLabel.setVisible(true);
				input.pseudoClassStateChanged(MainApplication.TEXT_ERROR_CLASS, true);
			} else {
				FXMLLoader loader = new FXMLLoader(InstanceNameController.class.getResource("new-instance-name.fxml"));
				Parent p = loader.load();
				Scene scene = new Scene(p);
				//
				((InstanceNameController) loader.getController()).stage = stage;
				((InstanceNameController) loader.getController()).builder = new InstanceBuilder().withLocalFileSource(file);
				//
				MainApplication.switchScene(stage, scene);
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}