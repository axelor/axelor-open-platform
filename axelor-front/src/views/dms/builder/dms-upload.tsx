import { ReactElement, useEffect, useReducer } from "react";
import { Box, Link } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { ProgressComponent } from "@/views/form/widgets/progress";
import { Uploader } from "./scope";
import { i18n } from "@/services/client/i18n";
import styles from "./dms-upload.module.scss";

export function DmsUpload({ uploader }: { uploader: Uploader }) {
  const [, reload] = useReducer(() => ({}), {});

  useEffect(() => {
    return uploader.subscribe(reload);
  }, [uploader]);

  return (uploader.items.length > 0 && (
    <Box border rounded className={styles.container} bgColor="body">
      <Box d="flex" px={3} py={2} className={styles.title}>
        <Box flex={1}>
          {uploader.running
            ? i18n.get("Uploading files...")
            : i18n.get("Upload complete")}
        </Box>
        <Box d="flex" className={styles.close}>
          {!uploader.running && (
            <MaterialIcon onClick={() => uploader.finish()} icon="close" />
          )}
        </Box>
      </Box>
      <Box borderTop>
        {uploader.items.map((info, ind) => (
          <Box key={ind} d="flex" className={styles.item} borderBottom p={1}>
            <Box className={styles.name}>{info.file?.name}</Box>
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
            {!info.loaded && (
              <Box className={styles.actions}>
                {info.pending ? (
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
        ))}
      </Box>
    </Box>
  )) as ReactElement;
}
