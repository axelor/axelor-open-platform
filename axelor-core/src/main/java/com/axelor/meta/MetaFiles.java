/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;

import javax.inject.Inject;
import javax.persistence.PersistenceException;

import com.axelor.app.AppSettings;
import com.axelor.db.EntityHelper;
import com.axelor.db.Model;
import com.axelor.dms.db.DMSFile;
import com.axelor.dms.db.repo.DMSFileRepository;
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

	private static final Path UPLOAD_PATH = Paths.get(AppSettings.get().get("file.upload.dir", DEFAULT_UPLOAD_PATH));
	private static final Path UPLOAD_PATH_TEMP = UPLOAD_PATH.resolve("tmp");

	private static final CopyOption[] COPY_OPTIONS = {
		StandardCopyOption.REPLACE_EXISTING,
		StandardCopyOption.COPY_ATTRIBUTES
	};

	private static final CopyOption[] MOVE_OPTIONS = {
		StandardCopyOption.REPLACE_EXISTING
	};

	// temp clean up threshold 24 hours
	private static final long TEMP_THRESHOLD = 24 * 3600 * 1000;

	private static final Object lock = new Object();

	private MetaFileRepository filesRepo;

	@Inject
	public MetaFiles(MetaFileRepository filesRepo) {
		this.filesRepo = filesRepo;
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
		return UPLOAD_PATH.resolve(file.getFilePath());
	}

	/**
	 * Get the actual storage path of the given relative file path.
	 *
	 * @param filePath
	 *            relative file path
	 * @return actual file path
	 */
	public static Path getPath(String filePath) {
		Preconditions.checkNotNull(filePath, "file path can't be null");
		return UPLOAD_PATH.resolve(filePath);
	}

	/**
	 * Create a temporary file under upload directory.
	 * 
	 * @see Files#createTempFile(String, String, FileAttribute...)
	 */
	public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs) throws IOException {
		// make sure the upload directories exist
		Files.createDirectories(UPLOAD_PATH_TEMP);
		return Files.createTempFile(UPLOAD_PATH_TEMP, prefix, suffix, attrs);
	}

	/**
	 * Find a temporary file by the given name created previously.
	 * 
	 * @param name
	 *            name of the temp file
	 * @return file path
	 */
	public static Path findTempFile(String name) {
		return UPLOAD_PATH_TEMP.resolve(name);
	}

	private Path getNextPath(String fileName) {
		synchronized (lock) {
			int dotIndex = fileName.lastIndexOf('.');
			int counter = 1;
			String fileNameBase = fileName.substring(0, dotIndex);
			String fileNameExt = "";
			if (dotIndex > -1) {
				fileNameExt = fileName.substring(dotIndex);
			}
			String targetName = fileName;
			Path target = UPLOAD_PATH.resolve(targetName);
			while (Files.exists(target)) {
				targetName = fileNameBase + " (" + counter++ + ")" + fileNameExt;
				target = UPLOAD_PATH.resolve(targetName);
			}
			return target;
		}
	}

	/**
	 * Clean up obsolete temporary files from upload directory.
	 *
	 */
	public void clean() throws IOException {
		if (!Files.isDirectory(UPLOAD_PATH_TEMP)) {
			return;
		}
		final long currentTime = System.currentTimeMillis();
		Files.walkFileTree(UPLOAD_PATH_TEMP, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				long diff = currentTime - Files.getLastModifiedTime(file).toMillis();
				if (diff >= TEMP_THRESHOLD) {
					Files.deleteIfExists(file);
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * This method can be used to delete temporary file of an incomplete upload.
	 *
	 * @param fileId
	 *            the upload file id
	 */
	public void clean(String fileId) throws IOException {
		Files.deleteIfExists(UPLOAD_PATH_TEMP.resolve(fileId));
	}

	/**
	 * Upload the given chunk of file data to a temporary file identified by the
	 * given file id.
	 *
	 * <p>
	 * Upload would restart if startOffset is 0 (zero), otherwise upload file
	 * size is checked against given startOffset. The startOffset must be less
	 * than expected fileSize.
	 *
	 * <p>
	 * Unlike the {@link #upload(File, MetaFile)} or {@link #upload(File)}
	 * methods, this method doesn't create {@link MetaFile} instance.
	 *
	 * <p>
	 * The temporary file generated should be manually uploaded again using
	 * {@link #upload(File, MetaFile)} or should be deleted using
	 * {@link #clean(String)} method if something went wrong.
	 *
	 * @param chunk
	 *            the input stream
	 * @param startOffset
	 *            the start offset byte position
	 * @param fileSize
	 *            the actual file size
	 * @param fileId
	 *            an unique upload file identifier
	 * @return a temporary file where upload is being saved
	 * @throws IOException
	 *             if there is any error during io operations
	 */
	public File upload(InputStream chunk, long startOffset, long fileSize, String fileId) throws IOException {
		final Path tmp = UPLOAD_PATH_TEMP.resolve(fileId);
		if ((fileSize > -1 && startOffset > fileSize)
				|| (Files.exists(tmp) && Files.size(tmp) != startOffset)
				|| (!Files.exists(tmp) && startOffset > 0)) {
			throw new IllegalArgumentException("Start offset is out of bound.");
		}

		// make sure the upload directories exist
		Files.createDirectories(UPLOAD_PATH_TEMP);

		// clean up obsolete temporary files
		try {
			clean();
		} catch (Exception e) {
		}

		final File file = tmp.toFile();
		final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, startOffset > 0));
		try {
			int read = 0;
			long total = startOffset;
			byte[] bytes = new byte[4096];
			while ((read = chunk.read(bytes)) != -1) {
				total += read;
				if (fileSize > -1 && total > fileSize) {
					throw new IllegalArgumentException("Invalid chunk, oversized upload.");
				}
				bos.write(bytes, 0, read);
			}
			bos.flush();
		} finally {
			bos.close();
		}

		return file;
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

		final String fileName = isBlank(metaFile.getFileName()) ? file.getName() : metaFile.getFileName();
		final String targetName = update ? metaFile.getFilePath() : fileName;
		final Path path = UPLOAD_PATH.resolve(targetName);
		final Path tmp = update ? createTempFile(null, null) : null;

		if (update && Files.exists(path)) {
			Files.move(path, tmp, MOVE_OPTIONS);
		}

		try {
			final Path source = file.toPath();
			final Path target = getNextPath(fileName);

			// make sure the upload path exists
			Files.createDirectories(UPLOAD_PATH);

			// if source is in tmp directory, move it otherwise copy
			if (UPLOAD_PATH_TEMP.equals(source.getParent())) {
				Files.move(source, target, MOVE_OPTIONS);
			} else {
				Files.copy(source, target, COPY_OPTIONS);
			}

			// only update file name if not provides from meta file
			if (isBlank(metaFile.getFileName())) {
				metaFile.setFileName(file.getName());
			}
			if (isBlank(metaFile.getFileType())) {
				metaFile.setFileType(Files.probeContentType(target));
			}
			metaFile.setFileSize(Files.size(target));
			metaFile.setFilePath(target.toFile().getName());

			try {
				return filesRepo.save(metaFile);
			} catch (Exception e) {
				// delete the uploaded file
				Files.deleteIfExists(target);
				// restore original file
				if (tmp != null) {
					Files.move(tmp, target, MOVE_OPTIONS);
				}
				throw new PersistenceException(e);
			}
		} finally {
			if (tmp != null) {
				Files.deleteIfExists(tmp);
			}
		}
	}

	/**
	 * Upload the given stream to the upload directory and link it to the to
	 * given {@link MetaFile}.
	 *
	 * <p>
	 * The given {@link MetaFile} instance must have fileName set to save the
	 * stream as file.
	 * </p>
	 *
	 * Upload the stream
	 *
	 * @param stream
	 *            the stream to upload
	 * @param metaFile
	 *            the {@link MetaFile} to link the uploaded file
	 * @return the given {@link MetaFile} instance
	 * @throws IOException
	 */
	@Transactional
	public MetaFile upload(InputStream stream, MetaFile metaFile) throws IOException {
		Preconditions.checkNotNull(stream, "stream can't be null");
		Preconditions.checkNotNull(metaFile, "meta file can't be null");
		Preconditions.checkNotNull(metaFile.getFileName(), "meta file should have filename");

		final Path tmp = createTempFile(null, null);
		final File tmpFile = upload(stream, 0, -1, tmp.toFile().getName());

		return upload(tmpFile, metaFile);
	}

	/**
	 * Upload the given stream to the upload directory.
	 *
	 * @param stream
	 *            the stream to upload
	 * @param fileName
	 *            the file name to use
	 * @return a new {@link MetaFile} instance
	 * @throws IOException
	 */
	@Transactional
	public MetaFile upload(InputStream stream, String fileName) throws IOException {
		final MetaFile file = new MetaFile();
		file.setFileName(fileName);
		return upload(stream, file);
	}

	/**
	 * Upload the given file stream and attach it to the given record.
	 *
	 * <p>
	 * The attachment will be saved as {@link DMSFile} and will be visible in
	 * DMS user interface. Use {@link #upload(InputStream, String)} along with
	 * {@link #attach(MetaFile, Model)} if you don't want to show the attachment
	 * in DMS interface.
	 * </p>
	 *
	 * @param stream
	 *            the stream to upload
	 * @param fileName
	 *            the file name to use
	 * @param entity
	 *            the record to attach to
	 * @return a {@link DMSFile} record created for the attachment
	 * @throws IOException
	 */
	@Transactional
	public DMSFile attach(InputStream stream, String fileName, Model entity) throws IOException {
		final MetaFile metaFile = upload(stream, fileName);
		return attach(metaFile, fileName, entity);
	}

	/**
	 * Attach the given file to the given record.
	 *
	 * @param metaFile
	 *            the file to attach
	 * @param fileName
	 *            alternative file name to use (optional, can be null)
	 * @param entity
	 *            the record to attach to
	 * @return a {@link DMSFile} record created for the attachment
	 * @throws IOException
	 */
	@Transactional
	public DMSFile attach(MetaFile metaFile, String fileName, Model entity) throws IOException {
		Preconditions.checkNotNull(metaFile);
		Preconditions.checkNotNull(metaFile.getId());
		Preconditions.checkNotNull(entity);
		Preconditions.checkNotNull(entity.getId());

		final String name = isBlank(fileName) ? metaFile.getFileName() : fileName;
		final DMSFile dmsFile = new DMSFile();
		final DMSFileRepository repository = Beans.get(DMSFileRepository.class);

		dmsFile.setFileName(name);
		dmsFile.setMetaFile(metaFile);
		dmsFile.setRelatedId(entity.getId());
		dmsFile.setRelatedModel(EntityHelper.getEntityClass(entity).getName());

		repository.save(dmsFile);

		return dmsFile;
	}

	/**
	 * Delete the given {@link DMSFile} and also delete linked file if not
	 * referenced by any other record.
	 *
	 * <p>
	 * It will attempt to clean up associated {@link MetaFile} and
	 * {@link MetaAttachment} records and also try to delete linked file from
	 * upload directory.
	 * </p>
	 *
	 * @param file
	 *            the {@link DMSFile} to delete
	 */
	@Transactional
	public void delete(DMSFile file) {
		final DMSFileRepository repository = Beans.get(DMSFileRepository.class);
		repository.remove(file);
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
		attachment.setObjectName(EntityHelper.getEntityClass(entity).getName());

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
		DMSFileRepository dms = Beans.get(DMSFileRepository.class);

		attachments.remove(attachment);

		MetaFile metaFile = attachment.getMetaFile();
		long count = dms.all().filter("self.metaFile = ?", metaFile).count();
		if (count == 0) {
			count = attachments.all()
			.filter("self.metaFile = ? and self.id != ?", metaFile, attachment.getId())
			.count();
		}

		// only delete real file if not reference anywhere else
		if (count > 0) {
			return;
		}

		files.remove(metaFile);

		Path target = UPLOAD_PATH.resolve(metaFile.getFilePath());
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

		Path target = UPLOAD_PATH.resolve(metaFile.getFilePath());
		files.remove(metaFile);

		Files.deleteIfExists(target);
	}

	public String fileTypeIcon(MetaFile file) {
		String fileType = file.getFileType();
		switch (fileType) {
		case "application/msword":
		case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
		case "application/vnd.oasis.opendocument.text":
			return "fa-file-word-o";
		case "application/vnd.ms-excel":
		case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
		case "application/vnd.oasis.opendocument.spreadsheet":
			return "fa-file-excel-o";
		case "application/pdf":
			return "fa-file-pdf-o";
		case "application/zip":
		case "application/gzip":
			return "fa-file-archive-o";
		default:
			if (fileType.startsWith("text")) return "fa-file-text-o";
			if (fileType.startsWith("image")) return "fa-file-image-o";
			if (fileType.startsWith("video")) return "fa-file-video-o";
		}
		return "fa-file-o";
	}
}
