package com.axelor.tool.x2j;

import java.io.File;
import java.io.IOException;

import org.slf4j.LoggerFactory;

import com.axelor.tool.x2j.pojo.Entity;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;

public class Generator {

	static final String DOMAIN_PATH = "src/main/resources/domains";

	static final String OUTPUT_PATH = "src-gen";

	protected Log log = new Log(LoggerFactory.getLogger(getClass()));

	protected File domainPath;

	protected File outputPath;

	protected File base;
	protected File target;

	public Generator(File base, File target) {
		this.base = base;
		this.domainPath = this.file(base, DOMAIN_PATH);
		this.outputPath = this.file(target, OUTPUT_PATH);
	}

	public Generator(String base, String target) {
		this(new File(base), new File(target));
	}
	
	public Log getLog() {
		return log;
	}

	protected File file(File base, String... parts) {
		return new File(base.getPath() + "/" + Joiner.on("/").join(parts));
	}

	protected void expand(File input, File outputPath, Entity entity) throws IOException {

		File output = this.file(outputPath, entity.getFile());
		String fileName = output.getPath();

		//TODO: check before parsing
		if (input.lastModified() < output.lastModified()) {
			return;
		}

		output.getParentFile().mkdirs();

		String[] existing = {
			entity.getName() + ".java",
			entity.getName() + ".groovy"
		};

		for (String name : existing) {
			File ex = this.file(output.getParentFile(), name);
			if (ex.exists()) {
				ex.delete();
			}
		}

		log.info("Generating: " + fileName);

		String code = Expander.expand(entity);

		Files.write(code, output, Charsets.UTF_8);
	}

	protected void process(File input, File outputPath) throws IOException {

		log.info("Processing: " + input);

		for (Entity it : XmlHelper.entities(input)) {
			expand(input, outputPath, it);
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

	public void clean() {

		log.info("Cleaning generated sources.");
		log.info("Output path: " + outputPath);

		if (!this.outputPath.exists()) return;

		for (File file : outputPath.listFiles()) {
			delete(file);
		}
	}

	public void start() throws IOException {

		log.info("Generating JPA classes.");
		log.info("Domain path: " + domainPath);
		log.info("Output path: " + outputPath);

		outputPath.mkdirs();

		for (File file : domainPath.listFiles()) {
			if (file.getName().endsWith(".xml")) {
				process(file, outputPath);
			}
		}
	}
}
