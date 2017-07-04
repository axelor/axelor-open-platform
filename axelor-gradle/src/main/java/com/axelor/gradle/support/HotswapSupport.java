/*
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
package com.axelor.gradle.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.invocation.Gradle;
import org.gradle.composite.internal.IncludedBuildInternal;

import com.axelor.common.FileUtils;
import com.axelor.gradle.tasks.GenerateCode;
import com.google.common.base.Joiner;

public class HotswapSupport extends AbstractSupport {

	public static String GENERATE_HOTSWAP_CONFIG_TASK = "generateHotswapConfig";

	private static boolean hasDCEVM;

	static {
		final Path home = Paths.get(System.getProperty("java.home"));
		final List<String> paths = Arrays.asList("lib/amd64/dcevm", "lib/i386/dcevm", "lib/dcevm", "bin/dcevm");
		final List<String> libs = Arrays.asList("libjvm.so", "libjvm.dylib", "jvm.dll");
		for (String path : paths) {
			for (String name : libs) {
				if (Files.exists(home.resolve(Paths.get(path, name)))) {
					hasDCEVM = true;
				}
			}
		}
	}

	public static boolean hasDCEVM() {
		return hasDCEVM;
	}

	public static List<String> getAgentArgs(Project project, boolean auto) {
		if (!hasDCEVM()) {
			return Collections.emptyList();
		}
		final Configuration tomcat = project.getConfigurations().getByName(TomcatSupport.TOMCAT_CONFIGURATION);
		final List<String> args = new ArrayList<>();
		tomcat.getFiles().stream()
			.filter(f -> f.getName().endsWith(".jar"))
			.filter(f -> f.getName().startsWith("hotswap-agent"))
			.findFirst()
			.ifPresent(agentJar -> {
				final File config = new File(project.getBuildDir(), "hotswap-agent.properties");
				final StringBuilder agent = new StringBuilder("-javaagent:").append(agentJar.getAbsolutePath());
				final List<String> agentArgs = new ArrayList<>();
				if (config.exists()) {
					agentArgs.add("propertiesFilePath=" + config.getAbsolutePath());
				}
				if (auto) {
					agentArgs.add("autoHotswap=true");
				}
				if (agentArgs.size() > 0) {
					agent.append("=").append(Joiner.on(",").skipNulls().join(agentArgs));
				}
				args.add("-XXaltjvm=dcevm");
				args.add(agent.toString());
			});
		return args;
	}

	@Override
	public void apply(Project project) {

		project.getTasks().create(GENERATE_HOTSWAP_CONFIG_TASK, task -> {
			task.setDescription("Generate hotswap-agent.properties");
			task.onlyIf(t -> hasDCEVM());
			task.doLast(t -> {
				generateHotswapConfig(project);
			});
		});

		project.getTasks().withType(GenerateCode.class).all(task -> task.finalizedBy(GENERATE_HOTSWAP_CONFIG_TASK));
	}

	private void generateHotswapConfig(Project project) {
		final Function<Project, List<File>> findClasses = p -> Arrays.asList(
				FileUtils.getFile(p.getProjectDir(), "bin", "main"),
				FileUtils.getFile(p.getBuildDir(), "classes", "main"));
		final Function<Project, File> findResources = p ->
				FileUtils.getFile(p.getProjectDir(), "src", "main", "resources");

		final List<File> extraClasspath = new ArrayList<>();
		final List<File> watchResources = new ArrayList<>();

		project.getAllprojects().stream()
			.filter(p -> FileUtils.getFile(p.getProjectDir(), "build.gradle").exists())
			.forEach(p -> {
				extraClasspath.addAll(findClasses.apply(p));
				watchResources.add(findResources.apply(p));
			});
		project.getGradle().getIncludedBuilds().forEach(b -> {
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

		hotswapProps.setProperty("extraClasspath", extraClasspath.stream()
				.filter(File::exists)
				.map(File::getPath)
				.collect(Collectors.joining(",")));

		hotswapProps.setProperty("watchResources", watchResources.stream()
				.filter(File::exists)
				.map(File::getPath)
				.collect(Collectors.joining(",")));

		final File target = new File(project.getBuildDir(), "hotswap-agent.properties");
		try (OutputStream os = new FileOutputStream(target)) {
			hotswapProps.store(os, null);
		} catch (IOException e) {
			project.getLogger().error(e.getMessage(), e);
		}
	}
}
