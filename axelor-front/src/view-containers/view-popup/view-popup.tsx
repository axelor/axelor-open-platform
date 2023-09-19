import { atom, useAtomValue } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { selectAtom } from "jotai/utils";
import { memo, useCallback, useEffect, useMemo, useRef, useState } from "react";

import { Box, Button, useClassNames } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { DialogButton, ModalDialog, dialogs } from "@/components/dialogs";
import { Tab, useTabs } from "@/hooks/use-tabs";
import { getActiveTabId } from "@/layout/nav-tabs/utils";
import { i18n } from "@/services/client/i18n";

import { Views } from "../views";
import { PopupHandler, PopupScope, usePopupHandlerAtom } from "./handler";

import { DataRecord } from "@/services/client/data.types";
import clsx from "clsx";
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
   * @param record
   */
  onClose: (result: boolean, record?: DataRecord) => void;

  /**
   * Additional header component
   *
   */
  header?: () => JSX.Element | null;

  /**
   * Additional footer component
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

  /**
   * Whether to maximize the popup or not
   */
  maximize?: boolean;
};

export const PopupDialog = memo(function PopupDialog({
  tab,
  open,
  maximize,
  onClose,
  header,
  footer,
  handler,
  buttons,
}: PopupProps) {
  const title = useAtomValue(
    useMemo(() => selectAtom(tab.state, (x) => x.title), [tab.state])
  );
  const [maximized, setMaximized] = useState<boolean>(maximize ?? false);
  const [expanded, setExpanded] = useState<boolean>(true);

  return (
    <ScopeProvider scope={PopupScope} value={{}}>
      <ModalDialog
        open={open}
        title={title || tab.title}
        size="xl"
        closeable={false}
        classes={{
          root: clsx({
            [styles.collapsed]: !expanded,
          }),
          footer: styles.footer,
          content: styles.content,
        }}
        content={
          <>
            <Views tab={tab} />
            {handler?.()}
          </>
        }
        header={(close) => (
          <Header
            header={header}
            maximized={maximized}
            expanded={expanded}
            params={tab.action?.params}
            close={close}
            setMaximized={setMaximized}
            setExpanded={setExpanded}
          />
        )}
        footer={footer}
        buttons={buttons}
        onClose={onClose}
        maximize={maximized}
      />
    </ScopeProvider>
  );
});

export const PopupViews = memo(function PopupViews({ tab }: { tab: Tab }) {
  const { id, action } = tab;
  const params = action?.params ?? {};
  const { close } = useTabs();
  const handleClose = useCallback(() => close(id), [close, id]);
  return (
    <PopupDialog
      tab={tab}
      open={true}
      footer={(close) => <Footer close={close} params={action.params} />}
      buttons={[]}
      onClose={handleClose}
      maximize={params["popup.maximized"]}
    />
  );
});

function Header({
  header: HeaderComp,
  maximized,
  expanded,
  params,
  close,
  setMaximized,
  setExpanded,
}: {
  header?: () => JSX.Element | null;
  params?: DataRecord;
  maximized: boolean;
  expanded: boolean;
  close: (result: boolean) => void;
  setMaximized: React.Dispatch<React.SetStateAction<boolean>>;
  setExpanded: React.Dispatch<React.SetStateAction<boolean>>;
}) {
  const classNames = useClassNames();
  const handlerAtom = usePopupHandlerAtom();
  const handler = useAtomValue(handlerAtom);
  const handleClose = useClose(handler, close, params);

  const popupCanConfirm = params?.["show-confirm"] !== false;

  const onClose = useCallback(() => {
    dialogs.confirmDirty(
      async () => popupCanConfirm && (handler.getState?.().dirty ?? false),
      async () => handleClose(false)
    );
  }, [handleClose, handler, popupCanConfirm]);

  return (
    <Box d="flex" g={2}>
      {HeaderComp && <HeaderComp />}
      <Box d="flex" alignItems="center">
        <MaterialIcon
          icon={expanded ? "expand_less" : "expand_more"}
          className={styles.icon}
          onClick={() => setExpanded((prev) => !prev)}
        />
        <MaterialIcon
          icon={maximized ? "fullscreen_exit" : "fullscreen"}
          className={styles.icon}
          onClick={() => setMaximized((prev) => !prev)}
        />
        <Box
          as="button"
          tabIndex={0}
          className={classNames("btn-close")}
          onClick={onClose}
        />
      </Box>
    </Box>
  );
}

function Footer({
  close,
  params,
}: {
  close: (result: boolean) => void;
  params?: DataRecord;
}) {
  const handlerAtom = usePopupHandlerAtom();
  const handler = useAtomValue(handlerAtom);
  const handleClose = useClose(handler, close, params);

  const popupCanConfirm = params?.["show-confirm"] !== false;
  const popupCanSave = params?.["popup-save"] !== false;

  const handleCancel = useCallback(() => {
    dialogs.confirmDirty(
      async () => popupCanConfirm && (handler.getState?.().dirty ?? false),
      async () => handleClose(false)
    );
  }, [handleClose, handler, popupCanConfirm]);

  const handleConfirm = useCallback(async () => {
    const { getState, onSave } = handler;

    try {
      if (getState && onSave) {
        await onSave(true, true, false);
      }
      handleClose(true);
    } catch (e) {
      // TODO: show error
    }
  }, [handleClose, handler]);

  useEffect(() => {
    return handler.actionHandler?.subscribe((data) => {
      if (data.type === "close") {
        handleClose(true);
      }
    });
  }, [handleClose, handler.actionHandler]);

  return (
    <Box d="flex" g={2}>
      <Button variant="secondary" onClick={handleCancel}>
        {i18n.get("Close")}
      </Button>
      {popupCanSave && (
        <Button variant="primary" onClick={handleConfirm}>
          {i18n.get("OK")}
        </Button>
      )}
    </Box>
  );
}

function useClose(
  handler: PopupHandler,
  close: (result: boolean) => void,
  params?: DataRecord
) {
  const readyAtom = useMemo(
    () => handler.readyAtom ?? atom<boolean | undefined>(undefined),
    [handler.readyAtom]
  );
  const ready = useAtomValue(readyAtom);

  const originalVersion = useRef<number | null>();

  useEffect(() => {
    if (ready && originalVersion.current == null) {
      originalVersion.current = handler.getState?.().record.version;
    }
  }, [handler, ready]);

  const parentId = useRef<string | null>(null);

  useEffect(() => {
    if (!parentId.current) {
      parentId.current = getActiveTabId(1);
    }
  }, []);

  const shouldReload = useCallback(() => {
    const popupCanReload = params?.popup === "reload";
    return (
      popupCanReload &&
      handler.getState?.().record.version !== originalVersion.current
    );
  }, [handler, params?.popup]);

  const triggerReload = useCallback(() => {
    if (parentId.current) {
      const detail = parentId.current;
      const event = new CustomEvent("tab:refresh", { detail });
      document.dispatchEvent(event);
    }
  }, []);

  const handleClose = useCallback(
    (result: boolean) => {
      if (shouldReload()) {
        triggerReload();
      }
      close(result);
    },
    [close, shouldReload, triggerReload]
  );

  return handleClose;
}
