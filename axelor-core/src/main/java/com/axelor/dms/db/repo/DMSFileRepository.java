package com.axelor.dms.db.repo;

import java.util.Map;

import org.joda.time.LocalDateTime;

import com.axelor.db.JpaRepository;
import com.axelor.dms.db.DMSFile;

public class DMSFileRepository extends JpaRepository<DMSFile> {

	public DMSFileRepository() {
		super(DMSFile.class);
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

		json.put("editIcon", "fa fa-pencil");
		json.put("removeIcon", "fa fa-remove");
		json.put("typeIcon", isFile ? "fa fa-file" : "fa fa-folder");
		json.put("downloadIcon", "fa fa-download");
		json.put("lastModified", dt);

		return json;
	}
}
