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

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.lib.Ref;
import tibetiroka.esmanager.instance.InstanceUtils.InstanceBuilder;
import tibetiroka.esmanager.instance.source.SourceType;
import tibetiroka.esmanager.ui.MainApplication;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CustomSourceController {
	@FXML
	protected TextField target;
	@FXML
	protected ChoiceBox<SourceType> type;
	@FXML
	protected TextField url;

	public InstanceBuilder addToBuilder(InstanceBuilder builder) throws MalformedURLException, URISyntaxException {
		return switch(type.getValue()) {
			case DYNAMIC_REFS -> builder.withDynamicRefSource(new URL(url.getText()).toURI(), Pattern.compile(target.getText()));
			default -> builder.withRemoteGitSource(new URL(url.getText()).toURI(), type.getValue(), target.getText());
		};
	}

	public boolean isEmpty() {
		return url.getText().isBlank() && target.getText().isBlank();
	}

	public boolean isValid() {
		boolean valid = verify();
		target.pseudoClassStateChanged(MainApplication.TEXT_ERROR_CLASS, !valid);
		url.pseudoClassStateChanged(MainApplication.TEXT_ERROR_CLASS, !valid);
		return valid;
	}

	@FXML
	protected void initialize() {
		type.setItems(FXCollections.observableList(List.of(SourceType.RELEASE, SourceType.LATEST_RELEASE, SourceType.BRANCH, SourceType.PULL_REQUEST, SourceType.COMMIT, SourceType.DYNAMIC_REFS)));
		type.setConverter(new StringConverter<>() {
			@Override
			public String toString(SourceType object) {
				if(object == null) {
					return null;
				}
				return object.name().replace('_', ' ').toLowerCase();
			}

			@Override
			public SourceType fromString(String string) {
				if(string == null) {
					return null;
				}
				return SourceType.valueOf(string.replace(' ', '_').toUpperCase());
			}
		});
		type.setValue(type.getItems().get(0));
	}

	private boolean verify() {
		try {
			if(url.getText().isBlank()) {
				return false;
			}
			return switch(type.getValue()) {
				case COMMIT -> true;
				case DYNAMIC_REFS -> {
					try {
						Pattern.compile(target.getText());
						yield true;
					} catch(Exception e) {
						yield false;
					}
				}
				default -> {
					String remoteURI = new URL(url.getText()).toURI().toString();
					LsRemoteCommand command = Git.lsRemoteRepository().setRemote(remoteURI);
					switch(type.getValue()) {
						case LATEST_RELEASE, RELEASE -> command.setHeads(false).setTags(true);
						case BRANCH -> command.setHeads(true).setTags(false);
						case PULL_REQUEST -> command.setTags(false).setHeads(false);
					}
					Map<String, Ref> refs = command.callAsMap();
					yield switch(type.getValue()) {
						case LATEST_RELEASE -> !refs.isEmpty();
						case RELEASE -> refs.containsKey("refs/tags/" + target.getText());
						case PULL_REQUEST -> refs.containsKey("refs/pull/" + target.getText() + "/head");
						case BRANCH -> refs.containsKey("refs/heads/" + target.getText());
						default -> false;
					};
				}
			};
		} catch(Exception e) {
			return false;
		}
	}
}