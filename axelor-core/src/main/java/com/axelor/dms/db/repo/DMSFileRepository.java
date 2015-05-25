/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
package com.axelor.dms.db.repo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.PersistenceException;

import org.joda.time.LocalDateTime;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.ClassUtils;
import com.axelor.common.Inflector;
import com.axelor.db.JpaRepository;
import com.axelor.db.JpaSecurity;
import com.axelor.db.JpaSecurity.AccessType;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.dms.db.DMSFile;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaAttachmentRepository;
import com.google.common.base.Strings;

public class DMSFileRepository extends JpaRepository<DMSFile> {

	@Inject
	private MetaFiles metaFiles;

	@Inject
	private JpaSecurity security;

	@Inject
	private DMSPermissionRepository dmsPermissions;

	@Inject
	private MetaAttachmentRepository attachments;

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
			MetaAttachment attachment = attachments.all()
					.filter("self.metaFile.id = ? AND self.objectId = ? AND self.objectName = ?",
							entity.getMetaFile().getId(),
							related.getId(),
							related.getClass().getName())
					.fetchOne();
			if (attachment == null) {
				attachment = metaFiles.attach(entity.getMetaFile(), related);
				attachments.save(attachment);
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

		DMSFile dmsRoot = all().filter(
				"(self.relatedId is null OR self.relatedId = 0) AND self.relatedModel = ? and self.isDirectory = true",
				entity.getRelatedModel()).fetchOne();

		final Inflector inflector = Inflector.getInstance();

		if (dmsRoot == null) {
			dmsRoot = new DMSFile();
			dmsRoot.setFileName(inflector.pluralize(inflector.humanize(related.getClass().getSimpleName())));
			dmsRoot.setRelatedModel(entity.getRelatedModel());
			dmsRoot.setIsDirectory(true);
			dmsRoot = super.save(dmsRoot); // should get id before it's child
		}

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

		// remove attached file
		if (entity.getMetaFile() != null) {
			final MetaFile metaFile = entity.getMetaFile();
			long count = attachments.all()
					.filter("self.metaFile = ?", metaFile)
					.count();
			if (count == 1) {
				final MetaAttachment attachment = attachments.all()
						.filter("self.metaFile = ?", metaFile)
						.fetchOne();
				attachments.remove(attachment);
			}
			count = all()
					.filter("self.metaFile = ?", metaFile)
					.count();
			if (count == 1) {
				entity.setMetaFile(null);
				try {
					metaFiles.delete(metaFile);
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

		final User user = AuthUtils.getUser();

		boolean canShare = file.getCreatedBy() == user ||
				security.isPermitted(AccessType.CREATE, DMSFile.class, file.getId()) ||
				dmsPermissions.all().filter(
					"self.file = ? AND self.value = 'FULL' AND (self.user = ? OR self.group = ?)",
					file, user, user.getGroup()).count() > 0;

		json.put("typeIcon", isFile ? "fa fa-file" : "fa fa-folder");
		json.put("lastModified", dt);
		json.put("canShare", canShare);

		return json;
	}
}
