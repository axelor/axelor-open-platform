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
