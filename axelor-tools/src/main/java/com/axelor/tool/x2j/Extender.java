package com.axelor.tool.x2j;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.axelor.tool.x2j.pojo.Entity;
import com.axelor.tool.x2j.pojo.Property;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

public class Extender extends Generator {

	private static final String PROJECT_PATH = "axelor-objects";

	private static final String OBJECT_PATH = "src/main/resources/objects";

	private  File projectPath;

	public Extender(File base, File target) {
		super(base, target);
		this.target = this.file(base, PROJECT_PATH, target.getName());
		this.outputPath = this.file(this.target, OUTPUT_PATH);
		this.projectPath = this.file(base, PROJECT_PATH);
	}

	private List<String> modules() {
		final File props = this.file(projectPath, "modules.txt");
		final File basePom = this.file(base, "pom.xml");
		try {
			if (props.lastModified() > basePom.lastModified()) {
				return Files.readLines(props, Charsets.UTF_8);
			}
		} catch (Exception e) {
		}

		if (!basePom.exists()) {
			return Lists.newArrayList();
		}

		final List<String> all = XmlHelper.modules(basePom);
		try {
			Files.write(Joiner.on("\n").join(all), props, Charsets.UTF_8);
		} catch (Exception e) {
		}

		return all;
	}

	private boolean prepare() {
		final File basePom = this.file(base, "pom.xml");
		final File thisPom = this.file(projectPath, "pom.xml");

		if (!base.exists()) {
			return false;
		}
		if (thisPom.exists()) {
			return true;
		}

		String version = XmlHelper.version(basePom);
		String code = Expander.expandPom(version);

		try {
			Files.createParentDirs(thisPom);
			thisPom.createNewFile();
			Files.write(code, thisPom, Charsets.UTF_8);
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	private Entity merge(Entity target, Entity source) {
		for(Property property : source.getProperties()) {
			target.addField(property);
		}
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

		if (!prepare()) {
			return;
		}

		Map<String, List<File>> all = Maps.newHashMap();

		for (String module : modules()) {
			File path = this.file(base, module, OBJECT_PATH);
			if (!path.exists()) continue;
			for (File input : path.listFiles()) {
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
