import { ReactElement, useEffect, useReducer } from "react";
import { Box, Link } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { ProgressComponent } from "@/views/form/widgets/progress";
import { UploadStatus, Uploader } from "./scope";
import { i18n } from "@/services/client/i18n";
import styles from "./dms-upload.module.scss";

export function DmsUpload({ uploader }: { uploader: Uploader }) {
  const [, reload] = useReducer(() => ({}), {});

  useEffect(() => {
    return uploader.subscribe(reload);
  }, [uploader]);

  const hasFailed = uploader.items.some(
    (info) =>
      info.status === UploadStatus.Failed ||
      info.status === UploadStatus.Cancelled,
  );
  const hasActive = uploader.items.some(
    (info) =>
      info.status === UploadStatus.Pending ||
      info.status === UploadStatus.Uploading ||
      info.status === UploadStatus.Uploaded,
  );

  const title =
    uploader.running || hasActive
      ? i18n.get("Uploading files...")
      : hasFailed
        ? i18n.get("Upload failed")
        : i18n.get("Upload complete");

  return (uploader.items.length > 0 && (
    <Box border rounded className={styles.container} bgColor="body">
      <Box d="flex" px={3} py={2} className={styles.title}>
        <Box flex={1}>{title}</Box>
        <Box d="flex" className={styles.close}>
          {!uploader.running && (
            <MaterialIcon onClick={() => uploader.finish()} icon="close" />
          )}
        </Box>
      </Box>
      <Box borderTop>
        {uploader.items.map((info) => {
          const canCancel = [
            UploadStatus.Uploading,
            UploadStatus.Pending,
          ].includes(info.status);
          const canRetry = [
            UploadStatus.Failed,
            UploadStatus.Cancelled,
          ].includes(info.status);

          return (
            <Box
              key={info.uuid}
              d="flex"
              className={styles.item}
              borderBottom
              p={1}
            >
              <Box className={styles.name} px={1}>
                {info.file?.name}
              </Box>
              <Box
                className={styles.progress}
                d="flex"
                alignItems="center"
                px={1}
              >
                <ProgressComponent
                  value={info.progress ?? 0}
                  schema={{ colors: "g:100" }}
                />
              </Box>
              {(canCancel || canRetry) && (
                <Box className={styles.actions} px={1}>
                  {canCancel ? (
                    <Link onClick={() => info?.abort?.()}>
                      {i18n.get("Cancel")}
                    </Link>
                  ) : (
                    <Link onClick={() => info?.retry?.()}>
                      {i18n.get("Retry")}
                    </Link>
                  )}
                </Box>
              )}
            </Box>
          );
        })}
      </Box>
    </Box>
  )) as ReactElement;
}
