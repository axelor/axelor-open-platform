/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
