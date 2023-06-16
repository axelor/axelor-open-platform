import { ReactNode } from "react";
import { Box } from "@axelor/ui";
import { useDrop } from "react-dnd";
import { NativeTypes } from "react-dnd-html5-backend";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { i18n } from "@/services/client/i18n";
import styles from "./dms-overlay.module.scss";

export function DmsOverlay({
  children,
  className,
  onUpload,
}: {
  children: ReactNode;
  className?: string;
  onUpload?: (files: FileList | null) => void;
}) {
  // file drag/drop upload
  const [{ canDrop, isOver }, drop] = useDrop({
    accept: [NativeTypes.FILE],
    drop(item, monitor: any) {
      const files = monitor.getItem().files;
      onUpload?.(files);
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      canDrop: monitor.canDrop(),
    }),
  });

  const showOverlay = canDrop && isOver;

  return (
    <Box ref={drop} className={className}>
      {showOverlay && (
        <Box className={styles.container}>
          <Box
            className={styles.content}
            d="flex"
            flexDirection="column"
            bgColor="body"
            border
          >
            <Box>
              <MaterialIcon fontSize={"2.5rem"} icon="upload" />
            </Box>
            <Box>{i18n.get("Drop your files to start upload.")}</Box>
          </Box>
        </Box>
      )}
      {children}
    </Box>
  );
}
