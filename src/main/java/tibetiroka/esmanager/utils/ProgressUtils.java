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

import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Utilities for progress trackers.
 *
 * @since 0.0.1
 */
public class ProgressUtils {
	/**
	 * Creates a task that updates its progress over time. The task's progress never reaches 1.
	 *
	 * @param tracker The tracker to use
	 * @return The fake task
	 */
	public static @NotNull FakeTask startFakeTimeTask(@NotNull UpdateProgressTracker tracker) {
		return new FakeTask(tracker) {
			private Timer timer;

			@Override
			public void end() {
				//timer.cancel();
				super.end();
			}

			@Override
			public void start(double share) {
				super.start(share);
				timer = new Timer();
				timer.scheduleAtFixedRate(new TimerTask() {
					final long offset = System.currentTimeMillis();
					long previous = offset;

					@Override
					public void run() {
						long current = System.currentTimeMillis();
						tracker.progressTask(calculateTimeProgressChange(previous, current - previous, offset));
						previous = current;
					}
				}, 50, 50);
			}
		};
	}

	/**
	 * Calculates how much progress has advanced based on the time elapsed.
	 *
	 * @param previous The number of milliseconds downloaded before the 'delta' is counted
	 * @param delta    The number of milliseconds since the last call
	 * @return The increase in the tracked progress
	 * @since 0.0.1
	 */
	private static double calculateTimeProgressChange(long previous, long delta, long offset) {
		if(delta == 0) {
			return 0;
		}
		double previousValue = 1. - 1. / Math.sqrt((previous - offset) / 900.); // 60 seconds -> 90%
		double currentValue = 1. - 1. / Math.sqrt((previous + delta - offset) / 900.);
		if(currentValue < 0 || currentValue < previousValue) {
			return 0;
		}
		if(previous == 0) {
			return Math.max(0, currentValue);
		}
		return Math.max(0, currentValue - previousValue);
	}

	/**
	 * A fake task that can update its progress arbitrarily. Do NOT create subtasks for these tasks.
	 *
	 * @since 0.0.1
	 */
	public static class FakeTask {
		/**
		 * The tracker on which this task operates
		 *
		 * @since 0.0.1
		 */
		private final @NotNull UpdateProgressTracker tracker;

		/**
		 * Creates a new task with the specified tracker. The task will not be visible to the tracker.
		 *
		 * @param tracker The tracker to use
		 * @since 0.0.1
		 */
		public FakeTask(@NotNull UpdateProgressTracker tracker) {
			this.tracker = tracker;
		}

		/**
		 * Ends the task on the tracker.
		 *
		 * @see UpdateProgressTracker#endTask()
		 * @since 0.0.1
		 */
		public void end() {
			tracker.endTask();
		}

		/**
		 * Begins the task on the tracker.
		 *
		 * @param share The share of the task
		 * @see UpdateProgressTracker#beginTask(double)
		 * @since 0.0.1
		 */
		public void start(double share) {
			tracker.beginTask(share);
		}
	}
}