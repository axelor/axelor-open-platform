import { ScopeProvider } from "jotai-molecules";
import { memo, useCallback } from "react";

import { DialogButton, ModalDialog } from "@/components/dialogs";
import { Tab, useTabs } from "@/hooks/use-tabs";

import { Views } from "../views";
import { PopupScope } from "./handler";

import styles from "./view-popup.module.scss";

export type PopupProps = {
  /**
   * View tab to show as popup
   *
   */
  tab: Tab;

  /**
   * Whether the popup is open or not
   *
   */
  open: boolean;

  /**
   * This callback is called when the popup is closed
   *
   * @param result confirm or cancel
   */
  onClose: (result: boolean) => void;

  /**
   * Additional header component
   *
   */
  header?: () => JSX.Element | null;

  /**
   * Additioanl footer component
   *
   */
  footer?: (close: (result: boolean) => void) => JSX.Element | null;

  /**
   * Additional handler component
   *
   * Generally, it should not render anything but handle
   * popup operations using `usePopupOptions` hook only.
   *
   */
  handler?: () => JSX.Element | null;

  /**
   * Override dialog buttons
   *
   */
  buttons?: DialogButton[];
};

export const PopupDialog = memo(function PopupDialog({
  tab,
  open,
  onClose,
  header,
  footer,
  handler,
  buttons,
}: PopupProps) {
  const { title } = tab;
  return (
    <ScopeProvider scope={PopupScope} value={{}}>
      <ModalDialog
        open={open}
        title={title}
        size="xl"
        classes={{ content: styles.content }}
        content={
          <>
            <Views tab={tab} />
            {handler?.()}
          </>
        }
        header={header?.()}
        footer={footer}
        buttons={buttons}
        onClose={onClose}
      />
    </ScopeProvider>
  );
});

export const PopupViews = memo(function PopupViews({ tab }: { tab: Tab }) {
  const { id } = tab;
  const { close } = useTabs();
  const handleClose = useCallback(() => close(id), [close, id]);
  return <PopupDialog tab={tab} open={true} onClose={handleClose} />;
});
