/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
package com.axelor.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import com.axelor.db.JPA;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

public final class ConfigGenerator {

	private static final String[] CORE = {
		"auth",
		"meta",
		"wkf",
	};

	private static final String MODE_RW = "read-write";
	private static final String MODE_NR = "nonstrict-read-write";

	private final List<String> coreNames = Lists.newArrayList();
	private final List<String> appNames = Lists.newArrayList();

	private String bigName;

	public ConfigGenerator() {

	}

	public void setBigName(String bigName) {
		this.bigName = bigName;
	}

	private void add(String module, String name) {
		List<String> names = appNames;
		String mode = MODE_RW;
		String region = bigName;

		if (region == null) {
			region = module;
		}

		if (Arrays.binarySearch(CORE, module) > -1) {
			names = coreNames;
			mode = MODE_NR;
		}

		names.add(module + ":" + name + " = " + mode + "," + region);
	}

	private void doPrintHeader(PrintWriter out) throws IOException {
		URL res = Resources.getResource("templates/ehcache-objects.template");
		List<String> lines = Resources.readLines(res, Charsets.UTF_8);
		for (String line : lines) {
			out.println(line);
		}
	}

	private void doPrint(String module, List<String> lines, PrintWriter out) {
		out.println();
		out.println("#");
		out.println("# " + module);
		out.println("#");
		for (String line : lines) {
			out.println(line);
		}
	}

	public void generate(Writer writer) throws IOException {

		for (Class<?> model : JPA.models()) {
			String pkg = model.getPackage().getName().replaceFirst("\\.db$", "");
			String module = pkg.substring(pkg.lastIndexOf('.') + 1, pkg.length());
			add(module, model.getName());
			for (Field field : model.getDeclaredFields()) {
				if (field.getAnnotation(OneToMany.class) != null ||
					field.getAnnotation(ManyToMany.class) != null ) {
					add(module, model.getName() + "." + field.getName());
				}
			}
		}

		Collections.sort(coreNames);
		Collections.sort(appNames);

		final Map<String, List<String>> all = Maps.newLinkedHashMap();
		final List<String> names = Lists.newArrayList();
		names.addAll(coreNames);
		names.addAll(appNames);

		for (String name : names) {
			String[] parts = name.split(":");
			String module = parts[0];
			String line = parts[1];

			List<String> lines = all.get(module);
			if (lines == null) {
				lines = Lists.newArrayList();
				all.put(module, lines);
			}
			lines.add(line);
		}

		PrintWriter out = new PrintWriter(writer);

		doPrintHeader(out);

		for (String module : all.keySet()) {
			List<String> lines = all.get(module);
			doPrint(module, lines, out);
		}

		out.flush();
	}

	public void ehcache(Writer writer) throws IOException {

		List<String> lines = Resources.readLines(
				Resources.getResource("templates/ehcache.template"), Charsets.UTF_8);

		if (bigName != null) {
			lines.add(lines.size() - 1, "  <cache");
			lines.add(lines.size() - 1, "    name=\"" + bigName + "\"");
			lines.add(lines.size() - 1, "    maxBytesLocalHeap=\"128M\"");
			lines.add(lines.size() - 1, "    maxMemoryOffHeap=\"32G\"");
			lines.add(lines.size() - 1, "    eternal=\"true\">");
			lines.add(lines.size() - 1, "    <persistence strategy=\"localTempSwap\"/>");
			lines.add(lines.size() - 1, "  </cache>");
			lines.add(lines.size() - 1, "");
		}

		PrintWriter out = new PrintWriter(writer);

		for (String line : lines) {
			out.println(line);
		}

		out.flush();
	}
}
