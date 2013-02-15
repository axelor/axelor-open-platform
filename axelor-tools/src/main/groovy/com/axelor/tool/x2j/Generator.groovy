package com.axelor.tool.x2j

import org.slf4j.Logger;

import groovy.text.GStringTemplateEngine
import groovy.text.Template
import groovy.util.logging.Slf4j;

import com.axelor.tool.x2j.pojo.Entity

@Slf4j
class Generator {
	
	static abstract class Expander {
		
		protected engine = new GStringTemplateEngine()
		
		protected Template pojoTemplate
		
		protected Template bodyTemplate
		
		protected Template headTemplate
		
		protected Reader read(String resource) {
			def is = Thread.currentThread().contextClassLoader.getResourceAsStream(resource)
			new BufferedReader(
					new InputStreamReader(is)
				)
		}
		
		protected Template template(String resource) {
			engine.createTemplate(read(resource))
		}
		
		String expand(Entity entity) {
			
			def binding = [pojo: entity]
			
			def body = bodyTemplate.make(binding).toString()
			def imports = headTemplate.make(binding).toString()
			
			binding = [namespace: entity.namespace, body: body, imports: imports]
			
			return pojoTemplate.make(binding).toString()
		}
	}
	
	@Singleton
	static class JavaExpander extends Expander {
		
		JavaExpander () {
			pojoTemplate = template("templates/pojo.template")
			headTemplate = template("templates/head.template")
			bodyTemplate = template("templates/body.template")
		}
	}
	
	@Singleton
	static class GroovyExpander extends JavaExpander {
		
	}
	
	File searchPath
	
	File outputPath
	
	public Generator(String searchPath, String outputPath) {
		
		this.searchPath = new File(searchPath)
		this.outputPath = new File(outputPath)
		
		if (!this.searchPath.directory) {
			throw new RuntimeException("No such directory: $searchPath")
		}
		
		if (this.outputPath.file) {
			throw new RuntimeException("Not a directory: $outputPath")
		}
	}
	
	void clean() {
		
		log.info("Cleaning generated sources.")
		log.info("Output path: " + outputPath)
		
		if (!this.outputPath.exists()) return;
		
		outputPath.eachDir {
			if (!it.name.startsWith("."))
				it.deleteDir()
		}
		
		outputPath.eachFile {
			if (!it.name.startsWith("."))
				it.delete()
		}
	}
	
	void start() {
		
		log.info("Generating JPA classes.")
		log.info("Search path: " + searchPath)
		log.info("Output path: " + outputPath)
		
		outputPath.mkdirs()
		
		searchPath.eachFileMatch(~/.*\.xml$/) {
			process it, outputPath
		}
	}
	
	void process(File input, File outdir) {
		
		log.info("Processing: " + input)

		def records = new XmlSlurper().parse(input)
		
		records.entity.each {
			
			def entity = new Entity(it)
			Expander expander = entity.groovy ? GroovyExpander.instance : JavaExpander.instance
			
			def code = expander.expand(entity)
			
			def fileName = outdir.path + "/" + entity.file
			def output = new File(fileName)

			if (input.lastModified() < output.lastModified())
				return
			
			output.parentFile.mkdirs()
			output.parentFile.eachFileMatch(~/${entity.name}\.(java|groovy)$/) {
				it.delete()
			}
			log.info("Generating: " + fileName)
			output.createNewFile()
			output.write(code)
		}
	}
}
