/**
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
package com.axelor.tools.x2j.pojo

import groovy.util.slurpersupport.NodeChild;

class Track {

	private List<Annotation> fields = []
	private List<Annotation> messages = []
	private List<Annotation> contents = []

	private List<String> imports = []

	private Entity entity

	private boolean subscribe;

	private boolean replace;

	private boolean files;

	private Track(Entity entity) {
		this.entity = entity;
	}

	Track(Entity entity, NodeChild node) {
		this.entity = entity
		node."*".each {
			if (it.name() == "field") fields += $field(it)
			if (it.name() == "message") messages += $message(it)
			if (it.name() == "content") contents += $message(it)
		}

		fields = fields.grep { it != null }
		messages = messages.grep { it != null }
		contents = contents.grep { it != null }
		subscribe = node.'@subscribe' == "true"
		replace = node.'@replace' == "true"
		files = node.'@files' == "true"
	}

	private Annotation $field(NodeChild node) {

		String name = node.'@name'
		String on = node.'@on'
		String condition = node.'@if'

		imports += ['com.axelor.db.annotations.TrackField']

		def annon = new Annotation(this.entity, "TrackField")
			.add("name", name)

		if (condition) annon.add("condition", condition)
		if (on) {
			imports += ['com.axelor.db.annotations.TrackEvent']
			annon.add("on", "com.axelor.db.annotations.TrackEvent.${on}", false)
		}

		return annon
	}

	private Annotation $message(NodeChild node) {

		String on = node.'@on'
		String tag = node.'@tag'
		String message = node.text()
		String condition = node.'@if'
		String fields = node.'@fields'

		imports += ['com.axelor.db.annotations.TrackMessage']

		def annon = new Annotation(this.entity, "TrackMessage")
			.add("message", message)
			.add("condition", condition)

		if (tag) annon.add("tag", tag)
		if (on) {
			imports += ['com.axelor.db.annotations.TrackEvent']
			annon.add("on", "com.axelor.db.annotations.TrackEvent.${on}", false)
		}

		if (fields) {
			List<String> names = fields.trim().split(/\s*,\s*/) as List;
			annon.add("fields", names, true, true);
		}

		return annon
	}

	def $track() {
		def annon = new Annotation(this.entity, "com.axelor.db.annotations.Track")
		imports.each { name -> this.entity.importType(name) }
		if (!fields.empty) annon.add("fields", fields, false, false)
		if (!messages.empty) annon.add("messages", messages, false, false)
		if (!contents.empty) annon.add("contents", contents, false, false)
		if (subscribe) annon.add("subscribe", "true", false, false)
		if (files) annon.add("files", "true", false, false)
		return annon
	}

	def merge(Track other) {
		fields.addAll(other.fields);
		messages.addAll(other.messages);
		contents.addAll(other.contents);
		imports.addAll(other.imports);
		if (other.replace) {
			subscribe = other.subscribe;
		}
		return this;
	}

	def copyFor(Entity base) {
		Track track = new Track(base);
		track.merge(this);
		return track;
	}
}
