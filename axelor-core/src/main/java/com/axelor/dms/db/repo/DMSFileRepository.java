package com.axelor.dms.db.repo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.PersistenceException;

import org.joda.time.LocalDateTime;

import com.axelor.common.ClassUtils;
import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.dms.db.DMSFile;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.repo.MetaAttachmentRepository;
import com.google.common.base.Strings;

public class DMSFileRepository extends JpaRepository<DMSFile> {

	@Inject
	private MetaFiles metaFiles;

	public DMSFileRepository() {
		super(DMSFile.class);
	}

	@SuppressWarnings("all")
	private Model findRelated(DMSFile file) {
		if (file.getRelatedId() == null ||
			file.getRelatedModel() == null) {
			return null;
		}
		Class<? extends Model> klass = null;
		try {
			klass = (Class) ClassUtils.findClass(file.getRelatedModel());
		} catch (Exception e) {
			return null;
		}
		return JpaRepository.of(klass).find(file.getRelatedId());
	}

	@Override
	public DMSFile save(DMSFile entity) {

		final DMSFile parent = entity.getParent();
		final Model related = findRelated(entity);
		final boolean isAttachment = related != null && entity.getMetaFile() != null;

		// if new attachment, save attachment reference
		if (isAttachment) {
			MetaAttachmentRepository repo = Beans.get(MetaAttachmentRepository.class);
			MetaAttachment attachment = repo.all()
					.filter("self.metaFile.id = ? AND self.objectId = ? AND self.objectName = ?",
							entity.getMetaFile().getId(),
							related.getId(),
							related.getClass().getName())
					.fetchOne();
			if (attachment == null) {
				attachment = metaFiles.attach(entity.getMetaFile(), related);
				repo.save(attachment);
			}
		}

		// if not an attachment or has parent, do nothing
		if (parent != null || related == null) {
			return super.save(entity);
		}

		// create parent folders

		Mapper mapper = Mapper.of(related.getClass());
		String homeName = null;
		try {
			homeName = mapper.getNameField().get(related).toString();
		} catch (Exception e) {
		}
		if (homeName == null) {
			homeName = Strings.padStart(""+related.getId(), 5, '0');
		}

		DMSFile dmsRoot = new DMSFile();
		dmsRoot.setFileName(entity.getRelatedModel());
		dmsRoot.setRelatedModel(entity.getRelatedModel());
		dmsRoot.setIsDirectory(true);
		dmsRoot = super.save(dmsRoot); // should get id before it's child

		DMSFile dmsHome = new DMSFile();
		dmsHome.setFileName(homeName);
		dmsHome.setRelatedId(entity.getRelatedId());
		dmsHome.setRelatedModel(entity.getRelatedModel());
		dmsHome.setParent(dmsRoot);
		dmsHome.setIsDirectory(true);
		dmsHome = super.save(dmsHome); // should get id before it's child

		entity.setParent(dmsHome);

		return super.save(entity);
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

		// remove attachment
		if (entity.getMetaFile() != null) {
			MetaAttachmentRepository attachmentRepo = Beans.get(MetaAttachmentRepository.class);
			MetaAttachment attachment = attachmentRepo.all().filter(
					"self.metaFile.id = ? AND self.objectId = ? AND self.objectName = ?",
					entity.getMetaFile().getId(),
					entity.getRelatedId(),
					entity.getRelatedModel()).fetchOne();

			if (attachment != null) {
				try {
					metaFiles.delete(attachment);
				} catch (IOException e) {
					throw new PersistenceException(e);
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
