import { cloneElement } from "react";

import { Box, ClickAwayListener, Popper, usePopperTrigger } from "@axelor/ui";

import styles from "./tooltip.module.scss";

export type TooltipProps = {
  children: React.ReactElement;
  title?: string;
  content: () => JSX.Element | null;
};

export function Tooltip({ children, title, content: Content }: TooltipProps) {
  const { open, targetEl, setTargetEl, setContentEl, onClickAway } =
    usePopperTrigger({
      trigger: "hover",
      interactive: true,
    });
  return (
    <>
      {cloneElement(children, {
        ref: setTargetEl,
      })}
      <Popper
        open={open}
        target={targetEl}
        offset={[0, 4]}
        placement="top"
        arrow
        shadow
      >
        <ClickAwayListener onClickAway={onClickAway}>
          <Box ref={setContentEl} className={styles.container}>
            {title && <Box className={styles.title}>{title}</Box>}
            <Box className={styles.content}>
              <Content />
            </Box>
          </Box>
        </ClickAwayListener>
      </Popper>
    </>
  );
}
