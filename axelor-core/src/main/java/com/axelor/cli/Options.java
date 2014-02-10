package com.axelor.cli;

import java.util.List;

import com.beust.jcommander.Parameter;

class Options {

	@Parameter( names = { "-h", "--help" }, description = "show this help message", help = true)
	public Boolean showHelp;
	
	@Parameter( names = { "-p", "--unit" }, description = "jpa persistence unit name", required = true)
	public String unit;

	@Parameter( names = { "-i", "--init" }, description = "initialize the database")
	public Boolean init;

	@Parameter( names = { "-u", "--update" }, description = "update the installed modules")
	public Boolean update;
	
	@Parameter( names = { "-m", "--modules" }, description = "list of modules to update", variableArity = true)
	public List<String> modules;

	@Parameter( names = { "-d", "--with-demo" }, description = "initialize or update demo data")
	public Boolean importDemo;
}
