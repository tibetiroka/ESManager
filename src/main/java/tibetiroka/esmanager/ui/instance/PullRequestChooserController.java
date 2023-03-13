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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.instance.InstanceUtils.InstanceBuilder;
import tibetiroka.esmanager.instance.source.SourceType;
import tibetiroka.esmanager.ui.MainApplication;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class PullRequestChooserController {
	protected InstanceBuilder builder;
	@FXML
	protected Label errorLabel;
	@FXML
	protected Button nextButton;
	@FXML
	protected TextField pr;
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
		pr.setTextFormatter(new TextFormatter<String>(new StringConverter<>() {
			@Override
			public String toString(String object) {
				if(object == null) {
					return null;
				}
				return object.replaceAll("[^0-9]", "");
			}

			@Override
			public String fromString(String string) {
				return string;
			}
		}));
	}

	@FXML
	protected void next() {
		try {
			nextButton.setDisable(true);
			pr.setDisable(true);
			errorLabel.setVisible(false);
			String pr = this.pr.getText();
			if(pr != null && !pr.isBlank()) {
				new Thread(() -> {
					try {
						boolean present = Git.lsRemoteRepository()
						                     .setRemote(remoteURI)
						                     .setHeads(false)
						                     .setTags(false)
						                     .callAsMap()
						                     .containsKey("refs/pull/" + pr + "/head");
						if(present) {
							Platform.runLater(() -> {
								try {
									FXMLLoader loader = new FXMLLoader(InstanceNameController.class.getResource("new-instance-name.fxml"));
									Parent p = loader.load();
									Scene scene = new Scene(p);
									//
									((InstanceNameController) loader.getController()).stage = stage;
									((InstanceNameController) loader.getController()).builder = new InstanceBuilder().withRemoteGitSource(URI.create(remoteURI), SourceType.PULL_REQUEST, pr);
									//
									MainApplication.switchScene(stage, scene);
								} catch(Exception e) {
								}
							});
						} else {
							errorLabel.setVisible(true);
							this.pr.pseudoClassStateChanged(MainApplication.TEXT_ERROR_CLASS, true);
						}
					} catch(GitAPIException e) {
						throw new RuntimeException(e);
					} finally {
						nextButton.setDisable(false);
						this.pr.setDisable(false);
					}
				}, "PR availability checking thread for UI").start();
			} else {
				this.pr.pseudoClassStateChanged(MainApplication.TEXT_ERROR_CLASS, true);
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}