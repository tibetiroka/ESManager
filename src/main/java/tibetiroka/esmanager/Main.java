/*
 * Copyright (c) 2023 by tibetiroka.
 *
 * ESManager is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ESManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tibetiroka.esmanager;

import javafx.application.Application;
import javafx.css.PseudoClass;
import javafx.scene.text.Text;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.io.IoBuilder;
import org.jetbrains.annotations.NotNull;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.ui.MainApplication;
import tibetiroka.esmanager.ui.MainController;

import javax.swing.JOptionPane;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static tibetiroka.esmanager.config.Launcher.LAUNCHER;
import static tibetiroka.esmanager.config.Launcher.localize;

/**
 * Launches the application. Order of operations:
 * <ol>
 *     <li>Configures the logger. This is done during class loading.
 *         <ul>
 *             <li>Sets the necessary environment variables for the log directory.</li>
 *             <li>Makes sure the log directory exists.</li>
 *             <li>Creates its own logger.</li>
 *             <li>Redirects {@link System#out} and {@link System#err} to the logger.</li>
 *         </ul>
 *     </li>
 *     <li>Loads the launcher configuration.</li>
 *     <li>Configures the main thread.</li>
 *     <li>Launches the JavaFX Application ({@link MainApplication})</li>
 * </ol>
 */
public class Main {
	/**
	 * Stores whether there has been a severe error.
	 */
	public static final AtomicBoolean ERROR = new AtomicBoolean(false);
	private static final org.apache.logging.log4j.Logger log;

	static {
		configureLogger();
		log = LogManager.getLogger(Main.class);
		System.setOut(createLoggingProxy(Level.DEBUG));
		System.setErr(createLoggingProxy(Level.WARN));
	}

	/**
	 * Configures a thread to properly log any uncaught exceptions. Severe issues cause the application to hard crash with a message, after writing debug information into the log files.
	 *
	 * @param thread The thread to configure
	 * @param severe Whether the failure of this thread is a severe issue
	 * @see #panic(String)
	 */
	public static void configureThread(Thread thread, boolean severe) {
		thread.setUncaughtExceptionHandler((t, e) -> {
			log.error("Uncaught exception in thread (" + t.getName() + ")", e);
			if(severe) {
				panic(localize("log.generic.thread.error.panic", t.getName(), e.getMessage()));
			}
		});
	}

	public static void main(String[] args) {
		AppConfiguration.loadLauncherConfiguration();
		configureThread(Thread.currentThread(), true);
		Application.launch(MainApplication.class);
	}

	public static synchronized void panic(String reason) {
		ERROR.set(true);
		log.error(localize("log.generic.thread.error.panic.polite", reason));
		log.error(localize("log.generic.thread.error.panic.details", reason));
		printSystemDebugInfo();
		printThreadDump();
		JOptionPane.showMessageDialog(null, localize("log.generic.thread.error.panic.display", reason), localize("log.generic.thread.error.panic.display.title", reason), JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}

	/**
	 * Configures the global logger
	 *
	 * @since 0.1.0
	 */
	private static void configureLogger() {
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
		System.setProperty("esmanager.log.directory", AppConfiguration.LOG_HOME.getAbsolutePath());
		AppConfiguration.LOG_HOME.mkdirs();
	}

	/**
	 * Creates a new {@link PrintStream} from the specified stream that writes to the log output.
	 *
	 * @param level The level of the logged messages
	 * @return The new logging stream
	 * @since 0.1.0
	 */
	private static @NotNull PrintStream createLoggingProxy(final Level level) {
		return IoBuilder.forLogger(log)
		                .setLevel(level)
		                .buildPrintStream();
	}

	/**
	 * Prints debug information about the underlying OS, the java instance, and the runtime in general.
	 *
	 * @since 0.1.0
	 */
	private static void printSystemDebugInfo() {
		log.debug("\n=======DEBUG INFO=======\n");
		log.debug("\n---ENVIRONMENT INFO---");
		String[] properties = new String[]{"file.encoding", "java.class.path", "java.class.version", "java.library.path", "java.runtime.name", "java.runtime.version", "java.specification.name", "java.specification.vendor", "java.specification.version", "java.vendor", "java.vendor.url", "java.version", "java.version.date", "java.vm.compressedOopsMode", "java.vm.info", "java.vm.name", "java.vm.specification.name", "java.vm.specification.vendor", "java.vm.specification.version", "java.vm.vendor", "java.vm.version", "jdk.debug", "os.arch", "os.name", "os.version", "sun.arch.data.model", "sun.cpu.endian", "sun.cpu.isalist", "sun.io.unicode.encoding", "sun.java.command", "sun.java.launcher", "sun.jnu.encoding", "sun.management.compiler", "sun.stderr.encoding", "sun.stdout.encoding"};
		for(String property : properties) {
			log.debug(property + ": " + System.getProperty(property));
		}
		log.debug("\n---RUNTIME INFO---");
		log.debug("Available processors: " + Runtime.getRuntime().availableProcessors());
		log.debug("Free memory: " + Runtime.getRuntime().freeMemory());
		log.debug("Total memory: " + Runtime.getRuntime().totalMemory());
		log.debug("Maximum memory: " + Runtime.getRuntime().maxMemory());
		try {
			com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
			log.debug("\n---OS INFO---");
			log.debug("Committed virtual memory size: " + os.getCommittedVirtualMemorySize());
			log.debug("Free memory: " + os.getFreeMemorySize());
			log.debug("Free swap space: " + os.getFreeSwapSpaceSize());
			log.debug("Total memory size: " + os.getTotalMemorySize());
			log.debug("Total swap space: " + os.getTotalSwapSpaceSize());
		} catch(Exception e) {
			log.debug("Could not query further OS information (build version, available memory etc). Please make sure to include the exact version of your OS (with the specific build id) in the description of any bug report.");
		}
		log.debug("\n=======END OF DEBUG INFO=======\n");
	}

	/**
	 * Dumps the thread stacks to the output and the log.
	 *
	 * @since 0.1.0
	 */
	private static void printThreadDump() {
		log.info("\n---THREAD DUMP---");
		Thread.getAllStackTraces().forEach((thread, stackTrace) -> {
			if(thread != null && stackTrace.length > 0) {
				log.info("Dumping thread " + thread.getName());
				log.debug(stackTrace[0]);
				for(int i = 1; i < stackTrace.length; i++) {
					log.debug("\tat " + stackTrace[i]);
				}
				log.debug("");
			}
		});
		log.info("\n---END OF THREAD DUMP---");
	}

	/**
	 * Log appender used for the graphical interface. Uses {@link Text} elements with {@code log-text} class and {@code debug}, {@code info}, {@code warning} and {@code error} pseudo classes for styling.
	 */
	@Plugin(name = "DisplayAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
	public static final class DisplayAppender extends AbstractAppender {
		final HashMap<Level, PseudoClass> styling = new HashMap<>();

		{
			styling.put(Level.DEBUG, PseudoClass.getPseudoClass("debug"));
			styling.put(Level.INFO, PseudoClass.getPseudoClass("info"));
			styling.put(Level.WARN, PseudoClass.getPseudoClass("warning"));
			styling.put(Level.ERROR, PseudoClass.getPseudoClass("error"));
		}

		@Deprecated
		public DisplayAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
			super(name, filter, layout);
		}

		@Deprecated
		public DisplayAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
			super(name, filter, layout, ignoreExceptions);
		}

		public DisplayAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
			super(name, filter, layout, ignoreExceptions, properties);
		}

		@PluginFactory
		public static DisplayAppender createAppender(@PluginAttribute("name") String name, @PluginElement("Filter") Filter filter, @PluginElement("pattern") PatternLayout pattern) {
			return new DisplayAppender(name, filter, pattern);
		}

		@Override
		public void append(LogEvent event) {
			if(MainController.getController() != null && MainController.getController().getLogArea() != null) {
				String string = new String(getLayout().toByteArray(event));
				Text text = new Text(string);
				text.getStyleClass().add("log-text");
				if(styling.containsKey(event.getLevel())) {
					text.pseudoClassStateChanged(styling.get(event.getLevel()), true);
				}
				LAUNCHER.disableLocalization(text);
				MainController.getController().log(text);
			}
		}
	}
}