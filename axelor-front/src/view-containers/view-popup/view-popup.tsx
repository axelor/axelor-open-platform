import { atom, useAtom, useAtomValue } from "jotai";
import { ScopeProvider } from "bunshi/react";
import { selectAtom } from "jotai/utils";
import { memo, useCallback, useEffect, useMemo, useRef, useState } from "react";

import { clsx, Box, Button, useClassNames } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import {
  DialogButton,
  DialogOptions,
  ModalDialog,
  dialogs,
} from "@/components/dialogs";
import { Tab, useTabs } from "@/hooks/use-tabs";
import { getActiveTabId } from "@/layout/nav-tabs/utils";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";

import { Views } from "../views";
import { PopupHandler, PopupScope, usePopupHandlerAtom } from "./handler";

import { showErrors } from "@/views/form";
import { handlePopper } from "./view-popper";

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
  header?: DialogOptions["header"];

  /**
   * Additional footer component
   *
   */
  footer?: DialogOptions["footer"];

  /**
   * Whether to show header.
   *
   */
  showHeader?: DialogOptions["showHeader"];

  /**
   * Whether to show footer.
   */
  showFooter?: DialogOptions["showFooter"];

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

export const PopupDialog = memo(function PopupDialog(props: PopupProps) {
  return (
    <ScopeProvider scope={PopupScope} value={{}}>
      <PopupDialogInner {...props} />
    </ScopeProvider>
  );
});

const PopupDialogInner = memo(function PopupDialog({
  tab,
  open,
  maximize,
  onClose,
  showHeader = true,
  showFooter = true,
  header,
  footer,
  handler,
  buttons,
}: PopupProps) {
  const title = useAtomValue(
    useMemo(() => selectAtom(tab.state, (x) => x.title), [tab.state]),
  );
  const [maximized, setMaximized] = useState<boolean>(maximize ?? false);
  const [expanded, setExpanded] = useState<boolean>(true);

  useEffect(() => handlePopper(), []);

  return (
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
      showHeader={showHeader}
      showFooter={showFooter}
      header={({ close }) => (
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
  );
});

export const PopupViews = memo(function PopupViews({ tab }: { tab: Tab }) {
  const { id, action } = tab;
  const params = action?.params ?? {};

  const showHeader = params["popup.show-header"] !== false;
  const showFooter = params["popup.show-footer"] !== false;
  const maximize = params["popup.maximized"];

  const { close } = useTabs();
  const handleClose = useCallback(() => close(id), [close, id]);

  return (
    <PopupDialog
      tab={tab}
      open={true}
      footer={({ close }) => <Footer close={close} params={action.params} />}
      buttons={[]}
      onClose={handleClose}
      maximize={maximize}
      showHeader={showHeader}
      showFooter={showFooter}
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
  header?: PopupProps["header"];
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
      async () => handleClose(),
    );
  }, [handleClose, handler, popupCanConfirm]);

  return (
    <Box d="flex" g={2}>
      {HeaderComp && <HeaderComp close={close} />}
      <Box d="flex" g={2} alignItems="center">
        <MaterialIcon
          icon={expanded ? "keyboard_arrow_up" : "keyboard_arrow_down"}
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
  const [handler, setHandler] = useAtom(handlerAtom);
  const handleClose = useClose(handler, close, params);

  const popupCanConfirm = params?.["show-confirm"] !== false;
  const popupCanSave = params?.["popup-save"] !== false;

  const getHandlerState = handler.getState;
  const handleCancel = useCallback(() => {
    dialogs.confirmDirty(
      async () => popupCanConfirm && (getHandlerState?.().dirty ?? false),
      async () => handleClose(),
    );
  }, [handleClose, getHandlerState, popupCanConfirm]);

  const handleConfirm = useCallback(async () => {
    const { getState, getErrors, commitForm, onSave } = handler;

    await commitForm?.();

    await handler.actionExecutor?.waitFor();
    await handler.actionExecutor?.wait();

    const state = getState?.();

    const errors = getErrors?.();
    if (errors) {
      showErrors(errors);
      return;
    }

    const { dirty, record } = state ?? {};
    const canSave = dirty || !record?.id;

    try {
      let rec: DataRecord | undefined = record;
      if (canSave && onSave) {
        rec = await onSave({
          shouldSave: true,
          callOnSave: true,
          callOnRead: false,
        });
      }
      handleClose(rec);
    } catch (e) {
      // TODO: show error
    }
  }, [handleClose, handler]);
  
  useEffect(() => {
    setHandler((popup) => ({ ...popup, close: handleCancel }));
  }, [handleCancel, setHandler]);

  useEffect(() => {
    return handler.actionHandler?.subscribe(async (data) => {
      const { actionExecutor, getState } = handler;
      await actionExecutor?.wait();
      if (data.type === "close") {
        handleClose(getState?.()?.record);
      }
    });
  }, [handleClose, handler]);

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
  params?: DataRecord,
) {
  const readyAtom = useMemo(
    () => handler.readyAtom ?? atom<boolean | undefined>(undefined),
    [handler.readyAtom],
  );
  const ready = useAtomValue(readyAtom);
  const originalRef = useRef<DataRecord>();
  const getHandlerState = handler.getState;

  useEffect(() => {
    const record = getHandlerState?.().record;
    if (originalRef.current == null && ready && record) {
      originalRef.current = { ...record };
    }
  }, [getHandlerState, ready]);

  const parentId = useRef<string | null>(null);

  useEffect(() => {
    if (!parentId.current) {
      parentId.current = getActiveTabId(1);
    }
  }, []);

  const shouldReload = useCallback(
    (record?: DataRecord) => {
      if (record) return true;
      const current = getHandlerState?.().record;
      const original = originalRef.current;
      return (
        !current ||
        !original ||
        current?.id !== original?.id ||
        current?.version !== original?.version
      );
    },
    [getHandlerState],
  );

  const triggerReload = useCallback(() => {
    if (params?.__onPopupReload) {
      params?.__onPopupReload?.();
    } else if (parentId.current) {
      const event = new CustomEvent("tab:refresh", {
        detail: {
          id: parentId.current,
          forceReload: true,
        },
      });
      document.dispatchEvent(event);
    }
  }, [params]);

  const handleClose = useCallback(
    (record?: DataRecord) => {
      const popupCanReload = params?.popup === "reload";
      const reload = shouldReload(record);
      if (popupCanReload && reload) {
        triggerReload();
      }
      close(reload);
    },
    [close, shouldReload, triggerReload, params?.popup],
  );

  return handleClose;
}
