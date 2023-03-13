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
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import tibetiroka.esmanager.ui.MainApplication;

public class OtherController {
	public Stage stage;
	@FXML
	protected RadioButton download;
	@FXML
	protected RadioButton git;
	@FXML
	protected RadioButton local;
	@FXML
	protected RadioButton release;
	@FXML
	protected ToggleGroup selectionGroup;

	@FXML
	protected void next() {
		try {
			Scene scene = null;
			if(local.isSelected()) {
				FXMLLoader loader = new FXMLLoader(LocalChooserController.class.getResource("local-chooser.fxml"));
				Parent p = loader.load();
				scene = new Scene(p);
				//
				((LocalChooserController) loader.getController()).stage = stage;
			} else if(download.isSelected()) {
				FXMLLoader loader = new FXMLLoader(DownloadChooserController.class.getResource("download-chooser.fxml"));
				Parent p = loader.load();
				scene = new Scene(p);
				//
				((DownloadChooserController) loader.getController()).stage = stage;
			} else if(release.isSelected()) {
				FXMLLoader loader = new FXMLLoader(GitRepositoryChooserController.class.getResource("git-repository-chooser.fxml"));
				Parent p = loader.load();
				scene = new Scene(p);
				//
				((GitRepositoryChooserController) loader.getController()).stage = stage;
			} else if(git.isSelected()) {
				FXMLLoader loader = new FXMLLoader(GitCombinedChooserController.class.getResource("git-combined-chooser.fxml"));
				Parent p = loader.load();
				scene = new Scene(p);
				//
				((GitCombinedChooserController) loader.getController()).stage = stage;
			}
			MainApplication.switchScene(stage, scene);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}