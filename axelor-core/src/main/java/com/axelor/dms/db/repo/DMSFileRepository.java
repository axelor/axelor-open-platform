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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.PersistenceException;

import org.apache.shiro.authz.UnauthorizedException;
import org.joda.time.LocalDateTime;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.common.ClassUtils;
import com.axelor.common.Inflector;
import com.axelor.db.JpaRepository;
import com.axelor.db.JpaSecurity;
import com.axelor.db.JpaSecurity.AccessType;
import com.axelor.db.Model;
import com.axelor.db.internal.EntityHelper;
import com.axelor.db.mapper.Mapper;
import com.axelor.dms.db.DMSFile;
import com.axelor.dms.db.DMSFileTag;
import com.axelor.i18n.I18n;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaAttachmentRepository;
import com.axelor.rpc.Resource;
import com.google.common.base.Strings;
import com.google.common.primitives.Longs;

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
		final Model entity = JpaRepository.of(klass).find(file.getRelatedId());
		return EntityHelper.getEntity(entity);
	}

	@Override
	public DMSFile save(DMSFile entity) {

		final DMSFile parent = entity.getParent();
		final Model related = findRelated(entity);
		final boolean isAttachment = related != null && entity.getMetaFile() != null;

		// if new attachment, save attachment reference
		if (isAttachment) {
			// remove old attachment if file is moved
			MetaAttachment attachmentOld = attachments.all()
					.filter("self.metaFile.id = ? AND self.objectId != ? AND self.objectName != ?",
							entity.getMetaFile().getId(),
							related.getId(),
							related.getClass().getName())
					.fetchOne();
			if (attachmentOld != null) {
				System.err.println("OLD: " + attachmentOld);
				attachments.remove(attachmentOld);
			}

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

	private DMSFile findFrom(Map<String, Object> json) {
		if (json == null || json.get("id") == null) {
			return null;
		}
		final Long id = Longs.tryParse(json.get("id").toString());
		return find(id);
	}

	private boolean canCreate(DMSFile parent) {
		final User user = AuthUtils.getUser();
		final Group group = user.getGroup();
		if (parent.getCreatedBy() == user ||
			security.hasRole("role.super") ||
			security.hasRole("role.admin")) {
			return true;
		}
		return dmsPermissions.all()
			.filter("self.file = :file AND self.permission.canWrite = true AND "
					+ "(self.user = :user OR self.group = :group)")
			.bind("file", parent)
			.bind("user", user)
			.bind("group", group)
			.autoFlush(false)
			.count() > 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> validate(Map<String, Object> json, Map<String, Object> context) {
		final DMSFile file = findFrom(json);
		final DMSFile parent = findFrom((Map<String, Object>) json.get("parent"));
		if (parent == null) {
			return json;
		}
		if (file != null && file.getParent() == parent) {
			return json;
		}

		// check whether user can create/move document here
		if (file == null  && !canCreate(parent)) {
			throw new UnauthorizedException(I18n.get("You can't create document here."));
		}
		if (file != null && file.getParent() != parent && !canCreate(parent)) {
			throw new UnauthorizedException(I18n.get("You can't move document here."));
		}

		return json;
	}

	@Override
	public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
		final DMSFile file = findFrom(json);
		if (file == null) {
			return json;
		}

		boolean isFile = file.getIsDirectory() != Boolean.TRUE;
		LocalDateTime dt = file.getUpdatedOn();
		if (dt == null) {
			dt = file.getCreatedOn();
		}

		final User user = AuthUtils.getUser();
		final MetaFile metaFile = file.getMetaFile();

		boolean canShare = file.getCreatedBy() == user ||
				security.isPermitted(AccessType.CREATE, DMSFile.class, file.getId()) ||
				dmsPermissions.all().filter(
					"self.file = ? AND self.value = 'FULL' AND (self.user = ? OR self.group = ?)",
					file, user, user.getGroup()).count() > 0;

		json.put("typeIcon", isFile ? "fa fa-file" : "fa fa-folder");
		json.put("downloadIcon", "fa fa-download");
		json.put("detailsIcon", "fa fa-info-circle");

		json.put("canShare", canShare);
		json.put("canWrite", canCreate(file));

		json.put("lastModified", dt);
		json.put("createdOn", file.getCreatedOn());
		json.put("createdBy", file.getCreatedBy());
		json.put("updatedBy", file.getUpdatedBy());
		json.put("updatedOn", file.getUpdatedOn());

		if (metaFile != null) {
			json.put("fileType", metaFile.getFileType());
		}

		if ("html".equals(file.getContentType())) {
			json.put("fileType", "text/html");
			json.put("contentType", "html");
			json.put("typeIcon", "fa fa-file-text");
			json.remove("downloadIcon");
		}
		if ("spreadsheet".equals(file.getContentType())) {
			json.put("fileType", "text/json");
			json.put("contentType", "spreadsheet");
			json.put("typeIcon", "fa fa-table");
			json.remove("downloadIcon");
		}

		if (file.getTags() != null) {
			final List<Object> tags = new ArrayList<>();
			for (DMSFileTag tag : file.getTags()) {
				tags.add(Resource.toMap(tag, "id", "code", "name", "style"));
			}
			json.put("tags", tags);
		}

		return json;
	}
}
