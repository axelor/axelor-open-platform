/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
package com.axelor.tools.x2j;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.axelor.tools.x2j.pojo.Entity;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Extender extends Generator {

	private static final String OBJECT_PATH = "src/main/resources/objects";

	private List<File> modulePaths;

	public Extender(File base, File target, List<File> modulePaths) {
		super(base, target);
		this.modulePaths = modulePaths;
	}

	public Extender(String base, String target, List<File> modulePaths) {
		this(new File(base), new File(target), modulePaths);
	}

	private Entity merge(Entity target, Entity source) {
		target.merge(source);
		return target;
	}

	private List<Entity> accept(List<Entity> target, File input) {

		log.info("Processing: " + input);

		List<Entity> source = XmlHelper.entities(input);
		List<Entity> all = Lists.newArrayList();

		if (target == null || target.isEmpty()) {
			return source;
		}

		for (Entity t : target) {
			for (Entity s : source) {
				if (Objects.equal(t.getName(), s.getName())) {
					s = merge(t, s);
				}
				if (!all.contains(s)) all.add(s);
			}
			if (!all.contains(t)) all.add(t);
		}

		return all;
	}

	@Override
	public void start() throws IOException {

		Map<String, List<File>> all = Maps.newHashMap();
		List<File> searchPaths = Lists.newArrayList();

		searchPaths.addAll(modulePaths);
		searchPaths.add(this.base);

		for (File searchPath : searchPaths) {
			List<File> found = Lists.newArrayList();
			File path = this.file(searchPath, OBJECT_PATH);
			if (path.exists()) {
				found.addAll(Lists.newArrayList(path.listFiles()));
			}
			path = this.file(searchPath, DOMAIN_PATH);
			if (path.exists()) {
				found.addAll(Lists.newArrayList(path.listFiles()));
			}
			if (found.isEmpty()) continue;
			for (File input : found) {
				List<File> set = all.get(input.getName());
				if (set == null) {
					set = Lists.newArrayList();
					all.put(input.getName(), set);
				}
				if (!set.contains(input)) {
					set.add(input);
				}
			}
		}

		for (List<File> files : all.values()) {
			process(files);
		}
	}

	private void process(List<File> files) throws IOException {
		if (files == null || files.isEmpty()) {
			return;
		}

		List<Entity> target = Lists.newArrayList();
		File latest = files.get(0);

		for (File found : files) {
			if (found.lastModified() > latest.lastModified()) {
				latest = found;
			}
			target = accept(target, found);
		}

		for (Entity it: target) {
			expand(latest, outputPath, it);
		}
	}
}
