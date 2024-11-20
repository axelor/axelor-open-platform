import { Box, Portal } from "@axelor/ui";
import { PropsWithChildren } from "react";

import styles from "./slider.module.scss";

export function SliderTooltip({
  children,
  position,
  isVisible,
}: PropsWithChildren<{
  position: { left: number; top: number };
  isVisible: boolean;
}>) {
  return (
    <Portal container={document.body}>
      <Box
        className={styles.tooltip}
        bgColor="primary"
        color="white"
        style={{
          left: position.left,
          top: position.top,
          opacity: isVisible ? 1 : 0,
        }}
      >
        {children}
      </Box>
    </Portal>
  );
}
