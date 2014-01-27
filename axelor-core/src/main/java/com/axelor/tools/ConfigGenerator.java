/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
