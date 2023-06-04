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

import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.jetbrains.annotations.NotNull;
import tibetiroka.esmanager.instance.annotation.*;
import tibetiroka.esmanager.instance.source.Source;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;
import static tibetiroka.esmanager.config.Launcher.localize;

public class SourceEditorController {
	@FXML
	protected VBox left;
	@FXML
	protected VBox right;
	private Source source;

	@SuppressWarnings("unchecked")
	public <T extends Source> void initialize(@NotNull T source) throws Exception {
		if(!source.getClass().isAnnotationPresent(EditableSource.class)) {
			throw new IllegalArgumentException(localize("log.source.editor.annotation.fail", source.getName(), source.getVersion(), source.getClass()));
		}
		this.source = source;
		List<Field> fields = findFields((Class<Source>) source.getClass(), source);
		fields.sort((a, b) -> {
			if(!a.getType().equals(b.getType())) {
				if(a.getType().isAssignableFrom(b.getType())) {
					return 1;
				}
				return -1;
			}
			return a.getName().compareTo(b.getName());
		});
		for(Field field : fields) {
			field.setAccessible(true);
			addFieldDisplay(field, !field.isAnnotationPresent(NonEditable.class));
		}
	}

	@FXML
	public void initialize() {
		LAUNCHER.disableLocalization(left);
		LAUNCHER.disableLocalization(right);
	}

	@FXML
	protected void apply() throws IllegalAccessException {
		for(Node child : right.getChildren()) {
			if(child.isDisabled()) {
				continue;
			}
			Field f = (Field) child.getProperties().get("field");
			Editable e = f.getAnnotation(Editable.class);
			Validator v = e.value();
			Object o = null;
			if(child instanceof ChoiceBox<?> c) {
				o = c.getValue();
			} else if(child instanceof TextField t) {
				o = t.getText();
			} else if(child instanceof CheckBox b) {
				o = b.isSelected();
			} else if(child instanceof Button) {
				continue;
			} else if(child instanceof Spinner<?> s) {
				o = s.getValue();
			}
			if(v.isValid(o)) {
				o = v.convert(o);
				child.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), false);
			} else {
				child.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
			}
			if(Property.class.isAssignableFrom(f.getType())) {
				((Property) f.get(source)).setValue(o);
			} else {
				f.set(source, o);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void addFieldDisplay(Field field, boolean editable) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		Class<?> c = field.getType();
		Region node;
		if(c.equals(String.class)) {
			node = new TextField((String) field.get(source));
		} else if(c.equals(double.class)) {
			node = new Spinner<Double>(Double.MIN_VALUE, Double.MAX_VALUE, (Double) field.get(source));
			((Spinner<Double>) node).setValueFactory(new DoubleSpinnerValueFactory(Double.MIN_VALUE, Double.MAX_VALUE));
		} else if(c.equals(float.class)) {
			node = new Spinner<Float>(Float.MIN_VALUE, Float.MAX_VALUE, (Float) field.get(source));
			((Spinner<Double>) node).setValueFactory(new DoubleSpinnerValueFactory(Float.MIN_VALUE, Float.MAX_VALUE));
		} else if(c.equals(int.class)) {
			node = new Spinner<Integer>(Integer.MIN_VALUE, Integer.MAX_VALUE, (Integer) field.get(source));
			((Spinner<Integer>) node).setValueFactory(new IntegerSpinnerValueFactory(Integer.MIN_VALUE, Integer.MAX_VALUE));
		} else if(c.equals(long.class)) {
			node = new Spinner<Long>(Long.MIN_VALUE, Long.MAX_VALUE, (Long) field.get(source));
			((Spinner<Integer>) node).setValueFactory(new IntegerSpinnerValueFactory(Integer.MIN_VALUE, Integer.MAX_VALUE));
		} else if(c.equals(short.class)) {
			node = new Spinner<Short>(Short.MIN_VALUE, Short.MAX_VALUE, (Short) field.get(source));
			((Spinner<Integer>) node).setValueFactory(new IntegerSpinnerValueFactory(Short.MIN_VALUE, Short.MAX_VALUE));
		} else if(c.equals(byte.class)) {
			node = new Spinner<Byte>(Byte.MIN_VALUE, Byte.MAX_VALUE, (Byte) field.get(source));
			((Spinner<Integer>) node).setValueFactory(new IntegerSpinnerValueFactory(Byte.MIN_VALUE, Byte.MAX_VALUE));
		} else if(c.equals(boolean.class)) {
			node = new CheckBox();
			((CheckBox) node).setSelected((Boolean) field.get(source));
		} else if(Date.class.isAssignableFrom(c)) {
			node = new TextField(Objects.toString(field.get(source)));
		} else if(c.isEnum()) {
			node = new ChoiceBox<>();
			List list = Arrays.asList((Object[]) c.getMethod("values").invoke(null));
			((ChoiceBox<?>) node).setItems(FXCollections.observableList(list));
			((ChoiceBox) node).setValue(field.get(source));
		} else if(Collection.class.isAssignableFrom(c)) {
			node = new Button("source.editor.field.collection.edit");
			LAUNCHER.localizeNode(node, LAUNCHER.localeProperty());
		} else if(StringProperty.class.isAssignableFrom(c)) {
			node = new TextField(((StringProperty) field.get(source)).get());
		} else {
			throw new UnsupportedOperationException(localize("source.editor.field.type.unsupported", c));
		}
		node.setDisable(!editable);
		node.getProperties().put("field", field);
		Label l = new Label(field.getName());
		l.setPrefHeight(25);
		l.setMinHeight(25);
		l.setMaxHeight(25);
		node.setPrefHeight(25);
		node.setMinHeight(25);
		node.setMaxHeight(25);
		node.setMinWidth(Region.USE_PREF_SIZE);
		node.setPrefWidth(Region.USE_COMPUTED_SIZE);
		node.setMaxWidth(Double.MAX_VALUE);
		if(node instanceof TextField tf) {
			tf.setPrefWidth(computeTextWidth(tf.getFont(), tf.getText(), 0) + 25);
			l.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
				if(e.getButton() == MouseButton.SECONDARY) {
					Clipboard clipboard = Clipboard.getSystemClipboard();
					ClipboardContent content = new ClipboardContent();
					content.putString(tf.getText());
					clipboard.setContent(content);
				}
			});
		}
		left.getChildren().add(l);
		right.getChildren().add(node);
		//TODO: add support for editing lists and remove this check
		if(node instanceof Button b) {
			b.setDisable(true);
		}
	}

	private double computeTextWidth(Font font, String text, double wrappingWidth) {
		Text helper = new Text();
		helper.setFont(font);
		helper.setText(text);
		// Note that the wrapping width needs to be set to zero before
		// getting the text's real preferred width.
		helper.setWrappingWidth(0);
		helper.setLineSpacing(0);
		double w = Math.min(helper.prefWidth(-1), wrappingWidth);
		helper.setWrappingWidth((int) Math.ceil(w));
		return Math.ceil(helper.getLayoutBounds().getWidth());
	}

	@SuppressWarnings("unchecked")
	private <T extends Source, U extends T> List<Field> findFields(Class<T> c, U t) {
		ArrayList<Field> fields = new ArrayList<>();
		for(Field field : c.getDeclaredFields()) {
			if(Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if(field.isAnnotationPresent(NonEditable.class) || field.isAnnotationPresent(Editable.class) || field.isAnnotationPresent(EditableCollection.class)) {
				fields.add(field);
			}
		}
		if(!c.equals(Source.class)) {
			fields.addAll(findFields((Class<Source>) c.getSuperclass(), t));
		}
		return fields;
	}
}