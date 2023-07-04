/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.utils;

import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * JavaFX utilities.
 *
 * @since 1.1.0
 */
public class FxUtils {
	/**
	 * Runs the specified runnable on the platform thread. If executed on the platform thread, the runnable is run before returning control. Any exceptions thrown in this case will propagate to the caller.
	 * <p>
	 * Please note that using this method instead of {@link Platform#runLater(Runnable)} may change semantics when the order of mixed platform and non-platform calls is important.
	 * </p>
	 *
	 * @param r The runnable to run
	 * @since 1.1.0
	 */
	public static void runOnPlatform(@NotNull Runnable r) {
		if(Platform.isFxApplicationThread()) {
			r.run();
		} else {
			Platform.runLater(r);
		}
	}
}