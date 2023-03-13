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
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import tibetiroka.esmanager.instance.InstanceUtils.InstanceBuilder;
import tibetiroka.esmanager.instance.source.SourceType;
import tibetiroka.esmanager.ui.MainApplication;

public class NewInstanceController {
	public Stage stage;
	@FXML
	protected RadioButton continuous;
	@FXML
	protected RadioButton customRelease;
	@FXML
	protected RadioButton latestRelease;
	@FXML
	protected Button nextButton;
	@FXML
	protected RadioButton other;
	@FXML
	protected RadioButton pullRequest;
	@FXML
	protected ToggleGroup selectionGroup;

	@FXML
	protected void initialize() {
	}

	@FXML
	protected void next() {
		try {
			Scene scene = null;
			if(continuous.isSelected()) {
				FXMLLoader loader = new FXMLLoader(InstanceNameController.class.getResource("new-instance-name.fxml"));
				Parent p = loader.load();
				scene = new Scene(p);
				//
				((InstanceNameController) loader.getController()).stage = stage;
				((InstanceNameController) loader.getController()).builder = new InstanceBuilder().withOfficialSource(SourceType.RELEASE, "continuous");
			} else if(latestRelease.isSelected()) {
				FXMLLoader loader = new FXMLLoader(InstanceNameController.class.getResource("new-instance-name.fxml"));
				Parent p = loader.load();
				scene = new Scene(p);
				//
				((InstanceNameController) loader.getController()).stage = stage;
				((InstanceNameController) loader.getController()).builder = new InstanceBuilder().withOfficialSource(SourceType.LATEST_RELEASE, null);
			} else if(customRelease.isSelected()) {
				FXMLLoader loader = new FXMLLoader(ReleaseVersionChooserController.class.getResource("release-version-chooser.fxml"));
				Parent p = loader.load();
				scene = new Scene(p);
				//
				((ReleaseVersionChooserController) loader.getController()).stage = stage;
				((ReleaseVersionChooserController) loader.getController()).builder = new InstanceBuilder();
				((ReleaseVersionChooserController) loader.getController()).listReleases();
			} else if(pullRequest.isSelected()) {
				FXMLLoader loader = new FXMLLoader(PullRequestChooserController.class.getResource("pull-request-chooser.fxml"));
				Parent p = loader.load();
				scene = new Scene(p);
				//
				((PullRequestChooserController) loader.getController()).stage = stage;
				((PullRequestChooserController) loader.getController()).builder = new InstanceBuilder();
			} else if(other.isSelected()) {
				FXMLLoader loader = new FXMLLoader(OtherController.class.getResource("new-instance-other.fxml"));
				Parent p = loader.load();
				scene = new Scene(p);
				//
				((OtherController) loader.getController()).stage = stage;
			}
			MainApplication.switchScene(stage, scene);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}