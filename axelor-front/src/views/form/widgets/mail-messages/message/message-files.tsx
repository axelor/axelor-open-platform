import { ReactElement } from "react";
import { Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { MessageFile } from "./types";
import { download as downloadFile } from "@/utils/download";
import { legacyClassNames } from "@/styles/legacy";
import styles from "./message-files.module.scss";

function download(file: MessageFile) {
  downloadFile(
    `ws/rest/com.axelor.meta.db.MetaFile/${
      file["metaFile.id"] || file["id"]
    }/content/download`,
    file.fileName
  );
}

export function MessageFiles({
  stack,
  data = [],
  showIcon = true,
  onRemove,
}: {
  data?: MessageFile[];
  showIcon?: boolean;
  stack?: boolean;
  onRemove?: (file: MessageFile, index: number) => void;
}) {
  return (data.length > 0 && (
    <Box as="ul" m={1} ms={0} me={0} p={0} className={styles.list}>
      {data.map(($file, ind) => (
        <Box
          as="li"
          d={stack ? "flex" : "inline-flex"}
          p={0}
          ps={1}
          pe={1}
          key={$file.id}
          {...(!stack && {
            alignItems: "center",
          })}
        >
          {onRemove && (
            <Box d="inline-flex" alignItems="center" as="span" me={1}>
              <MaterialIcon
                className={styles.close}
                fill
                icon="close"
                onClick={() => onRemove($file, ind)}
                fontSize={"1rem"}
              />
            </Box>
          )}
          {showIcon && (
            <Box
              as="i"
              me={1}
              className={legacyClassNames(
                "fa",
                $file.typeIcon || $file.fileIcon || "fa-paperclip"
              )}
            />
          )}
          <Box
            as="a"
            className={styles.link}
            onClick={(e) => {
              e.preventDefault();
              download($file);
            }}
          >
            {$file.fileName}
          </Box>
        </Box>
      ))}
    </Box>
  )) as ReactElement;
}
