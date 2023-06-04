/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.util.StringConverter;
import org.eclipse.jgit.merge.ContentMergeStrategy;
import org.eclipse.jgit.merge.MergeStrategy;
import tibetiroka.esmanager.config.GensonFactory;
import tibetiroka.esmanager.instance.GitSettings;

import java.util.Arrays;
import java.util.List;

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;

public class GitSettingsController {
	private static GitSettingsController CONTROLLER;
	@FXML
	protected ChoiceBox<ContentMergeStrategy> contentMergeStrategy;
	@FXML
	protected Label contentMergeStrategyLabel;
	@FXML
	protected ChoiceBox<MergeStrategy> mergeStrategy;
	@FXML
	protected Label mergeStrategyLabel;

	public static void bind() {
		CONTROLLER.mergeStrategy.valueProperty().bindBidirectional(GitSettings.getSettings().mergeStrategyProperty());
		CONTROLLER.contentMergeStrategy.valueProperty().bindBidirectional(GitSettings.getSettings().contentMergeStrategyProperty());
	}

	@FXML
	protected void initialize() {
		CONTROLLER = this;
		mergeStrategy.setConverter(new StringConverter<>() {
			@Override
			public String toString(MergeStrategy object) {
				if(object == null) {
					return null;
				}
				return GensonFactory.createGenson().serialize(object).replace("\"", "");
			}

			@Override
			public MergeStrategy fromString(String string) {
				if(string == null) {
					return null;
				}
				return GensonFactory.createGenson().deserialize('"' + string + '"', MergeStrategy.class);
			}
		});
		mergeStrategy.setItems(FXCollections.observableList(List.of(MergeStrategy.RESOLVE, MergeStrategy.RECURSIVE, MergeStrategy.OURS, MergeStrategy.SIMPLE_TWO_WAY_IN_CORE, MergeStrategy.THEIRS)));
		contentMergeStrategy.setItems(FXCollections.observableList(Arrays.stream(ContentMergeStrategy.values()).toList()));
		LAUNCHER.disableSelfLocalization(mergeStrategy);
		LAUNCHER.disableChildrenLocalization(mergeStrategy);
		LAUNCHER.disableSelfLocalization(contentMergeStrategy);
		LAUNCHER.disableChildrenLocalization(contentMergeStrategy);
	}
}