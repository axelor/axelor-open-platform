import { ReactElement } from "react";
import { Box } from "@axelor/ui";
import { MessageFile } from "./types";
import { download as downloadFile } from "@/utils/download";
import clsx from "clsx";
import styles from "./message-menu.module.scss";

function download(file: MessageFile) {
  downloadFile(
    `ws/rest/com.axelor.meta.db.MetaFile/${
      file["metaFile.id"] || file["id"]
    }/content/download`,
    file.fileName
  );
}

export function MessageFiles({ data = [] }: { data?: MessageFile[] }) {
  return (data.length > 0 && (
    <Box as="ul" m={1} ms={0} me={0} p={0} className={styles.list}>
      {data.map(($file) => (
        <Box as="li" d="inline-block" p={0} ps={1} pe={1} key={$file.id}>
          <Box
            as="i"
            me={1}
            className={clsx("fa", $file.fileIcon || "fa-paperclip")}
          />
          <Box
            as="a"
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
