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
package com.axelor.tools.x2j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.tools.x2j.pojo.Entity;
import com.axelor.tools.x2j.pojo.EnumType;
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

	private final Set<String> defines = new HashSet<>();
	private final List<Generator> lookup = new ArrayList<>();

	private final Multimap<String, Entity> entities = LinkedHashMultimap.create();
	private final Multimap<String, EnumType> enums = LinkedHashMultimap.create();

	public Generator(File domainPath, File outputPath) {
		this.domainPath = domainPath;
		this.outputPath = outputPath;
	}

	private File file(File base, String... parts) {
		return new File(base.getPath() + "/" + Joiner.on("/").join(parts));
	}
	
	private List<File> render(Collection<EnumType> items) throws IOException {

		if (items == null || items.isEmpty()) {
			return null;
		}

		final List<EnumType> all = new ArrayList<>(items);
		final EnumType entity = all.remove(0);

		final File entityFile = this.file(outputPath, entity.getFile());
		
		entityFile.getParentFile().mkdirs();

		String[] existing = {
			entity.getName() + ".java",
		};

		for (String fname : existing) {
			File ex = this.file(entityFile.getParentFile(), fname);
			if (ex.exists()) {
				ex.delete();
			}
		}

		final List<File> rendered = new ArrayList<>();

		log.info("Generating: " + entityFile.getPath());
		String code = Expander.expand(entity);
		Files.write(Utils.stripTrailing(code) + "\n", entityFile, Charsets.UTF_8);
		rendered.add(entityFile);

		return rendered;
	}

	private List<File> render(Collection<Entity> items, boolean doLookup) throws IOException {

		if (items == null || items.isEmpty()) {
			return null;
		}

		final List<Entity> all = new ArrayList<>(items);
		final Entity first = all.get(0);

		final String ns = first.getNamespace();
		final String name = first.getName();

		// prepend all lookup entities
		if (doLookup) {
			for (Generator gen : lookup) {
				if (gen.defines.contains(name)) {
					if (gen.entities.isEmpty()) {
						gen.processAll(false);
					}
					all.addAll(0, gen.entities.get(name));
				}
			}
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

		final List<File> rendered = new ArrayList<>();

		log.info("Generating: " + entityFile.getPath());
		String code = Expander.expand(entity, false);
		Files.write(Utils.stripTrailing(code), entityFile, Charsets.UTF_8);
		rendered.add(entityFile);

		if (repoFile != null) {
			log.info("Generating: " + repoFile.getPath());
			String repo = Expander.expand(entity, true);
			Files.write(Utils.stripTrailing(repo) + "\n", repoFile, Charsets.UTF_8);
			rendered.add(repoFile);
		}

		return rendered;
	}

	protected void findFrom(File input) throws IOException {
		defines.addAll(XmlHelper.findEntityNames(input));
	}

	protected void findAll() throws IOException {
		if (!domainPath.exists()) return;
		for (File file : domainPath.listFiles()) {
			if (file.getName().endsWith(".xml")) {
				findFrom(file);
			}
		}
	}

	protected void process(File input, boolean verbose) throws IOException {
		
		if (verbose) {
			log.info("Processing: " + input);
		}
		
		final List<Entity> all = XmlHelper.entities(input);
		for (Entity entity : all) {
			entity.setLastModified(input.lastModified());
			entities.put(entity.getName(), entity);
		}
		
		for (EnumType entity : XmlHelper.enums(input)) {
			entity.setLastModified(input.lastModified());
			enums.put(entity.getName(), entity);
		}
	}

	protected void processAll(boolean verbose) throws IOException {
		if (!domainPath.exists()) return;
		for (File file : domainPath.listFiles()) {
			if (file.getName().endsWith(".xml")) {
				process(file, verbose);
			}
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
	
	public void addLookupSource(Generator generator) throws IOException {
		if (generator == null) return;
		if (generator.defines.isEmpty()) {
			generator.findAll();
		}
		lookup.add(0, generator);
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

		log.info("Generating classes...");
		log.info("Domain path: " + domainPath);
		log.info("Output path: " + outputPath);

		outputPath.mkdirs();

		final Set<File> generated = new HashSet<>();

		if (this.domainPath.exists()) {
			for (File file : domainPath.listFiles()) {
				if (file.getName().endsWith(".xml")) {
					process(file, true);
				}
			}
		}

		for (String name : enums.keySet()) {
			final List<File> rendered = render(enums.get(name));
			if (rendered != null) {
				generated.addAll(rendered);
			}
		}

		for (String name : entities.keySet()) {
			final List<File> rendered = render(entities.get(name), true);
			if (rendered != null) {
				generated.addAll(rendered);
			}
		}

		// make sure to generate extended entities from parent modules
		final Multimap<String, Entity> extended = LinkedHashMultimap.create();
		for (Generator generator : lookup) {
			for (String name : generator.defines) {
				if (entities.containsKey(name)) {
					continue;
				}
				if (generator.entities.isEmpty()) {
					generator.processAll(false);
				}
				extended.putAll(name, generator.entities.get(name));
			}
		}
		for (String name : extended.keySet()) {
			final List<Entity> all = new ArrayList<>(extended.get(name));
			if (all == null || all.size() < 2) {
				continue;
			}
			Collections.reverse(all);
			final List<File> rendered = render(all, false);
			if (rendered != null) {
				generated.addAll(rendered);
			}
		}

		// clean up obsolete files
		java.nio.file.Files.walk(outputPath.toPath())
			.map(Path::toFile)
			.filter(f -> f.getName().endsWith(".java") || f.getName().endsWith(".groovy"))
			.filter(f -> !generated.contains(f))
			.forEach(f -> {
				log.info("Deleting obsolete file: {}", f);
				f.delete();
			});
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
	public static Generator forFiles(final Collection<File> files) {
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
			
			@Override
			protected void processAll(boolean verbose) throws IOException {
				for (File file : files) {
					process(file, verbose);
				}
			}
		};
		for (File file : files) {
			try {
				gen.findFrom(file);
			} catch (IOException e) {
			}
		}
		return gen;
	}
}
