import { ReactNode } from "react";

import { Box, clsx } from "@axelor/ui";

import { i18n } from "@/services/client/i18n";

import { useFileDrop } from "./use-file-drop";
import styles from "./file-droppable.module.scss";

interface DragOverlayProps {
  className?: string;
  children: ReactNode;
}

type FileDroppableProps = React.ComponentProps<typeof Box> & {
  accept?: string;
  disabled?: boolean;
  onDropFile?: (file: File) => void | Promise<void>;
  renderDragOverlay?: (props: DragOverlayProps) => ReactNode;
};

export function FileDroppable({
  accept,
  disabled,
  onDropFile,
  renderDragOverlay,
  children,
  className,
  ...rest
}: FileDroppableProps) {
  const { isDragging, dropZoneProps } = useFileDrop({
    disabled,
    accept,
    onDropFile,
  });

  const overlayProps: DragOverlayProps = {
    className: clsx(styles.popover),
    children: i18n.get("Drop files to upload"),
  };

  function renderOverlay() {
    if (!isDragging) return null;
    return renderDragOverlay ? (
      renderDragOverlay(overlayProps)
    ) : (
      <Box {...overlayProps} />
    );
  }

  return (
    <Box
      className={clsx(styles.dropZone, className, {
        [styles.dragging]: isDragging,
      })}
      {...dropZoneProps}
      {...rest}
    >
      {children}
      {renderOverlay()}
    </Box>
  );
}
