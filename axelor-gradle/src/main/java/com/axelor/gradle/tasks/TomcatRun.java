/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.gradle.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.tasks.options.Option;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskAction;
import org.gradle.composite.internal.IncludedBuildInternal;

import com.axelor.common.FileUtils;

public class TomcatRun extends JavaExec {

	public static final String TOMCAT_CONFIGURATION = "tomcat";

	private Configuration tomcatConfiguration;

	private boolean hot;

	private int port = 8080;
	
	private File baseDir;
	
	private File webappDir;

	public TomcatRun() {
		this.tomcatConfiguration = getProject().getConfigurations().getByName(TOMCAT_CONFIGURATION);
	}

	@Option(option = "hot", description = "Specify whether to enable hot-swaping.")
	public void setHot(boolean hot) {
		this.hot = hot;
	}

	@Option(option = "port", description = "Specify the tomcat server port.")
	public void setHttpPort(String port) {
		this.port = Integer.parseInt(port);
	}

	@InputDirectory
	public File getBaseDir() {
		return baseDir;
	}

	public void setBaseDir(File baseDir) {
		this.baseDir = baseDir;
	}

	@InputDirectory
	public File getWebappDir() {
		return webappDir;
	}

	public void setWebappDir(File webappDir) {
		this.webappDir = webappDir;
	}
	
	@TaskAction
	@Override
	public void exec() {
		configureMain();
		configureHotswap();
		super.exec();
	}

	private void configureMain() {
		setMain("com.axelor.tomcat.TomcatRunner");
		setClasspath(tomcatConfiguration.getAsFileTree().matching(f -> f.exclude("*hotswap-agent*")));
		args("--port", port);
		args("--base-dir", baseDir.getAbsolutePath());
		args("--context-path", webappDir.getName());
		args(webappDir.getAbsolutePath());
	}

	private void configureHotswap() {
		if (!hasDCEVM()) {
			getLogger().info("Cannot enable hot-swaping as DCEVM is not installed.");
			return;
		}
		tomcatConfiguration.getFiles().stream()
			.filter(f -> f.getName().endsWith(".jar"))
			.filter(f -> f.getName().startsWith("hotswap-agent"))
			.findFirst()
			.ifPresent(agentJar -> {
				final String jdwpArgs = getAllJvmArgs().stream()
					.filter(s -> s.startsWith("-agentlib:jdwp="))
					.map(s -> s.replace("-agentlib:jdwp=", ""))
					.findFirst()
					.orElse("");
				
				if (hot || jdwpArgs.length() > 0) {
					final File hotswapConfig = createHotswapConfig(jdwpArgs);
					final StringBuilder agentArgs = new StringBuilder("-javaagent:")
							.append(agentJar.getAbsolutePath())
							.append("=propertiesFilePath=")
							.append(hotswapConfig.getAbsolutePath());
					if (jdwpArgs.length() == 0) {
						agentArgs.append(",autoHotswap=true");
					}
					jvmArgs("-XXaltjvm=dcevm");
					jvmArgs(agentArgs.toString());
				}
			});
	}

	protected static boolean hasDCEVM() {
		final Path home = Paths.get(System.getProperty("java.home"));
		final List<String> paths = Arrays.asList("lib/amd64/dcevm", "lib/i386/dcevm", "lib/dcevm", "bin/dcevm");
		final List<String> libs = Arrays.asList("libjvm.so", "libjvm.dylib", "jvm.dll");
		for (String path : paths) {
			for (String name : libs) {
				if (Files.exists(home.resolve(Paths.get(path, name)))) {
					return true;
				}
			}
		}
		return false;
	}

	protected File createHotswapConfig(String jdwpArgs) {
		final Function<Project, List<File>> findClasses = p -> Arrays.asList(
				FileUtils.getFile(p.getProjectDir(), "bin", "main"),
				FileUtils.getFile(p.getBuildDir(), "classes", "main"));
		final Function<Project, File> findResources = p ->
				FileUtils.getFile(p.getProjectDir(), "src", "main", "resources");

		final List<File> extraClasspath = new ArrayList<>();
		final List<File> watchResources = new ArrayList<>();

		getProject().getAllprojects().stream()
			.filter(p -> FileUtils.getFile(p.getProjectDir(), "build.gradle").exists())
			.forEach(p -> {
				extraClasspath.addAll(findClasses.apply(p));
				watchResources.add(findResources.apply(p));
			});
		getProject().getGradle().getIncludedBuilds().forEach(b -> {
			Gradle included = ((IncludedBuildInternal) b).getConfiguredBuild();
			included.getRootProject().getAllprojects().stream()
				.filter(p -> !p.getName().equals("axelor-gradle"))
				.filter(p -> !p.getName().equals("axelor-tomcat"))
				.filter(p -> !p.getName().equals("axelor-test"))
				.forEach(p -> {
					extraClasspath.addAll(findClasses.apply(p));
					watchResources.add(findResources.apply(p));
				});
		});

		final Properties hotswapProps = new Properties();
		hotswapProps.setProperty("LOGGER", "reload");
		hotswapProps.setProperty("autoHotswap", "true");

		for (String arg : jdwpArgs.split(",")) {
			String[] val = arg.split("=");
			if (val.length > 1 && val[0].equals("address")) {
				String address = val[1];
				if (address.contains(":")) {
					address = address.substring(address.lastIndexOf(":"));
				}
				hotswapProps.setProperty("autoHotswap.port", address);
			}
		}

		hotswapProps.setProperty("extraClasspath", extraClasspath.stream()
				.filter(File::exists)
				.map(File::getPath)
				.collect(Collectors.joining(",")));

		hotswapProps.setProperty("watchResources", watchResources.stream()
				.filter(File::exists)
				.map(File::getPath)
				.collect(Collectors.joining(",")));

		final File hotswapConfig = FileUtils.getFile(getBaseDir(), "hotswap-agent.properties");
		try (OutputStream os = new FileOutputStream(hotswapConfig)) {
			hotswapProps.store(os, null);
		} catch (IOException e) {
			getProject().getLogger().error(e.getMessage(), e);
		}
		return hotswapConfig;
	}
}
