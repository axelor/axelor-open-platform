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
package com.axelor.meta;

import static com.axelor.common.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.persistence.PersistenceException;

import com.axelor.app.AppSettings;
import com.axelor.db.Model;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaAttachmentRepository;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.google.common.base.Preconditions;
import com.google.inject.persist.Transactional;

/**
 * This class provides some helper methods to deal with files.
 *
 */
public class MetaFiles {

	private static final String DEFAULT_UPLOAD_PATH = "{java.io.tmpdir}/axelor/attachments";
	private static final String UPLOAD_PATH = AppSettings.get().getPath("file.upload.dir", DEFAULT_UPLOAD_PATH);

	public MetaFiles() {

	}

	/**
	 * Get the actual storage path of the file represented by the give
	 * {@link MetaFile} instance.
	 *
	 * @param file
	 *            the given {@link MetaFile} instance
	 * @return actual file path
	 */
	public static Path getPath(MetaFile file) {
		Preconditions.checkNotNull(file, "file instance can't be null");
		return Paths.get(UPLOAD_PATH, file.getFilePath());
	}

	private Path getNextPath(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		int counter = 1;
		String fileNameBase = fileName.substring(0, dotIndex);
		String fileNameExt = "";
		if (dotIndex > -1) {
			fileNameExt = fileName.substring(dotIndex);
		}
		String targetName = fileName;
		Path target = Paths.get(UPLOAD_PATH, targetName);
		while (Files.exists(target)) {
			targetName = fileNameBase + " (" + counter++ + ")" + fileNameExt;
			target = Paths.get(UPLOAD_PATH, targetName);
		}
		return target;
	}

	/**
	 * Upload the given file to the file upload directory and create an instance
	 * of {@link MetaFile} for the given file.
	 *
	 * @param file
	 *            the given file
	 * @return an instance of {@link MetaFile}
	 * @throws IOException
	 *             if unable to read the file
	 * @throws PersistenceException
	 *             if unable to save to a {@link MetaFile} instance
	 */
	@Transactional
	public MetaFile upload(File file) throws IOException {
		return upload(file, new MetaFile());
	}

	/**
	 * Upload the given {@link File} to the upload directory and link it to the
	 * to given {@link MetaFile}.
	 *
	 * <p>
	 * Any existing file linked to the given {@link MetaFile} will be removed
	 * from the upload directory.
	 * </p>
	 *
	 * @param file
	 *            the file to upload
	 * @param metaFile
	 *            the target {@link MetaFile} instance
	 * @return persisted {@link MetaFile} instance
	 * @throws IOException
	 *             if unable to read the file
	 * @throws PersistenceException
	 *             if unable to save to {@link MetaFile} instance
	 */
	@Transactional
	public MetaFile upload(File file, MetaFile metaFile) throws IOException {
		Preconditions.checkNotNull(metaFile);
		Preconditions.checkNotNull(file);

		final boolean update = !isBlank(metaFile.getFilePath());
		final String targetName = update ? metaFile.getFilePath() : file.getName();
		final Path path = Paths.get(UPLOAD_PATH, targetName);
		final Path tmp = update ? Files.createTempFile(null, null) : null;

		if (update && Files.exists(path)) {
			Files.move(path, tmp, StandardCopyOption.REPLACE_EXISTING);
		}

		try {
			final Path source = file.toPath();
			final Path target = getNextPath(targetName);

			// make sure the upload path exists
			Files.createDirectories(Paths.get(UPLOAD_PATH));

			// copy the file to upload directory
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

			final String mime = Files.probeContentType(file.toPath());

			metaFile.setFileName(file.getName());
			metaFile.setMime(mime);
			metaFile.setSize(Files.size(file.toPath()));
			metaFile.setFilePath(target.toFile().getName());

			final MetaFileRepository repo = Beans.get(MetaFileRepository.class);
			try {
				return repo.save(metaFile);
			} catch (Exception e) {
				// delete the uploaded file
				Files.deleteIfExists(target);
				throw new PersistenceException(e);
			}
		} finally {
			if (tmp != null) {
				Files.deleteIfExists(tmp);
			}
		}
	}


	/**
	 * Attach the given {@link MetaFile} to the given {@link Model} object and
	 * return an instance of a {@link MetaAttachment} that represents the
	 * attachment.
	 * <p>
	 * The {@link MetaAttachment} instance is not persisted.
	 * </p>
	 *
	 * @param file
	 *            the given {@link MetaFile} instance
	 * @param entity
	 *            the given {@link Model} instance
	 * @return a new instance of {@link MetaAttachment}
	 */
	public MetaAttachment attach(MetaFile file, Model entity) {
		Preconditions.checkNotNull(file);
		Preconditions.checkNotNull(entity);
		Preconditions.checkNotNull(entity.getId());

		MetaAttachment attachment = new MetaAttachment();
		attachment.setMetaFile(file);
		attachment.setObjectId(entity.getId());
		attachment.setObjectName(entity.getClass().getName());

		return attachment;
	}

	/**
	 * Delete the given attachment & related {@link MetaFile} instance along
	 * with the file content.
	 *
	 * @param attachment
	 *            the attachment to delete
	 * @throws IOException if unable to delete file
	 */
	@Transactional
	public void delete(MetaAttachment attachment) throws IOException {
		Preconditions.checkNotNull(attachment);
		Preconditions.checkNotNull(attachment.getMetaFile());

		MetaAttachmentRepository attachments = Beans.get(MetaAttachmentRepository.class);
		MetaFileRepository files = Beans.get(MetaFileRepository.class);

		MetaFile metaFile = attachment.getMetaFile();
		Path target = Paths.get(UPLOAD_PATH, metaFile.getFilePath());

		attachments.remove(attachment);
		files.remove(metaFile);

		Files.deleteIfExists(target);
	}

	/**
	 * Delete the given {@link MetaFile} instance along with the file content.
	 *
	 * @param file
	 *            the file to delete
	 * @throws IOException
	 *             if unable to delete file
	 */
	@Transactional
	public void delete(MetaFile metaFile) throws IOException {
		Preconditions.checkNotNull(metaFile);
		MetaFileRepository files = Beans.get(MetaFileRepository.class);

		Path target = Paths.get(UPLOAD_PATH, metaFile.getFilePath());
		files.remove(metaFile);

		Files.deleteIfExists(target);
	}
}
