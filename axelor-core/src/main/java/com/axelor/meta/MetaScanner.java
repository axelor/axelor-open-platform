/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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
