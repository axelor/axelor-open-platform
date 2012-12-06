package com.axelor.data;

import java.io.File;
import java.util.List;
import java.util.Properties;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;

public class Commander {
	
	@Parameter(required = true, names = { "-c", "--config" }, description = "data binding configuration file")
	private File config;
	
	@Parameter(required = true, names = { "-d", "--data-dir" }, description = "location of data directory")
	private File dataDir;
	
	@DynamicParameter(names = "-D", description = "define input mappings")
	private Properties files = new Properties();
	
	@Parameter( names = { "-h", "--help" }, description = "show this help message")
	private Boolean showHelp;
	
	public void setConfig(File config) {
		this.config = config;
	}
	
	public File getConfig() {
		return config;
	}
	
	public File getDataDir() {
		return dataDir;
	}
	
	public Properties getFiles() {
		return files;
	}
	
	public Boolean getShowHelp() {
		return showHelp;
	}
	
	public File getFile(String name) {
		return new File(dataDir, name); 
	}
	
	public List<File> getFiles(String key) {
		List<File> all = Lists.newArrayList();
		if (files.containsKey(key))
			for (String file : files.get(key).toString().split(","))
				all.add(new File(dataDir, file));
		return all;
	}
	
	public void setFiles(Properties files) {
		this.files = files;
	}
	
	public JCommander parse(String... args) {
		JCommander commander = getCommander(this);
		commander.parse(args);
		return commander;
	}
	
	private static JCommander getCommander(Commander cmd) {
		JCommander commander = new JCommander(cmd);
		commander.setProgramName("axelor-data");
		return commander;
	}
	
	public static void usage() {
		Commander cmd = new Commander();
		cmd.setFiles(null);
		getCommander(cmd).usage();
	}
	
}
