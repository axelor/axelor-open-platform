import { useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { ScopeProvider } from "jotai-molecules";
import { memo, useCallback, useEffect, useMemo, useRef, useState } from "react";

import { Box, Button } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { DialogButton, ModalDialog, dialogs } from "@/components/dialogs";
import { Tab, useTabs } from "@/hooks/use-tabs";
import { i18n } from "@/services/client/i18n";

import { Views } from "../views";
import { PopupScope, usePopupHandlerAtom } from "./handler";

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
        header={
          <Header
            header={header}
            maximized={maximized}
            expanded={expanded}
            setMaximized={setMaximized}
            setExpanded={setExpanded}
          />
        }
        footer={footer}
        buttons={buttons}
        onClose={onClose}
        maximize={maximized}
      />
    </ScopeProvider>
  );
});

function Header({
  header: HeaderComp,
  maximized,
  expanded,
  setMaximized,
  setExpanded,
}: {
  header?: () => JSX.Element | null;
  maximized: boolean;
  expanded: boolean;
  setMaximized: React.Dispatch<React.SetStateAction<boolean>>;
  setExpanded: React.Dispatch<React.SetStateAction<boolean>>;
}) {
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
      </Box>
    </Box>
  );
}

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

function Footer({
  close,
  params,
}: {
  close: (result: boolean) => void;
  params?: DataRecord;
}) {
  const handlerAtom = usePopupHandlerAtom();
  const handler = useAtomValue(handlerAtom);

  const parentId = useRef<string | null>(null);

  useEffect(() => {
    if (parentId.current) return;
    const elems = [
      document.querySelector(`[data-tab-content][data-tab-active='true']`),
      ...document.querySelectorAll("body > [data-dialog='true']"),
    ];

    elems.pop();

    const elem = elems[elems.length - 1] as HTMLElement;
    const parent =
      elem && (elem.querySelector("[data-view-id]") as HTMLElement);
    if (parent) {
      parentId.current = parent.getAttribute("data-view-id");
    }
  }, []);

  const triggerReload = useCallback(() => {
    if (parentId.current) {
      const detail = parentId.current;
      const event = new CustomEvent("tab:refresh", { detail });
      document.dispatchEvent(event);
    }
  }, []);

  const popupCanConfirm = params?.["show-confirm"] !== false;
  const popupCanSave = params?.["popup-save"] !== false;
  const popupCanReload = params?.popup === "reload";

  const handleCancel = useCallback(() => {
    dialogs.confirmDirty(
      async () => popupCanConfirm && (handler.getState?.().dirty ?? false),
      async () => close(false)
    );
  }, [close, handler, popupCanConfirm]);

  const handleConfirm = useCallback(async () => {
    if (handler.getState === undefined) return close(true);
    const onSave = handler.onSave;

    try {
      if (onSave) {
        await onSave(true, true, false);
      }
      if (popupCanReload) {
        triggerReload();
      }
      close(true);
    } catch (e) {
      // TODO: show error
    }
  }, [close, handler, popupCanReload, triggerReload]);

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
