/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager.config;

import com.owlike.genson.*;
import com.owlike.genson.reflect.TypeUtil;
import com.owlike.genson.reflect.VisibilityFilter;
import com.owlike.genson.stream.ObjectReader;
import com.owlike.genson.stream.ObjectWriter;
import javafx.beans.property.*;
import org.eclipse.jgit.merge.ContentMergeStrategy;
import org.eclipse.jgit.merge.MergeStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tibetiroka.esmanager.instance.BuildHelper.BuildSystem;
import tibetiroka.esmanager.instance.source.Source;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Locale;
import java.util.UUID;

/**
 * Utility class for creating a customized instance of {@link Genson}
 *
 * @since 0.0.1
 */
public class GensonFactory {
	/**
	 * Globally available {@link Genson} instance.
	 *
	 * @since 0.0.1
	 */
	public static Genson GENSON = createGenson();

	/**
	 * Creates a fully customized {@link Genson} instance.
	 *
	 * @return a new instance
	 * @since 0.0.1
	 */
	public static @NotNull Genson createGenson() {
		return sourcelessGensonBuilder()
				.withConverterFactory(new SourceConverterFactory())
				.create();
	}

	/**
	 * Creates a builder for a mostly customized {@link Genson}, but without the {@link SourceConverterFactory}.
	 *
	 * @return A {@link GensonBuilder} without custom source serialization
	 * @since 0.0.1
	 */
	private static @Nullable GensonBuilder sourcelessGensonBuilder() {
		return new GensonBuilder()
				.setSkipNull(true)
				.useMethods(false)
				.useFields(true, new VisibilityFilter(Modifier.TRANSIENT, Modifier.STATIC, Modifier.FINAL))
				.useMetadata(true)
				.useClassMetadata(false)
				.useClassMetadataWithStaticType(false)
				.useRuntimeType(true)
				.useIndentation(true)
				.withConverter(new Converter<>() {
					@Override
					public void serialize(UUID object, ObjectWriter writer, Context ctx) throws Exception {
						writer.writeString(object.toString());
					}

					@Override
					public UUID deserialize(ObjectReader reader, Context ctx) throws Exception {
						return UUID.fromString(reader.valueAsString());
					}
				}, UUID.class)
				.withConverter(new Converter<>() {
					@Override
					public void serialize(URL object, ObjectWriter writer, Context ctx) throws Exception {
						writer.writeString(object.toExternalForm());
					}

					@Override
					public URL deserialize(ObjectReader reader, Context ctx) throws Exception {
						return new URL(reader.valueAsString());
					}
				}, URL.class)
				.withConverter(new Converter<>() {
					@Override
					public void serialize(File object, ObjectWriter writer, Context ctx) throws Exception {
						writer.writeString(object.getAbsolutePath());
					}

					@Override
					public File deserialize(ObjectReader reader, Context ctx) throws Exception {
						return new File(reader.valueAsString());
					}
				}, File.class)
				.withConverter(new Converter<>() {
					@Override
					public void serialize(Locale object, ObjectWriter writer, Context ctx) throws Exception {
						writer.writeString(object.toLanguageTag());
					}

					@Override
					public Locale deserialize(ObjectReader reader, Context ctx) throws Exception {
						return Locale.forLanguageTag(reader.valueAsString());
					}
				}, Locale.class)
				.withConverterFactory(new MergeStrategyConverterFactory())
				.withConverterFactory(new PropertyConverterFactory());
	}

	/**
	 * Converts a {@link SimpleObjectProperty}&lt;{@link BuildSystem}&gt; to {@link BuildSystem}.
	 *
	 * @since 0.0.1
	 */
	public static class BuildSystemPropertyConverter implements Converter<SimpleObjectProperty<?>> {
		public BuildSystemPropertyConverter() {
		}

		@Override
		public void serialize(SimpleObjectProperty<?> object, ObjectWriter writer, Context ctx) throws Exception {
			GENSON.provideConverter(BuildSystem.class).serialize(object.get(), writer, ctx);
		}

		@Override
		public SimpleObjectProperty<BuildSystem> deserialize(ObjectReader reader, Context ctx) throws Exception {
			return new SimpleObjectProperty<>((BuildSystem) GENSON.provideConverter(BuildSystem.class).deserialize(reader, ctx));
		}
	}

	/**
	 * Converts a {@link SimpleObjectProperty}&lt;{@link ContentMergeStrategy}&gt; to {@link ContentMergeStrategy}.
	 *
	 * @since 0.0.1
	 */
	public static class ContentMergeStrategyPropertyConverter implements Converter<SimpleObjectProperty<?>> {
		public ContentMergeStrategyPropertyConverter() {
		}

		@Override
		public void serialize(SimpleObjectProperty<?> object, ObjectWriter writer, Context ctx) throws Exception {
			GENSON.provideConverter(ContentMergeStrategy.class).serialize(object.get(), writer, ctx);
		}

		@Override
		public SimpleObjectProperty<ContentMergeStrategy> deserialize(ObjectReader reader, Context ctx) throws Exception {
			return new SimpleObjectProperty<>((ContentMergeStrategy) GENSON.provideConverter(ContentMergeStrategy.class).deserialize(reader, ctx));
		}
	}

	/**
	 * Converts a {@link SimpleObjectProperty}&lt;{@link Locale}&gt; to {@link Locale}.
	 *
	 * @since 0.0.1
	 */
	public static class LocalePropertyConverter implements Converter<SimpleObjectProperty<?>> {
		public LocalePropertyConverter() {
		}

		@Override
		public void serialize(SimpleObjectProperty<?> object, ObjectWriter writer, Context ctx) throws Exception {
			GENSON.provideConverter(Locale.class).serialize(object.get(), writer, ctx);
		}

		@Override
		public SimpleObjectProperty<Locale> deserialize(ObjectReader reader, Context ctx) throws Exception {
			return new SimpleObjectProperty<>((Locale) GENSON.provideConverter(Locale.class).deserialize(reader, ctx));
		}
	}

	/**
	 * Converts a {@link SimpleObjectProperty}&lt;{@link MergeStrategy}&gt; to {@link MergeStrategy}.
	 *
	 * @since 0.0.1
	 */
	public static class MergeStrategyPropertyConverter implements Converter<SimpleObjectProperty<?>> {
		public MergeStrategyPropertyConverter() {
		}

		@Override
		public void serialize(SimpleObjectProperty<?> object, ObjectWriter writer, Context ctx) throws Exception {
			GENSON.provideConverter(MergeStrategy.class).serialize(object.get(), writer, ctx);
		}

		@Override
		public SimpleObjectProperty<MergeStrategy> deserialize(ObjectReader reader, Context ctx) throws Exception {
			return new SimpleObjectProperty<>((MergeStrategy) GENSON.provideConverter(MergeStrategy.class).deserialize(reader, ctx));
		}
	}

	/**
	 * Converts a {@link ContentMergeStrategy} to {@link String}.
	 *
	 * @since 0.0.1
	 */
	private static class MergeStrategyConverterFactory implements Factory<Converter<MergeStrategy>> {
		@Override
		public Converter<MergeStrategy> create(Type type, Genson genson) {
			final Class<?> c = TypeUtil.getRawClass(type);
			if(MergeStrategy.class.isAssignableFrom(c)) {
				return new Converter<>() {
					@Override
					public void serialize(MergeStrategy object, ObjectWriter writer, Context ctx) throws Exception {
						for(Field field : MergeStrategy.class.getFields()) {
							if(MergeStrategy.class.isAssignableFrom(field.getType())) {
								if(object == field.get(null)) {
									writer.writeValue(field.getName());
									return;
								}
							}
						}
						writer.writeNull();
					}

					@Override
					public MergeStrategy deserialize(ObjectReader reader, Context ctx) throws Exception {
						String name = reader.valueAsString();
						for(Field field : MergeStrategy.class.getFields()) {
							if(MergeStrategy.class.isAssignableFrom(field.getType())) {
								if(name.equalsIgnoreCase(field.getName())) {
									return (MergeStrategy) field.get(null);
								}
							}
						}
						return null;
					}
				};
			}
			return null;
		}
	}

	/**
	 * Converts a {@link Property} to its stored type.
	 *
	 * @since 0.0.1
	 */
	private static class PropertyConverterFactory implements Factory<Converter<Property>> {
		@Override
		public Converter<Property> create(Type type, Genson genson) {
			final Class<?> c = TypeUtil.getRawClass(type);
			if(Property.class.isAssignableFrom(c)) {
				return new Converter<>() {
					@Override
					public void serialize(Property object, ObjectWriter writer, Context ctx) throws Exception {
						String name = object.getClass().getName();
						//package.SimpleTypeProperty
						if(!name.startsWith(SimpleStringProperty.class.getPackageName() + ".") || !name.endsWith("Property")) {
							throw new IllegalArgumentException("Only simple properties are supported by this converter!");
						}
						name = name.substring((SimpleStringProperty.class.getPackageName() + ".").length());
						//SimpleTypeProperty
						name = name.substring("Simple".length());
						//TypeProperty
						name = name.substring(0, name.length() - "Property".length());
						//Type
						Class serializedClass;
						try {
							//checking primitive type
							serializedClass = (Class) Class.forName("java.lang." + name).getField("TYPE").get(null);
						} catch(Exception ignored) {
							//checking java.lang.* type
							serializedClass = Class.forName(String.class.getPackageName() + "." + name);
						}
						//
						if(serializedClass.isPrimitive() || serializedClass.equals(String.class)) {//json primitives
							genson.provideConverter(serializedClass).serialize(object.getValue(), writer, ctx);
						} else {
							writer.beginObject();
							writer.writeMetadata("propertyType", serializedClass.getSimpleName());
							if(serializedClass.equals(Object.class)) {
								writer.writeMetadata("valueType", object.getValue().getClass().getName());
							}
							writer.writeName("value");
							genson.provideConverter(serializedClass).serialize(object.getValue(), writer, ctx);
							writer.endObject();
						}
						//
					}

					@Override
					public Property deserialize(ObjectReader reader, Context ctx) throws Exception {
						if(ObjectProperty.class.isAssignableFrom(c)) {
							return deserializeObject(reader, ctx);
						}
						return switch(reader.getValueType()) {
							case NULL -> throw new IllegalArgumentException("Property value cannot be null");
							case ARRAY -> throw new IllegalArgumentException("Property value cannot be an array");
							case OBJECT -> deserializeObject(reader, ctx);
							case DOUBLE -> {
								if(FloatProperty.class.isAssignableFrom(c)) {//explicitly float, not double
									yield new SimpleFloatProperty(reader.valueAsFloat());
								}
								yield new SimpleDoubleProperty(reader.valueAsDouble());
							}
							case INTEGER -> {
								if(IntegerProperty.class.isAssignableFrom(c)) {//explicitly int, not long
									yield new SimpleIntegerProperty(reader.valueAsInt());
								}
								yield new SimpleLongProperty(reader.valueAsLong());
							}
							case BOOLEAN -> new SimpleBooleanProperty(reader.valueAsBoolean());
							case STRING -> new SimpleStringProperty(reader.valueAsString());
						};
					}

					private Property deserializeObject(ObjectReader reader, Context ctx) throws Exception {
						return switch(reader.getValueType()) {
							case OBJECT -> {
								reader.nextObjectMetadata();
								String propertyType = reader.metadata("propertyType");
								Class<Property> p = (Class<Property>) Class.forName(SimpleStringProperty.class.getPackageName() + ".Simple" + propertyType + "Property");
								Method getter = p.getMethod("getValue");
								Type valueType;
								if(reader.metadata("valueType") != null) {
									valueType = Class.forName(reader.metadata("valueType"));
								} else {
									valueType = getter.getGenericReturnType();
								}
								reader.beginObject();
								while(reader.hasNext()) {
									reader.next();
									if(reader.name().equals("value")) {
										Property property = p.getConstructor().newInstance();
										property.setValue(genson.provideConverter(valueType).deserialize(reader, ctx));
										yield property;
									}
								}
								yield null;
							}
							case STRING -> new SimpleObjectProperty(reader.valueAsString());
							case BOOLEAN -> new SimpleObjectProperty(reader.valueAsBoolean());
							case INTEGER -> new SimpleObjectProperty(reader.valueAsLong());
							case DOUBLE -> new SimpleObjectProperty(reader.valueAsDouble());
							case NULL -> new SimpleObjectProperty(null);
							case ARRAY -> new SimpleObjectProperty(genson.provideConverter(Object[].class).deserialize(reader, ctx));
						};
					}
				};
			}
			return null;
		}
	}

	/**
	 * A special converter factory for sources. They use a custom class annotation scheme to support more drastic backend changes.
	 */
	private static class SourceConverterFactory implements Factory<Converter<Source>> {
		@Override
		public Converter<Source> create(Type type, Genson genson) {
			final Class<?> c = TypeUtil.getRawClass(type);
			if(Source.class.isAssignableFrom(c)) {
				return new Converter<>() {
					@Override
					public void serialize(Source object, ObjectWriter writer, Context ctx) throws Exception {
						writer.beginNextObjectMetadata();
						writer.writeMetadata("sourceSubclass", object.getClass().getSimpleName());
						sourcelessGensonBuilder().create().provideConverter(object.getClass()).serialize(object, writer, ctx);
					}

					@Override
					public Source deserialize(ObjectReader reader, Context ctx) throws Exception {
						String meta = reader.nextObjectMetadata().metadata("sourceSubclass");
						Class<?> c = Class.forName(Source.class.getPackageName() + "." + meta);
						if(!Source.class.isAssignableFrom(c)) {
							throw new ClassCastException("Invalid source class: " + c.getName());
						}
						return (Source) sourcelessGensonBuilder().create().provideConverter(c).deserialize(reader, ctx);
					}
				};
			}
			return null;
		}
	}
}