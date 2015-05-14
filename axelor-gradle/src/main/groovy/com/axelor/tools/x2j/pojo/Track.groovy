package com.axelor.tools.x2j.pojo

import groovy.util.slurpersupport.NodeChild;

class Track {

	private List<Annotation> fields = []
	private List<Annotation> messages = []

	private Entity entity

	Track(Entity entity, NodeChild node) {
		this.entity = entity
		node."*".each {
			if (it.name() == "field") fields += $field(it)
			if (it.name() == "message") messages += $message(it)
		}

		fields = fields.grep { it != null }
		messages = messages.grep { it != null }
	}

	private Annotation $field(NodeChild node) {

		String name = node.'@name'
		String on = node.'@on'
		String condition = node.'@if'

		def annon = new Annotation(this.entity, "com.axelor.db.annotations.TrackField")
			.add("name", name)

		if (condition) annon.add("condition", condition)
		if (on) {
			this.entity.importType("com.axelor.db.annotations.TrackEvent");
			annon.add("on", "TrackEvent.${on}", false)
		}

		return annon
	}

	private Annotation $message(NodeChild node) {

		String on = node.'@on'
		String tag = node.'@tag'
		String message = node.text()
		String condition = node.'@if'
		String fields = node.'@fields'

		def annon = new Annotation(this.entity, "com.axelor.db.annotations.TrackMessage")
			.add("message", message)
			.add("condition", condition)

		if (tag) annon.add("tag", tag)
		if (on) {
			this.entity.importType("com.axelor.db.annotations.TrackEvent");
			annon.add("on", "TrackEvent.${on}", false)
		}

		if (fields) {
			List<String> names = fields.trim().split(/\s*,\s*/) as List;
			annon.add("fields", names, true, true);
		}

		return annon
	}

	def $track() {
		def annon = new Annotation(this.entity, "com.axelor.db.annotations.Track")
		annon.add("fields", fields, false, false)
		annon.add("messages", messages, false, false)
		return annon
	}
}
