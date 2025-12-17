import { useAtom } from "jotai";
import { useAtomCallback } from "jotai/utils";
import isEqual from "lodash/isEqual";
import uniqueId from "lodash/uniqueId";
import { useCallback, useEffect, useRef } from "react";

import { Box, Button, CommandBar } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { openTab_internal as openTab } from "@/hooks/use-tabs";
import { getActiveTabId } from "@/layout/nav-tabs/utils";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { findView } from "@/services/client/meta-cache";
import { ActionView, FormView, Schema } from "@/services/client/meta.types";
import { PopupProps, showPopup } from "@/view-containers/view-popup";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { useViewTab } from "@/view-containers/views/scope";
import { showErrors } from "@/views/form";
import { useAfterActions, useFormScope } from "@/views/form/builder/scope";

import { initTab } from "../use-tabs";
import { useSingleClickHandler } from "../use-button";

export type EditorOptions = {
  model: string;
  title: string;
  record?: DataRecord | null;
  readonly?: boolean;
  maximize?: boolean;
  view?: FormView;
  viewName?: string;
  context?: DataContext;
  canAttach?: boolean;
  canSave?: boolean;
  params?: ActionView["params"];
  header?: PopupProps["header"];
  footer?: PopupProps["footer"];
  onClose?: (result: boolean, record?: DataRecord) => void;
  onSave?: (record: DataRecord) => Promise<DataRecord> | void;
  onSelect?: (record: DataRecord) => void;
};

export function useEditorInTab(schema: Schema) {
  const {
    target,
    formView,
    gridView,
    widgetAttrs,
    widget,
    editWindow = widgetAttrs?.editWindow || "popup",
  } = schema;
  const tab = useViewTab();

  const handleEdit = useCallback(
    async (record: DataRecord, readonly = false) => {
      const model = target;
      const { view } =
        (await findView<FormView>({
          type: "form",
          name: formView,
          model,
        })) || {};
      return openTab({
        title: view?.title || "",
        name: uniqueId("$act"),
        model,
        viewType: "form",
        views: [
          { name: formView, type: "form" },
          {
            type: "grid",
            name: gridView,
          },
        ],
        params: {
          forceEdit: !readonly,
        },
        context: {
          _showRecord: record.id,
          __check_version: tab.action?.context?.__check_version,
        },
      });
    },
    [formView, gridView, target, tab.action],
  );

  if (!tab.popup && (editWindow === "blank" || widget === "ref-link")) {
    return handleEdit;
  }

  return null;
}

export function useEditor() {
  return useCallback(async (options: EditorOptions) => {
    const {
      title,
      model,
      record,
      view,
      viewName,
      context,
      readonly,
      maximize,
      canAttach = true,
      canSave = true,
      params,
      header,
      footer,
      onClose,
      onSave,
      onSelect,
    } = options;

    const tabParams = {
      "show-toolbar": false,
      ...params,
      popup: true,
      forceEdit: !readonly,
      "_popup-record": record,
    };
    const tab = await initTab({
      name: uniqueId("$selector"),
      title,
      model,
      viewType: "form",
      views: [{ type: "form", name: viewName, ...view }],
      params: tabParams,
      context: {
        _showRecord: record?.id,
        ...context,
      },
    });

    if (!tab) return;

    await showPopup({
      tab,
      open: true,
      maximize,
      onClose: (result, record) => onClose?.(result, record),
      header,
      footer: ({ close }) => (
        <Footer
          footer={footer}
          canAttach={canAttach && (record?.id ?? 0) > 0}
          hasOk={canSave}
          params={tabParams}
          onClose={close}
          onSave={onSave}
          onSelect={onSelect}
        />
      ),
      buttons: [],
    });
  }, []);
}

function Footer({
  canAttach = true,
  hasOk = true,
  footer: FooterComp,
  params,
  onClose,
  onSave,
  onSelect,
}: {
  canAttach?: boolean;
  hasOk?: boolean;
  footer?: EditorOptions["footer"];
  onClose: (result: boolean) => void;
  onSave?: EditorOptions["onSave"];
  onSelect?: EditorOptions["onSelect"];
  params?: ActionView["params"];
}) {
  const popupCanConfirm = params?.["show-confirm"] !== false;
  const popupCanSave = params?.["popup-save"] !== false;
  const popupRecord = params?.["_popup-record"];

  const hasToMany = Boolean(onSave); // o2m, m2m grid
  const handlerAtom = usePopupHandlerAtom();
  const [handler, setHandler] = useAtom(handlerAtom);

  const getHandlerState = handler.getState;
  const handleClose = useCallback(() => {
    dialogs.confirmDirty(
      async () => popupCanConfirm && (getHandlerState?.().dirty ?? false),
      async () => onClose(false),
    );
  }, [getHandlerState, popupCanConfirm, onClose]);

  const handleConfirm = useAfterActions(
    useAtomCallback(
      useCallback(
        async (get) => {
          if (handler.getState === undefined) return onClose(true);

          await handler.commitForm?.();

          await handler.actionExecutor?.waitFor();
          await handler.actionExecutor?.wait();

          const state = handler.getState();
          const { original, record } = state;

          const hasRecordChanged = () => !isEqual(original, record);

          const isNew = popupRecord && !popupRecord?.id;
          const dirtyAtom = handler.dirtyAtom;
          const dirty = (dirtyAtom && get(dirtyAtom)) || state.dirty;
          const canSave = dirty || isNew || (hasToMany && hasRecordChanged());

          try {
            const errors = handler.getErrors?.();
            if (errors) {
              showErrors(errors);
              return;
            }

            if (canSave) {
              if (onSave) {
                onSave({ ...record, _dirty: state.dirty });
              } else if (onSelect && handler.onSave) {
                const rec = await handler.onSave({
                  shouldSave: true,
                  callOnSave: true,
                  callOnRead: false,
                });
                onSelect(rec);
              }
            }

            onClose(true);
          } catch (e) {
            // TODO: show error
            console.error(e);
          }
        },
        [handler, hasToMany, popupRecord, onClose, onSave, onSelect],
      ),
    ),
  );

  const handleOk = useSingleClickHandler(handleConfirm);

  useEffect(() => {
    setHandler((popup) => ({ ...popup, close: handleClose }));
  }, [setHandler, handleClose]);

  useEffect(() => {
    return handler.actionHandler?.setCloseHandler(async () => {
      const { actionExecutor } = handler;
      await actionExecutor?.wait();
      onClose(true);
    });
  }, [handler, onClose]);

  const { attachmentItem } = handler;

  return (
    <>
      {canAttach && attachmentItem && (
        <CommandBar items={[attachmentItem]} iconOnly />
      )}
      <Box d="flex" flex={1} justifyContent="flex-end" g={2}>
        {FooterComp && <FooterComp close={onClose} />}
        <Box d="flex" g={2}>
          <Button variant="secondary" onClick={handleClose}>
            {i18n.get("Close")}
          </Button>
          {hasOk && popupCanSave && (
            <Button variant="primary" onClick={handleOk}>
              {i18n.get("OK")}
            </Button>
          )}
        </Box>
      </Box>
    </>
  );
}

export function useManyEditor(action: ActionView, dashlet?: boolean) {
  const { formAtom, actionHandler } = useFormScope();

  const confirmSave = useAtomCallback(
    useCallback(
      async (get) => {
        const { dirty = false, record, model } = get(formAtom);
        const saveNeeded = Boolean(model) && (dirty || !record.id);

        if (!saveNeeded) {
          return true;
        }

        try {
          const confirmed = await dialogs.confirmSave(
            async () => saveNeeded,
            async () => actionHandler.save(),
          );

          return confirmed;
        } catch (e) {
          return false;
        }
      },
      [formAtom, actionHandler],
    ),
  );

  const parentId = useRef<string | null>(null);

  useEffect(() => {
    if (!parentId.current) {
      parentId.current = getActiveTabId();
    }
  }, []);

  const showEditor = useEditor();

  const popup = action.params?.popup;

  return useCallback(
    async (options: EditorOptions & { onSearch?: () => void }) => {
      const { record, readonly, onSelect, onSearch, ...rest } = options;
      const popupCanReload = dashlet && popup === "reload";

      if (popupCanReload && !readonly && !(await confirmSave())) {
        return;
      }

      const originalVersion = record?.version;

      const isChanged = (result: boolean, record?: DataRecord) =>
        result || record?.version !== originalVersion;

      const reloadOptions: Partial<EditorOptions> = {};
      let selected = false;

      if (popupCanReload) {
        reloadOptions.onSelect = (record) => {
          selected = true;
          onSelect?.(record);
        };

        reloadOptions.onClose = (result, record) => {
          const tabId = parentId.current;
          if (tabId && (selected || isChanged(result, record))) {
            const event = new CustomEvent("tab:refresh", {
              detail: {
                id: tabId,
                forceReload: true,
              },
            });
            document.dispatchEvent(event);
          }
        };
      } else {
        if (!readonly) {
          reloadOptions.onSelect = (record) => {
            selected = true;
            onSelect && record ? onSelect(record) : onSearch?.();
          };
        }

        reloadOptions.onClose = (result, record) => {
          if (!selected && isChanged(result, record)) {
            onSelect && record ? onSelect(record) : onSearch?.();
          }
        };
      }

      return showEditor({
        ...rest,
        record,
        readonly,
        ...reloadOptions,
      });
    },
    [confirmSave, showEditor, popup, dashlet],
  );
}
