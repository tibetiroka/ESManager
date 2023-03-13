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

import com.owlike.genson.annotation.JsonConverter;
import javafx.beans.property.SimpleObjectProperty;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.merge.ContentMergeStrategy;
import org.eclipse.jgit.merge.MergeStrategy;
import org.jetbrains.annotations.NotNull;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.config.GensonFactory;
import tibetiroka.esmanager.config.GensonFactory.ContentMergeStrategyPropertyConverter;
import tibetiroka.esmanager.config.GensonFactory.MergeStrategyPropertyConverter;

/**
 * Utility class for managing git settings. Acts as a singleton.
 *
 * @since 0.0.1
 */
public class GitSettings {
	/**
	 * The active settings instance.
	 *
	 * @since 0.0.1
	 */
	public static @NotNull GitSettings SETTINGS;
	/**
	 * The preferred content merging strategy.
	 *
	 * @see MergeCommand#setContentMergeStrategy(ContentMergeStrategy)
	 * @since 0.0.1
	 */
	@JsonConverter(ContentMergeStrategyPropertyConverter.class)
	private @NotNull SimpleObjectProperty<@NotNull ContentMergeStrategy> contentMergeStrategy = new SimpleObjectProperty<>(ContentMergeStrategy.valueOf(((String) AppConfiguration.DEFAULT_CONFIGURATION.get("git.merge.content.strategy")).toUpperCase()));
	/**
	 * The preferred merging strategy.
	 *
	 * @see MergeCommand#setStrategy(MergeStrategy)
	 * @since 0.0.1
	 */
	@JsonConverter(MergeStrategyPropertyConverter.class)
	private @NotNull SimpleObjectProperty<@NotNull MergeStrategy> mergeStrategy = new SimpleObjectProperty<>(GensonFactory.createGenson().deserialize((String) AppConfiguration.DEFAULT_CONFIGURATION.get("git.merge.strategy"), MergeStrategy.class));

	public GitSettings() {
		SETTINGS = this;
	}

	/**
	 * Gets the active settings instance.
	 *
	 * @return {@link #SETTINGS}
	 * @since 0.0.1
	 */
	public static @NotNull GitSettings getSettings() {
		return SETTINGS;
	}

	/**
	 * Gets the preferred content merging strategy.
	 *
	 * @return {@link #contentMergeStrategy}
	 * @see MergeCommand#setContentMergeStrategy(ContentMergeStrategy)
	 * @since 0.0.1
	 */
	public @NotNull SimpleObjectProperty<@NotNull ContentMergeStrategy> contentMergeStrategyProperty() {
		return contentMergeStrategy;
	}

	/**
	 * Gets the preferred merging strategy.
	 *
	 * @return {@link #mergeStrategy}
	 * @see MergeCommand#setStrategy(MergeStrategy)
	 * @since 0.0.1
	 */
	public @NotNull SimpleObjectProperty<@NotNull MergeStrategy> mergeStrategyProperty() {
		return mergeStrategy;
	}
}