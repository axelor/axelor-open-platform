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
package com.axelor.tools.x2j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.tools.x2j.pojo.Entity;
import com.axelor.tools.x2j.pojo.Repository;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

public class Generator {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private File domainPath;

	private File outputPath;

	private final Multimap<String, Entity> lookup = LinkedHashMultimap.create();
	private final Multimap<String, Entity> entities = LinkedHashMultimap.create();

	public Generator(File domainPath, File outputPath) {
		this.domainPath = domainPath;
		this.outputPath = outputPath;
	}

	private File file(File base, String... parts) {
		return new File(base.getPath() + "/" + Joiner.on("/").join(parts));
	}

	private void expand(Collection<Entity> items, boolean doLookup) throws IOException {
		expand(outputPath, items, doLookup);
	}
	
	private void expand(File outputPath, Collection<Entity> items, boolean doLookup) throws IOException {

		if (items == null || items.isEmpty()) {
			return;
		}

		final List<Entity> all = new ArrayList<>(items);
		final Entity first = all.get(0);

		final String ns = first.getNamespace();
		final String name = first.getName();

		// prepend all lookup entities
		if (doLookup && lookup.get(name) != null) {
			all.addAll(0, lookup.get(name));
		}
		
		// check that all entities have same namespace
		for (Entity it : all) {
			if (!ns.equals(it.getNamespace())) {
				throw new IllegalArgumentException(String.format(
						"Invalid namespace: %s.%s != %s.%s", ns, name,
						it.getNamespace(), name));
			}
		}

		final Entity entity = all.remove(0);
		final Repository repository = entity.getRepository();

		final File entityFile = this.file(outputPath, entity.getFile());
		final File repoFile = repository == null ? null : this.file(outputPath, repository.getFile());

		long lastModified = entity.getLastModified();
		
		for (Entity it : all) {
			if (lastModified < it.getLastModified()) {
				lastModified = it.getLastModified();
			}
		}
		
		if (lastModified < entityFile.lastModified()) {
			return;
		}
		
		for (Entity it : all) {
			entity.merge(it);
		}
		
		entityFile.getParentFile().mkdirs();
		if (repoFile != null) {
			repoFile.getParentFile().mkdirs();
		}

		String[] existing = {
			entity.getName() + ".java",
			entity.getName() + ".groovy"
		};

		for (String fname : existing) {
			File ex = this.file(entityFile.getParentFile(), fname);
			if (ex.exists()) {
				ex.delete();
			}
		}

		log.info("Generating: " + entityFile.getPath());
		String code = Expander.expand(entity, false);
		Files.write(Utils.stringTrailing(code), entityFile, Charsets.UTF_8);
		
		if (repoFile == null) return;

		log.info("Generating: " + repoFile.getPath());
		String repo = Expander.expand(entity, true);
		Files.write(Utils.stringTrailing(repo), repoFile, Charsets.UTF_8);
	}

	private void process(File input, boolean verbose) throws IOException {
		
		if (verbose) {
			log.info("Processing: " + input);
		}
		
		final List<Entity> all = XmlHelper.entities(input);
		for (Entity entity : all) {
			entity.setLastModified(input.lastModified());
			entities.put(entity.getName(), entity);
		}
	}

	private void delete(File file) {
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				delete(f);
			}
		}
		file.delete();
	}

	private void processAll(boolean verbose) throws IOException {
		if (!domainPath.exists()) return;
		for (File file : domainPath.listFiles()) {
			if (file.getName().endsWith(".xml")) {
				process(file, verbose);
			}
		}
	}
	
	public void addLookupSource(Generator generator) throws IOException {
		if (generator == null) return;
		if (generator.entities.isEmpty()) {
			generator.processAll(false);
		}
		lookup.putAll(generator.entities);
	}
	
	public void clean() {

		if (!this.outputPath.exists()) return;

		log.info("Cleaning generated sources.");
		log.info("Output path: " + outputPath);

		for (File file : outputPath.listFiles()) {
			delete(file);
		}
	}

	public void start() throws IOException {

		log.info("Generating JPA classes.");
		log.info("Domain path: " + domainPath);
		log.info("Output path: " + outputPath);

		outputPath.mkdirs();

		if (this.domainPath.exists()) {
			for (File file : domainPath.listFiles()) {
				if (file.getName().endsWith(".xml")) {
					process(file, true);
				}
			}
		}
		
		for (String name : entities.keySet()) {
			expand(entities.get(name), true);
		}

		// make sure to generate extended entities from parent modules
		for (String name : lookup.keySet()) {
			if (entities.containsKey(name)) {
				continue;
			}
			final Collection<Entity> all = lookup.get(name);
			if (all == null || all.size() < 2) {
				continue;
			}
			expand(all, false);
		}
	}
	
	/**
	 * Get a {@link Generator} instance for the given source files.
	 * <p>
	 * Used by code generator task to add lookup source to core modules.
	 * 
	 * @param files
	 *            input files
	 * @return a {@link Generator} instance
	 */
	public static Generator forFiles(Collection<File> files) {
		if (files == null || files.isEmpty()) {
			return null;
		}
		final Generator gen = new Generator(null, null) {

			@Override
			public void start() throws IOException {}

			@Override
			public void clean() {}

			@Override
			public void addLookupSource(Generator generator) throws IOException {}
		};
		for (File file : files) {
			try {
				gen.process(file, false);
			} catch (IOException e) {
			}
		}
		return gen;
	}
}
