/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.instance.annotation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * An enum for structures that can check the validity and create valid versions of supported types, used in the instance editor panel.
 *
 * @since 1.0.0
 */
public enum Validator {
	/**
	 * Accepts all values of any type, including null.
	 *
	 * @since 1.0.0
	 */
	VALID {
		@Override
		public <T> boolean isValid(@Nullable T t) {
			return true;
		}

		@Override
		public <T> @Nullable T convert(@Nullable T t) {
			return t;
		}
	},
	/**
	 * Accepts all non-blank string values.
	 *
	 * @since 1.0.0
	 */
	NOT_BLANK_STRING {
		@Override
		public <T> boolean isValid(@Nullable T t) {
			return t instanceof String s && !s.isBlank();
		}

		@Override
		public <T> @NotNull T convert(@Nullable T t) {
			return (T) ((String) t).trim();
		}
	},
	/**
	 * Accepts all non-null values.
	 *
	 * @since 1.0.0
	 */
	NON_NULL {
		@Override
		public <T> boolean isValid(@Nullable T t) {
			return t != null;
		}

		@Override
		public <T> @NotNull T convert(@Nullable T t) {
			return t;
		}
	},
	/**
	 * Accepts all strings that represent valid {@link Pattern patterns}.
	 *
	 * @since 1.0.0
	 */
	PATTERN {
		@Override
		public <T> boolean isValid(@Nullable T t) {
			if(t instanceof String s) {
				try {
					Pattern.compile(s);
					return true;
				} catch(Exception e) {
				}
			}
			return false;
		}

		@Override
		public <T> @NotNull String convert(@Nullable T t) {
			return (String) t;
		}
	},
	/**
	 * Accepts all strings that represent valid {@link URI URIs}.
	 *
	 * @since 1.0.0
	 */
	URI {
		@Override
		public <T> boolean isValid(@Nullable T t) {
			if(t instanceof String s) {
				try {
					java.net.URI.create(s);
					return true;
				} catch(Exception e) {
				}
			}
			return false;
		}

		@Override
		public <T> @NotNull String convert(@Nullable T t) {
			return (String) t;
		}
	};

	/**
	 * Converts a valid object into its internal representation. The object's validity must be verified before passing it to this method.
	 *
	 * @param t   The object to convert
	 * @param <T> The type of the object
	 * @return The converted object
	 * @since 1.0.0
	 */
	public abstract <T> @Nullable Object convert(@Nullable T t);

	/**
	 * Checks if the specified object is valid.
	 *
	 * @param t   The object to check
	 * @param <T> The type of the object
	 * @return True if valid
	 * @since 1.0.0
	 */
	public abstract <T> boolean isValid(@Nullable T t);
}