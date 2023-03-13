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
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.instance.InstanceUtils.InstanceBuilder;
import tibetiroka.esmanager.instance.ReleaseUtils;
import tibetiroka.esmanager.instance.source.SourceType;
import tibetiroka.esmanager.ui.MainApplication;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public class ReleaseVersionChooserController {
	protected InstanceBuilder builder;
	@FXML
	protected Button nextButton;
	protected boolean official = true;
	@FXML
	protected ChoiceBox<String> release;
	protected String remoteURI;
	protected Stage stage;

	{
		try {
			remoteURI = new URL((String) AppConfiguration.DEFAULT_CONFIGURATION.get("source.instance.remoteRepository")).toURI().toString();
		} catch(URISyntaxException | MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@FXML
	protected void initialize() {
		nextButton.setDisable(true);
		release.setDisable(true);
	}

	protected void listReleases() {
		new Thread(() -> {
			try {
				List<String> refs = Git.lsRemoteRepository()
				                       .setRemote(remoteURI)
				                       .setHeads(false)
				                       .setTags(true)
				                       .call()
				                       .stream()
				                       .filter(Objects::nonNull)
				                       .map(ref -> ref.getName())
				                       .sorted(ReleaseUtils.latestFirst())
				                       .map(s -> s.substring("refs/tags/".length()))
				                       .toList();
				Platform.runLater(() -> {
					release.setItems(FXCollections.observableList(refs));
					if(!release.getItems().isEmpty()) {
						release.setValue(release.getItems().get(0));
					}
					nextButton.setDisable(false);
					release.setDisable(false);
				});
			} catch(GitAPIException e) {
				throw new RuntimeException(e);
			}
		}, "Version listing thread for UI").start();
	}

	@FXML
	protected void next() {
		try {
			String release = this.release.getValue();
			if(release != null && !release.isBlank()) {
				FXMLLoader loader = new FXMLLoader(InstanceNameController.class.getResource("new-instance-name.fxml"));
				Parent p = loader.load();
				Scene scene = new Scene(p);
				//
				((InstanceNameController) loader.getController()).stage = stage;
				if(official) {
					((InstanceNameController) loader.getController()).builder = new InstanceBuilder().withOfficialSource(SourceType.RELEASE, release);
				} else {
					((InstanceNameController) loader.getController()).builder = new InstanceBuilder().withReleaseSource(SourceType.RELEASE, new URI(remoteURI), release);
				}
				//
				MainApplication.switchScene(stage, scene);
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}