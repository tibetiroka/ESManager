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
import org.eclipse.jgit.api.Git;
import tibetiroka.esmanager.instance.InstanceUtils.InstanceBuilder;
import tibetiroka.esmanager.ui.MainApplication;

import java.net.URL;

public class GitRepositoryChooserController {
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
			String repository = input.getText();
			URL url = new URL(repository);
			if(repository.isBlank()) {
				input.pseudoClassStateChanged(MainApplication.TEXT_ERROR_CLASS, true);
				return;
			}
			Git.lsRemoteRepository().setRemote(url.toURI().toString()).call();
			FXMLLoader loader = new FXMLLoader(InstanceNameController.class.getResource("release-version-chooser.fxml"));
			Parent p = loader.load();
			Scene scene = new Scene(p);
			//
			((ReleaseVersionChooserController) loader.getController()).stage = stage;
			((ReleaseVersionChooserController) loader.getController()).builder = new InstanceBuilder();
			((ReleaseVersionChooserController) loader.getController()).remoteURI = url.toURI().toString();
			((ReleaseVersionChooserController) loader.getController()).official = false;
			((ReleaseVersionChooserController) loader.getController()).listReleases();
			//
			MainApplication.switchScene(stage, scene);
		} catch(Exception e) {
			input.pseudoClassStateChanged(MainApplication.TEXT_ERROR_CLASS, true);
			errorLabel.setVisible(true);
		}
	}
}