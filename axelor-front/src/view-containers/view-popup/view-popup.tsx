import { memo, useCallback } from "react";

import { ModalDialog } from "@/components/dialogs";
import { Tab, useTabs } from "@/hooks/use-tabs";

import { Views } from "../views";

import styles from "./view-popup.module.scss";

export const PopupViews = memo(function PopupViews({ tab }: { tab: Tab }) {
  const { id, title } = tab;
  const { close } = useTabs();

  const handleClose = useCallback(() => {
    close(id);
  }, [close, id]);

  return (
    <ModalDialog
      id={id}
      title={title}
      size="xl"
      classes={{ content: styles.content }}
      content={<Views tab={tab} />}
      onClose={handleClose}
    />
  );
});
