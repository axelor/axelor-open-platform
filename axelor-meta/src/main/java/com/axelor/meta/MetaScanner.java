package com.axelor.meta;

import java.util.List;
import java.util.regex.Pattern;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.vfs.Vfs;
import org.reflections.vfs.Vfs.File;

import com.google.common.collect.Lists;

public class MetaScanner extends ResourcesScanner {

	private List<Vfs.File> files = Lists.newArrayList();

	private Pattern pattern;
	
	public MetaScanner(String regex) {
		this.pattern = Pattern.compile(regex);
	}

	@Override
	public boolean acceptsInput(String file) {
		return pattern.matcher(file).matches();
	}
	
	@Override
	public boolean acceptResult(String fqn) {
		System.err.println("FFFF: " + fqn);
		return pattern.matcher(fqn).matches();
	}

	@Override
	public Object scan(File file, Object classObject) {
		this.files.add(file);
		return super.scan(file, classObject);
	}
	
	public static List<Vfs.File> findAll(String regex) {
		MetaScanner scanner = new MetaScanner(regex);
		new Reflections(scanner);
		return scanner.files;
	}
}
