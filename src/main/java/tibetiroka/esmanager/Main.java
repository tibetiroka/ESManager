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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.application.Application;
import javafx.css.PseudoClass;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import tibetiroka.esmanager.config.AppConfiguration;
import tibetiroka.esmanager.ui.MainApplication;
import tibetiroka.esmanager.ui.MainController;
import tibetiroka.esmanager.utils.LogUtils;

import javax.swing.JOptionPane;
import java.io.*;
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
 *
 * @since 0.0.1
 */
public class Main {
	/**
	 * Stores whether there has been a severe error.
	 *
	 * @since 0.0.1
	 */
	public static final AtomicBoolean ERROR = new AtomicBoolean(false);
	private static final Logger log;

	static {
		try {
			configureLogger();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		log = LoggerFactory.getLogger(Main.class);
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
		if(AppConfiguration.isNativeImage()) {
			log.warn("Running inside native image!");
		}
		Application.launch(MainApplication.class);
	}

	public static synchronized void panic(String reason) {
		ERROR.set(true);
		log.error(localize("log.generic.thread.error.panic.polite", reason));
		log.error(localize("log.generic.thread.error.panic.details", reason));
		printSystemDebugInfo();
		printThreadDump();
		Thread t = new Thread(() -> {
			try {
				Thread.sleep(15000);
			} catch(InterruptedException e) {
				throw new RuntimeException(e);
			}
			System.exit(0);
		}, "Backup Shutdown Thread");
		t.setDaemon(true);
		t.start();
		JOptionPane.showMessageDialog(null, localize("log.generic.thread.error.panic.display", reason), localize("log.generic.thread.error.panic.display.title", reason), JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}

	/**
	 * Configures the global logger
	 *
	 * @since 0.0.1
	 */
	private static void configureLogger() throws IOException {
		System.setProperty("esmanager.log.directory", AppConfiguration.LOG_HOME.getAbsolutePath());
		AppConfiguration.LOG_HOME.mkdirs();
		File file = new File(AppConfiguration.LOG_HOME, "latest.log");
		if(file.exists() && file.length() > 10 * 1000 * 1000) {
			// >10MB
			file.delete();
		}
		if(!AppConfiguration.isNativeImage()) {
			PipedInputStream outLog = new PipedInputStream();
			PipedOutputStream out = new PipedOutputStream(outLog);
			System.setOut(new PrintStream(out, true));
			PipedInputStream errLog = new PipedInputStream();
			PipedOutputStream err = new PipedOutputStream(errLog);
			System.setErr(new PrintStream(err, true));
			LogUtils.logAsync(outLog, Level.DEBUG);
			LogUtils.logAsync(errLog, Level.WARN);
		}
	}

	/**
	 * Prints debug information about the underlying OS, the java instance, and the runtime in general.
	 *
	 * @since 0.0.1
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
	 * @since 0.0.1
	 */
	private static void printThreadDump() {
		log.info("\n---THREAD DUMP---");
		Thread.getAllStackTraces().forEach((thread, stackTrace) -> {
			if(thread != null && stackTrace.length > 0) {
				log.info("Dumping thread " + thread.getName());
				log.debug(stackTrace[0].toString());
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
	 *
	 * @since 0.0.1
	 */
	public static final class DisplayAppender extends AppenderBase<ILoggingEvent> {
		private final HashMap<ch.qos.logback.classic.Level, PseudoClass> styling = new HashMap<>();
		private boolean hasMessage = false;

		{
			styling.put(ch.qos.logback.classic.Level.DEBUG, PseudoClass.getPseudoClass("debug"));
			styling.put(ch.qos.logback.classic.Level.INFO, PseudoClass.getPseudoClass("info"));
			styling.put(ch.qos.logback.classic.Level.WARN, PseudoClass.getPseudoClass("warning"));
			styling.put(ch.qos.logback.classic.Level.ERROR, PseudoClass.getPseudoClass("error"));
		}

		public DisplayAppender() {
			super();
		}

		@Override
		protected void append(ILoggingEvent event) {
			if(MainController.getController() != null && MainController.getController().getLogArea() != null) {
				Text text = new Text((hasMessage ? System.lineSeparator() : "") + event.getFormattedMessage().stripTrailing());
				text.getStyleClass().add("log-text");
				if(styling.containsKey(event.getLevel())) {
					text.pseudoClassStateChanged(styling.get(event.getLevel()), true);
				}
				LAUNCHER.disableLocalization(text);
				MainController.getController().log(text);
				hasMessage = true;
			}
		}
	}
}