import { useAtomValue } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { memo, useCallback, useEffect } from "react";

import { Box, Button } from "@axelor/ui";

import { DialogButton, ModalDialog, dialogs } from "@/components/dialogs";
import { Tab, useTabs } from "@/hooks/use-tabs";
import { i18n } from "@/services/client/i18n";

import { Views } from "../views";
import { PopupScope, usePopupHandlerAtom } from "./handler";

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
  return (
    <PopupDialog
      tab={tab}
      open={true}
      footer={(close) => <Footer close={close} />}
      buttons={[]}
      onClose={handleClose}
    />
  );
});

function Footer({ close }: { close: (result: boolean) => void }) {
  const handlerAtom = usePopupHandlerAtom();
  const handler = useAtomValue(handlerAtom);

  const handleCancel = useCallback(() => {
    dialogs.confirmDirty(
      async () => handler.getState?.().dirty ?? false,
      async () => close(false)
    );
  }, [close, handler]);

  const handleConfirm = useCallback(async () => {
    if (handler.getState === undefined) return close(true);
    const state = handler.getState();
    const record = state.record;
    const canSave = state.dirty || !record.id;
    const onSave = handler.onSave;

    try {
      if (canSave) {
        if (onSave) {
          await onSave();
        }
      }
      close(true);
    } catch (e) {
      // TODO: show error
    }
  }, [close, handler]);

  useEffect(() => {
    return handler.actionHandler?.subscribe((data) => {
      if (data.type === "close") {
        close(false);
      }
    });
  }, [close, handleCancel, handleConfirm, handler]);

  return (
    <Box d="flex" g={2}>
      <Button variant="secondary" onClick={handleCancel}>
        {i18n.get("Cancel")}
      </Button>
      <Button variant="primary" onClick={handleConfirm}>
        {i18n.get("OK")}
      </Button>
    </Box>
  );
}
