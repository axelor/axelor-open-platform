package com.axelor.dms.db.repo;

import java.util.List;
import java.util.Map;

import org.joda.time.LocalDateTime;

import com.axelor.db.JpaRepository;
import com.axelor.dms.db.DMSFile;

public class DMSFileRepository extends JpaRepository<DMSFile> {

	public DMSFileRepository() {
		super(DMSFile.class);
	}

	@Override
	public void remove(DMSFile entity) {
		// remove all children
		if (entity.getIsDirectory() == Boolean.TRUE) {
			final List<DMSFile> children = all().filter("self.parent.id = ?", entity.getId()).fetch();
			for (DMSFile child : children) {
				if (child != entity) {
					remove(child);;
				}
			}
		}
		super.remove(entity);
	}

	@Override
	public Map<String, Object> populate(Map<String, Object> json) {
		final Object id = json.get("id");
		if (id == null) {
			return json;
		}
		final DMSFile file = find((Long) id);
		if (file == null) {
			return json;
		}

		boolean isFile = file.getIsDirectory() != Boolean.TRUE;
		LocalDateTime dt = file.getUpdatedOn();
		if (dt == null) {
			dt = file.getCreatedOn();
		}

		json.put("typeIcon", isFile ? "fa fa-file" : "fa fa-folder");
		json.put("lastModified", dt);

		return json;
	}
}
