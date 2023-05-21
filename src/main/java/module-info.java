/**
 * The main module for ESManager.
 *
 * @author tibetiroka
 * @since 0.0.1
 */
module ESManager {
	requires javafx.media;
	requires javafx.swing;
	requires javafx.fxml;
	requires javafx.controls;
	requires javafx.base;
	requires javafx.graphics;
	requires genson;
	requires org.kordamp.ikonli.core;
	requires org.kordamp.ikonli.javafx;
	requires org.kordamp.ikonli.fontawesome5;
	requires org.eclipse.jgit;
	requires org.slf4j;
	requires ch.qos.logback.classic;
	requires ch.qos.logback.core;
	requires jdk.management;
	requires java.base;
	requires java.sql;
	requires org.apache.commons.codec;
	requires org.apache.commons.io;
	requires org.jetbrains.annotations;

	exports tibetiroka.esmanager.audio to genson;
	opens tibetiroka.esmanager.audio to genson;
	exports tibetiroka.esmanager.instance to genson;
	opens tibetiroka.esmanager.instance to genson;
	exports tibetiroka.esmanager.config to genson;
	opens tibetiroka.esmanager.config to genson;
	exports tibetiroka.esmanager.plugin to genson;
	opens tibetiroka.esmanager.plugin to genson;
	exports tibetiroka.esmanager.instance.source to genson;
	opens tibetiroka.esmanager.instance.source to genson;
	exports tibetiroka.esmanager.launcher to genson;
	opens tibetiroka.esmanager.launcher to genson;
	exports tibetiroka.esmanager.utils to genson;
	opens tibetiroka.esmanager.utils to genson;
	exports tibetiroka.esmanager to ch.qos.logback.classic, ch.qos.logback.core;
	exports tibetiroka.esmanager.ui;
	opens tibetiroka.esmanager.ui;
	exports tibetiroka.esmanager.ui.instance;
	opens tibetiroka.esmanager.ui.instance;
}