/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.common.logging;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.axelor.common.FileUtils;
import com.axelor.common.ResourceUtils;
import com.axelor.common.StringUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.util.OptionHelper;

/**
 * The logger configuration builder using logback-classic.
 * 
 * <p>
 * It can be configured with following properties:
 * </p>
 * 
 * <ul>
 * <li><b>logging.config</b> - the path to custom logback config file</li>
 * <li><b>logging.path</b> - the directory where log files are saved</li>
 * <li><b>logging.pattern.file</b> - the logging pattern for file appender</li>
 * <li><b>logging.pattern.console</b> - the logging pattern for console appender
 * </li>
 * <li><b>logging.level.root</b> - the logging level of root logger</li>
 * <li><b>logging.level.com.axelor</b> - the logging level of given logger</li>
 * </ul>
 * 
 * If <b>logging.config</b> is given, all other properties will be ignored.<br>
 * If <b>logging.path</b> is not given, file appender will be disabled</br>
 * If <b>logging.pattern.file</b> is set to <code>OFF</code>, file appender will
 * be disabled<br>
 * If <b>logging.pattern.console</b> is set to <code>OFF</code>, console
 * appender will be disabled<br>
 * If <b>logback.xml</b> is found in classpath, no custom configuration is done.
 *
 * <p>
 * The logging pattern can use <code>%clr()</code> to highlight based on log
 * level, or <code>%clr(){color}</code> with
 * <code>faint, red, gree, yellow, blue, magenta, cyan</code> as color to style
 * the log message on console output.
 * </p>
 */
public class LoggerConfiguration {

	private static final String DEFAULT_CONFIG = "logback.xml";

	private static final String LOGGING_CONFIG = "logging.config";
	private static final String LOGGING_PATH = "logging.path";

	private static final String LOGGING_PATTERN_FILE = "logging.pattern.file";
	private static final String LOGGING_PATTERN_CONSOLE = "logging.pattern.console";

	private static final Pattern LOGGING_LEVEL_PATTERN = Pattern.compile("logging\\.level\\.(.*?)");

	private static final String ANSI_LOG_PATTERN = "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) "
			+ "%clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} "
			+ "%clr(:){faint} %m%n";

	private static final String FILE_LOG_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p ${PID:- } --- [%t] %-40.40logger{39} : %m%n";

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private LoggerContext context;
	private Properties config;

	public LoggerConfiguration(Properties config) {
		this.context = (LoggerContext) LoggerFactory.getILoggerFactory();
		this.config = config;
	}

	private boolean isInstalled() {
		return this.context.getObject(LoggerConfiguration.class.getName()) != null;
	}
	
	private void markInstalled() {
		this.context.putObject(LoggerConfiguration.class.getName(), true);
	}
	
	private void markUninstalled() {
		this.context.removeObject(LoggerConfiguration.class.getName());
	}

	public void install() {
		if (isInstalled()) {
			return;
		}

		// install JUL handler
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		
		// set PID as system property
		if (System.getProperty("PID") == null) {
			System.setProperty("PID", getPid());
		}

		// configure logback
		configure();
		markInstalled();
	}

	public void uninstall() {
		// uninstall JUL handler
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.uninstall();
		markUninstalled();
	}

	private String getPid() {
		try {
			String jvmName = ManagementFactory.getRuntimeMXBean().getName();
			return jvmName.split("@")[0];
		} catch (Throwable ex) {
			return "???";
		}
	}

	private void configure() {
		if (config.getProperty(LOGGING_CONFIG) != null) {
			final File file = FileUtils.getFile(config.getProperty(LOGGING_CONFIG));
			if (file.exists()) {
				reset(file);
				return;
			} else {
				throw new RuntimeException("Unable to access logging config file: " + file);
			}
		}

		// don't do anything if default config found in classpath
		if (ResourceUtils.getResource(DEFAULT_CONFIG) != null) {
			return;
		}

		// start from scratch
		reset(null);

		// add common loggers
		logger("com.axelor", Level.INFO);
		logger("org.apache.catalina.startup.DigesterFactory", Level.ERROR);
		logger("org.apache.catalina.util.LifecycleBase", Level.ERROR);
		logger("org.apache.coyote.http11.Http11NioProtocol", Level.WARN);
		logger("org.apache.sshd.common.util.SecurityUtils", Level.WARN);
		logger("org.apache.tomcat.util.net.NioSelectorPool", Level.WARN);
		logger("org.eclipse.jetty.util.component.AbstractLifeCycle", Level.ERROR);
		logger("org.hibernate.validator.internal.util.Version", Level.WARN);

		Level rootLevel = Level.ERROR;

		// add loggers
		for (String key : config.stringPropertyNames()) {
			final Matcher matcher = LOGGING_LEVEL_PATTERN.matcher(key);
			if (matcher.matches()) {
				final String name = matcher.group(1);
				final Level level = Level.toLevel(config.getProperty(key, "ERROR"));
				if (org.slf4j.Logger.ROOT_LOGGER_NAME.equalsIgnoreCase(name)) {
					rootLevel = level;
				}
				logger(name, level);
			}
		}

		final Logger rootLogger = this.context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		final String logPath = config.getProperty(LOGGING_PATH);

		rootLogger.setLevel(rootLevel);

		// create console appender
		if (!"OFF".equalsIgnoreCase(config.getProperty(LOGGING_PATTERN_CONSOLE))) {
			rootLogger.addAppender(createConsoleAppender());
		}

		// create file appender
		if (!"OFF".equalsIgnoreCase(config.getProperty(LOGGING_PATTERN_FILE)) && !StringUtils.isBlank(logPath)) {
			rootLogger.addAppender(createFileAppender(Paths.get(logPath, "axelor.log").toString()));
		}
	}

	private void reset(File configFile) {
		final JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(context);
		context.reset();

		// register color converter
		conversionRule("clr", ColorConverter.class);

		if (configFile == null) {
			return;
		}
		try {
			configurator.doConfigure(configFile);
		} catch (JoranException e) {
			throw new RuntimeException(e);
		}
	}

	private Appender<ILoggingEvent> createConsoleAppender() {
		final ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<ILoggingEvent>();
		final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		final String logPattern = config.getProperty(LOGGING_PATTERN_CONSOLE, ANSI_LOG_PATTERN);

		encoder.setPattern(OptionHelper.substVars(logPattern, this.context));
		encoder.setCharset(UTF8);

		appender.setName("CONSOLE");
		appender.setEncoder(encoder);

		start(encoder);
		start(appender);

		return appender;
	}

	private Appender<ILoggingEvent> createFileAppender(String logFile) {
		final RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
		final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		final String logPattern = config.getProperty(LOGGING_PATTERN_FILE, FILE_LOG_PATTERN);

		encoder.setPattern(OptionHelper.substVars(logPattern, this.context));

		appender.setName("FILE");
		appender.setFile(logFile);
		appender.setEncoder(encoder);

		final FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
		final SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>("10MB");

		rollingPolicy.setFileNamePattern(logFile + ".%i");
		rollingPolicy.setParent(appender);

		appender.setRollingPolicy(rollingPolicy);
		appender.setTriggeringPolicy(triggeringPolicy);

		start(encoder);
		start(rollingPolicy);
		start(triggeringPolicy);
		start(appender);

		return appender;
	}

	@SuppressWarnings("all")
	public void conversionRule(String word, Class<? extends Converter<?>> converter) {
		Map<String, String> registry = (Map) this.context.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
		if (registry == null) {
			registry = new HashMap<String, String>();
			this.context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, registry);
		}
		registry.put(word, converter.getName());
	}

	private Logger logger(String name, Level level) {
		final Logger logger = this.context.getLogger(name);
		if (level != null) {
			logger.setLevel(level);
		}
		return logger;
	}

	private void start(LifeCycle lifeCycle) {
		if (lifeCycle instanceof ContextAware) {
			((ContextAware) lifeCycle).setContext(this.context);
		}
		lifeCycle.start();
	}
}
