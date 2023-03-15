/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.instance;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * A simple task-based progress tracker. Each task can be split into other tasks. The sum of shares of these subtasks should not exceed 1.
 *
 * @since 0.0.1
 */
public class UpdateProgressTracker {
	/**
	 * Stores the progress values at which each task will end.
	 *
	 * @since 0.0.1
	 */
	private final @NotNull ArrayList<@NotNull Double> endings = new ArrayList<>();
	/**
	 * Stores whether the update has failed.
	 *
	 * @since 0.0.1
	 */
	private final @NotNull SimpleBooleanProperty failedUpdate = new SimpleBooleanProperty(false);
	/**
	 * Stores whether an update has been started at any point during this session.
	 *
	 * @since 0.0.1
	 */
	private final @NotNull SimpleBooleanProperty hasUpdated = new SimpleBooleanProperty(false);
	/**
	 * Stores whether the update is currently in progress.
	 *
	 * @since 0.0.1
	 */
	private final @NotNull SimpleBooleanProperty isWorking = new SimpleBooleanProperty(false);
	/**
	 * Stores the multiplier of each task; subtasks have the multipliers of the parents times their share.
	 *
	 * @since 0.0.1
	 */
	private final @NotNull ArrayList<@NotNull Double> multipliers = new ArrayList<>();
	/**
	 * Stores the current progress of the update.
	 *
	 * @since 0.0.1
	 */
	private final @NotNull SimpleDoubleProperty updateProgress = new SimpleDoubleProperty(-1);

	/**
	 * Begins a new task. It will use up the specified share of the parent. The parent is the current task (or nothing, if this is a root task).
	 *
	 * @param share The ratio of work done by this task compared to the parent
	 * @since 0.0.1
	 */
	public void beginTask(double share) {
		Platform.runLater(() -> {
			double current = updateProgress.get();
			if(current >= 0) {
				if(endings.isEmpty()) {
					endings.add(share + current);
					multipliers.add(share);
				} else {
					int index = endings.size() - 1;
					endings.add(Math.min(0.99, current + multipliers.get(index) * share));//todo: remove this and actually find where it's broken
					multipliers.add(multipliers.get(index) * share);
				}
			}
		});
	}

	/**
	 * Ends all tasks. Sets the progress to 1. Marks the update as done.
	 *
	 * @since 0.0.1
	 */
	public void endAll() {
		Platform.runLater(() -> {
			updateProgress.set(1);
			endings.clear();
			multipliers.clear();
			isWorking.set(false);
		});
	}

	/**
	 * Ends the current task.
	 *
	 * @since 0.0.1
	 */
	public void endTask() {
		Platform.runLater(() -> {
			if(updateProgress.get() >= 0) {
				updateProgress.set(endings.remove(endings.size() - 1));
				multipliers.remove(multipliers.size() - 1);
			}
		});
	}

	/**
	 * Gets whether the update has failed.
	 *
	 * @since 0.0.1
	 */
	public @NotNull SimpleBooleanProperty failedUpdateProperty() {
		return failedUpdate;
	}

	/**
	 * Gets whether an update has been started at any point during this session.
	 *
	 * @since 0.0.1
	 */
	public @NotNull SimpleBooleanProperty hasUpdatedProperty() {
		return hasUpdated;
	}

	/**
	 * Gets whether the update is currently in progress.
	 *
	 * @since 0.0.1
	 */
	public @NotNull SimpleBooleanProperty isWorkingProperty() {
		return isWorking;
	}

	/**
	 * Resets this tracker, making it ready for another update.
	 *
	 * @since 0.0.1
	 */
	public void reset() {
		Platform.runLater(() -> {
			updateProgress.set(0);
			endings.clear();
			multipliers.clear();
			failedUpdate.set(false);
			hasUpdated.set(false);
			isWorking.set(false);
		});
	}

	/**
	 * Sets whether the update has failed.
	 *
	 * @param value The new value
	 * @since 0.0.1
	 */
	public void setFailedUpdate(boolean value) {
		Platform.runLater(() -> failedUpdate.set(value));
	}

	/**
	 * Sets the progress of the update.
	 *
	 * @param value The new value
	 * @since 0.0.1
	 */
	public void setProgress(double value) {
		Platform.runLater(() -> updateProgress.set(value));
	}

	/**
	 * Sets whether the updater has been started.
	 *
	 * @param value The new value
	 * @since 0.0.1
	 */
	public void setUpdated(boolean value) {
		Platform.runLater(() -> hasUpdated.set(value));
	}

	/**
	 * Sets whether the update is in progress.
	 *
	 * @param value The new value
	 * @since 0.0.1
	 */
	public void setWorking(boolean value) {
		Platform.runLater(() -> isWorking.set(value));
	}

	/**
	 * Gets the current progress of the update.
	 *
	 * @since 0.0.1
	 */
	public @NotNull SimpleDoubleProperty updateProgressProperty() {
		return updateProgress;
	}
}