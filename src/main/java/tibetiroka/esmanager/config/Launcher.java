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

import com.owlike.genson.annotation.JsonConverter;
import com.owlike.genson.annotation.JsonIgnore;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.kordamp.ikonli.javafx.FontIcon;
import tibetiroka.esmanager.config.GensonFactory.LocalePropertyConverter;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.*;

/**
 * Manages the configuration of the application, its theming and localization. This class acts as a singleton.
 *
 * @since 0.0.1
 */
public class Launcher {
	/**
	 * The default locale. A built-in resource bundle must exist for this locale, and it must provide entries for all used localization keys.
	 *
	 * @since 0.0.1
	 */
	private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag((String) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.locale"));
	/**
	 * The active {@link Launcher} instance. After the configuration for the launcher is loaded, this field is effectively final.
	 *
	 * @since 0.0.1
	 */
	public static Launcher LAUNCHER;
	/**
	 * The map of built-in resource bundles and their locales.
	 *
	 * @since 0.0.1
	 */
	@JsonIgnore
	private final transient HashMap<@NotNull Locale, @NotNull ResourceBundle> builtinBundles;
	/**
	 * The map of user-provided resource bundles and their locales.
	 *
	 * @since 0.0.1
	 */
	@JsonIgnore
	private final transient HashMap<@NotNull Locale, @NotNull ResourceBundle> customBundles;
	/**
	 * The set of objects that are localized. This contains any object for which {@link #localizeNode(Object, Property[])} was called.
	 *
	 * @since 0.0.1
	 */
	@JsonIgnore
	private final transient HashSet<Object> localizedNodes = new HashSet<>();
	/**
	 * Stores whether to automatically update the created instances. This value is stored in the configuration files.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private SimpleBooleanProperty autoUpdateInstances = new SimpleBooleanProperty((Boolean) AppConfiguration.DEFAULT_CONFIGURATION.get("instance.autoupdate"));
	/**
	 * Stores whether to automatically update this application. This value is stored in the configuration files.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private SimpleBooleanProperty autoUpdateLauncher = new SimpleBooleanProperty((Boolean) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.autoupdate"));
	/**
	 * Stores whether to launch instances in debug mode by default. This value is stored in the configuration files.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private SimpleBooleanProperty debugByDefault = new SimpleBooleanProperty((Boolean) AppConfiguration.DEFAULT_CONFIGURATION.get("instance.debug"));
	/**
	 * Stores the locale used in the launcher. This value is stored in the configuration files.
	 *
	 * @since 0.0.1
	 */
	@JsonConverter(LocalePropertyConverter.class)
	@NotNull
	private SimpleObjectProperty<@NotNull Locale> locale = new SimpleObjectProperty<>(DEFAULT_LOCALE);
	/**
	 * Stores the name of the theme file used in the launcher. This value is stored in the configuration files. The value of this property should never be null.
	 *
	 * @since 0.0.1
	 */
	@NotNull
	private SimpleStringProperty theme = new SimpleStringProperty((String) AppConfiguration.DEFAULT_CONFIGURATION.get("launcher.theme"));

	public Launcher() {
		LAUNCHER = this;
		//
		builtinBundles = getResourceBundles(Launcher.class.getPackageName() + ".locales.ui", Thread.currentThread().getContextClassLoader());
		try {
			File file = new File(AppConfiguration.DATA_HOME, "locales");
			if(file.exists()) {
				customBundles = getResourceBundles("ui", new URLClassLoader(new URL[]{file.toURI().toURL()}));
			} else {
				customBundles = new HashMap<>();
			}
		} catch(MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the {@link Launcher} instance.
	 *
	 * @return {@link #LAUNCHER}
	 * @since 0.0.1
	 */
	public static @NotNull Launcher getLauncher() {
		return LAUNCHER;
	}

	/**
	 * Creates a localized version of the specified string.
	 *
	 * @param name    The localization key
	 * @param objects The parameters used in {@link MessageFormat#format(String, Object...)}
	 * @return The localized message
	 * @since 0.0.1
	 */
	public static @NotNull String localize(@NotNull String name, @NotNull Object... objects) {
		return LAUNCHER.getText(name).format(objects);
	}

	/**
	 * Gets the resource bundles using the specified class loader.
	 *
	 * @param baseName The name of the bundle; bundle files are named baseName_language_TAG.properties
	 * @param loader   The loader that scans the possible paths
	 * @return The map of loaded bundles and their locales
	 */
	private static @NotNull HashMap<@NotNull Locale, @NotNull ResourceBundle> getResourceBundles(@NotNull String baseName, @NotNull ClassLoader loader) {
		HashMap<Locale, ResourceBundle> resourceBundles = new HashMap<>();
		for(Locale locale : Locale.getAvailableLocales()) {
			try {
				ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, loader);
				if(bundle.getLocale().equals(locale)) {
					resourceBundles.put(locale, bundle);
				}
			} catch(MissingResourceException ignored) {
			}
		}
		return resourceBundles;
	}

	/**
	 * Gets whether instances are automatically updated on startup.
	 *
	 * @return {@link #autoUpdateInstances}
	 * @since 0.0.1
	 */
	public @NotNull SimpleBooleanProperty AutoUpdateInstancesProperty() {
		return autoUpdateInstances;
	}

	/**
	 * Gets whether the launcher is automatically updated on startup.
	 *
	 * @return {@link #autoUpdateLauncher}
	 * @since 0.0.1
	 */
	public @NotNull SimpleBooleanProperty autoUpdateLauncherProperty() {
		return autoUpdateLauncher;
	}

	/**
	 * Gets whether instances are launched in debug mode by default.
	 *
	 * @return {@link #debugByDefault}
	 * @since 0.0.1
	 */
	public @NotNull SimpleBooleanProperty debugByDefaultProperty() {
		return debugByDefault;
	}

	/**
	 * Disables localization for all children of the specified node, while allowing the node itself to be localized.
	 *
	 * @param node The node
	 * @since 0.0.1
	 */
	public void disableChildrenLocalization(@NotNull Node node) {
		node.getProperties().put("localization.children", false);
	}

	/**
	 * Disables localization for the context menu of the specified node.
	 *
	 * @param node The node to disable context menu localization for
	 * @since 1.0.0
	 */
	public void disableContextMenuLocalization(@NotNull Node node) {
		node.getProperties().put("localization.contextmenu", false);
	}

	/**
	 * Disables all localization for the specified node, its children and its tooltip.
	 *
	 * @param node The node to disable localization for
	 * @since 0.0.1
	 */
	public void disableLocalization(@NotNull Node node) {
		disableChildrenLocalization(node);
		disableSelfLocalization(node);
		disableTooltipLocalization(node);
		disableContextMenuLocalization(node);
	}

	/**
	 * Disables localization for the specified node, while allowing its children to be localized.
	 *
	 * @param node The node to disable localization for
	 * @since 0.0.1
	 */
	public void disableSelfLocalization(@NotNull Node node) {
		node.getProperties().put("localization.self", false);
	}

	/**
	 * Disables localization for the tooltip of the specified node.
	 *
	 * @param node The node to disable tooltip localization for
	 * @since 0.0.1
	 */
	public void disableTooltipLocalization(@NotNull Node node) {
		node.getProperties().put("localization.tooltip", false);
	}

	/**
	 * Gets the set of all locales that have available bundles.
	 *
	 * @return The set of locales
	 * @since 0.0.1
	 */
	public @NotNull Set<@NotNull Locale> getLocales() {
		HashSet<Locale> locales = new HashSet<>();
		locales.addAll(builtinBundles.keySet());
		locales.addAll(customBundles.keySet());
		return locales;
	}

	/**
	 * Gets the locale used in the launcher.
	 *
	 * @return {@link #locale}
	 * @since 0.0.1
	 */
	public @NotNull SimpleObjectProperty<Locale> localeProperty() {
		return locale;
	}

	/**
	 * Localizes the specified object, and all of its current and future children. Individual nodes can disable the localization of themselves and their children. Some classes might have localization disabled by default.
	 * <p>
	 * <br>
	 * Uses the text, title and promptText values of the node as the localization key. No localization is performed on blank, empty or nonexistent properties.
	 * <br>
	 * If this component has a tooltip or a context menu, they are recursively passed to this method.
	 * <p>
	 * <br>
	 * A {@link ChangeListener} is registered on stages to automatically localize their future {@link Stage#sceneProperty() scenes}. Localized scenes localize their {@link Scene#getRoot() root}. {@link Parent Parents} localize all their current and future children.
	 * <p>
	 * <br>
	 * The specified properties are passed to every child in the node hierarchy.
	 *
	 * @param node      The node to localize
	 * @param suppliers The properties that provide the localization values in {@link MessageFormat#format(String, Object...)}. The text is updated when either the locale has changed, or any of these properties have updated.
	 * @since 0.0.1
	 */
	public void localizeNode(Object node, Property<?>... suppliers) {
		try {
			if(node == null || !localizedNodes.add(node)) {
				return;
			}
			boolean selfAllowed = !(node instanceof FontIcon);
			boolean childrenAllowed = !(node instanceof FontIcon || node instanceof ButtonBase || node instanceof Menu || node instanceof Label || node instanceof TextInputControl || node instanceof TextFlow);
			boolean tooltipAllowed = true;
			boolean contextMenuAllowed = true;
			if(node instanceof Node n) {
				Object self = n.getProperties().get("localization.self");
				Object children = n.getProperties().get("localization.children");
				Object tooltip = n.getProperties().get("localization.tooltip");
				Object contextMenu = n.getProperties().get("localization.contextmenu");
				if(self instanceof Boolean b) {
					selfAllowed = b;
				}
				if(children instanceof Boolean b) {
					childrenAllowed = b;
				}
				if(tooltip instanceof Boolean b) {
					tooltipAllowed = b;
				}
				if(contextMenu instanceof Boolean b) {
					contextMenuAllowed = b;
				}
			}
			if(selfAllowed) {
				String[] entries = new String[]{"text", "title", "promptText"};
				for(String entry : entries) {
					String method = "get" + entry.substring(0, 1).toUpperCase() + entry.substring(1);
					String property = entry + "Property";
					//
					Optional<Method> getter = Arrays.stream(node.getClass().getMethods()).filter(m -> m.getName().equals(method)).filter(m -> m.getParameterCount() == 0).findAny();
					if(getter.isPresent()) {
						String key = (String) getter.get().invoke(node);
						if(key != null && !key.isBlank()) {
							Property<?>[] dependencies = Arrays.copyOf(suppliers, suppliers.length + 1);
							dependencies[dependencies.length - 1] = LAUNCHER.locale;
							addBinding(node, Bindings.createStringBinding(() -> localize(key, Arrays.stream(suppliers).map(ObservableValue::getValue).toArray()), dependencies), property);
						}
					}
				}
			}
			if(selfAllowed) {
				if(node instanceof MenuButton button) {
					for(MenuItem item : button.getItems()) {
						localizeNode(item, suppliers);
					}
					button.getItems().addListener((ListChangeListener<MenuItem>) c -> {
						while(c.next()) {
							for(MenuItem node1 : c.getAddedSubList()) {
								localizeNode(node1, suppliers);
							}
						}
					});
				}
				if(node instanceof Menu menu) {
					for(MenuItem item : menu.getItems()) {
						localizeNode(item, suppliers);
					}
					menu.getItems().addListener((ListChangeListener<MenuItem>) c -> {
						while(c.next()) {
							for(MenuItem node1 : c.getAddedSubList()) {
								localizeNode(node1, suppliers);
							}
						}
					});
				}
				if(node instanceof ContextMenu menu) {
					for(MenuItem item : menu.getItems()) {
						localizeNode(item, suppliers);
					}
					menu.getItems().addListener((ListChangeListener<MenuItem>) c -> {
						while(c.next()) {
							for(MenuItem node1 : c.getAddedSubList()) {
								localizeNode(node1, suppliers);
							}
						}
					});
				}
			}
			if(tooltipAllowed) {
				Optional<Method> tooltip = Arrays.stream(node.getClass().getMethods()).filter(m -> m.getName().equals("getTooltip")).filter(m -> m.getParameterCount() == 0).findAny();
				if(tooltip.isPresent()) {
					Tooltip t = (Tooltip) tooltip.get().invoke(node);
					if(t != null) {
						localizeNode(t, suppliers);
					}
				}
			}
			if(contextMenuAllowed) {
				Optional<Method> contextMenu = Arrays.stream(node.getClass().getMethods()).filter(m -> m.getName().equals("getContextMenu")).filter(m -> m.getParameterCount() == 0).findAny();
				if(contextMenu.isPresent()) {
					ContextMenu menu = (ContextMenu) contextMenu.get().invoke(node);
					if(menu != null) {
						localizeNode(menu, suppliers);
					}
				} else {
					if(node instanceof Node n) {
						Object o = n.getUserData();
						if(o != null) {
							Optional<Field> f = Arrays.stream(o.getClass().getFields()).filter(field -> field.getName().equals("contextMenu")).findAny();
							if(f.isPresent()) {
								localizeNode(f.get().get(o), suppliers);
							}
						}
					}
				}
			}
			if(childrenAllowed) {
				if(node instanceof Stage stage) {
					localizeNode(stage.getScene(), suppliers);
					stage.sceneProperty().addListener((observable, oldValue, newValue) -> {
						if(newValue != null) {
							localizeNode(newValue, suppliers);
						}
					});
				}
				if(node instanceof Scene scene) {
					localizeNode(scene.getRoot(), suppliers);
				}
				if(node instanceof Parent parent) {
					for(Node node1 : parent.getChildrenUnmodifiable()) {
						localizeNode(node1, suppliers);
					}
					parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) c -> {
						while(c.next()) {
							for(Node node1 : c.getAddedSubList()) {
								localizeNode(node1, suppliers);
							}
						}
					});
				}
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the name of the theme file used in the launcher.
	 *
	 * @return {@link #theme}
	 * @since 0.0.1
	 */
	public @NotNull SimpleStringProperty themeProperty() {
		return theme;
	}

	/**
	 * Adds the string binding to the property of the node that can be retrieved using the specified method
	 *
	 * @param node    The node to get the binding of
	 * @param binding The binding to add
	 * @param method  The name of the getter for the property
	 * @since 0.0.1
	 */
	private void addBinding(@NotNull Object node, @NotNull StringBinding binding, @NotNull String method) {
		try {
			StringProperty property = (StringProperty) node.getClass().getMethod(method).invoke(node);
			property.bind(binding);
		} catch(Exception e) {
		}
	}

	/**
	 * Gets the value of the specified localization key. It is retrieved from the first of these bundles that have a matching entry:
	 * <ol>
	 *     <li>A custom bundle matching the current locale</li>
	 *     <li>A built-in bundle matching the current locale</li>
	 *     <li>The default bundle</li>
	 * </ol>
	 * The default bundle should provide an entry for every localization key.
	 *
	 * @param name The localization key
	 * @return The localized but unformatted message
	 */
	private @NotNull MessageFormat getText(@NotNull String name) {
		if(customBundles.containsKey(locale.get())) {
			if(customBundles.get(locale.get()).containsKey(name)) {
				return new MessageFormat(customBundles.get(locale.get()).getString(name));
			}
		}
		if(builtinBundles.containsKey(locale.get())) {
			if(builtinBundles.get(locale.get()).containsKey(name)) {
				return new MessageFormat(builtinBundles.get(locale.get()).getString(name));
			}
		}
		return new MessageFormat(builtinBundles.get(DEFAULT_LOCALE).getString(name));
	}
}