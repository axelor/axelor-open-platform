import { useAtomValue } from "jotai";
import { useAtomCallback } from "jotai/utils";
import { uniqueId } from "lodash";
import { useCallback, useEffect, useRef } from "react";

import { Box, Button } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { openTab_internal as openTab } from "@/hooks/use-tabs";
import { getActiveTabId } from "@/layout/nav-tabs/utils";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { findView } from "@/services/client/meta-cache";
import { ActionView, FormView, Schema } from "@/services/client/meta.types";
import { showPopup } from "@/view-containers/view-popup";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { useViewTab } from "@/view-containers/views/scope";
import { showErrors, useGetErrors } from "@/views/form";
import { useFormScope } from "@/views/form/builder/scope";

import { initTab } from "../use-tabs";

export type EditorOptions = {
  model: string;
  title: string;
  record?: DataRecord | null;
  readonly?: boolean;
  maximize?: boolean;
  view?: FormView;
  viewName?: string;
  context?: DataContext;
  canSave?: boolean;
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
      const { view } = await findView<FormView>({
        type: "form",
        name: formView,
        model,
      });
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
      canSave = true,
      onClose,
      onSave,
      onSelect,
    } = options;
    const tab = await initTab({
      name: uniqueId("$selector"),
      title,
      model,
      viewType: "form",
      views: [{ type: "form", name: viewName, ...view }],
      params: {
        popup: true,
        forceEdit: !readonly,
        "show-toolbar": false,
        "_popup-record": context?._parent
          ? {
              ...record,
              _parent: context._parent,
            }
          : record,
      },
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
      footer: (close) => (
        <Footer
          hasOk={canSave}
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
  hasOk = true,
  onClose,
  onSave,
  onSelect,
}: {
  hasOk?: boolean;
  onClose: (result: boolean) => void;
  onSave?: EditorOptions["onSave"];
  onSelect?: EditorOptions["onSelect"];
}) {
  const handlerAtom = usePopupHandlerAtom();
  const handler = useAtomValue(handlerAtom);

  const getErrors = useGetErrors();

  const handleClose = useCallback(() => {
    dialogs.confirmDirty(
      async () => handler.getState?.().dirty ?? false,
      async () => onClose(false),
    );
  }, [handler, onClose]);

  const handleConfirm = useCallback(async () => {
    if (handler.getState === undefined) return onClose(true);
    const state = handler.getState();
    const record = state.record;
    const canSave = state.dirty || !record.id;

    try {
      const errors = getErrors(state);
      if (errors) {
        showErrors(errors);
        return;
      }

      if (canSave) {
        if (onSave) {
          onSave(record);
        } else if (onSelect && handler.onSave) {
          const rec = await handler.onSave({
            shouldSave: true,
            callOnSave: true,
            callOnLoad: false,
          });
          onSelect(rec);
        }
      }

      onClose(true);
    } catch (e) {
      // TODO: show error
    }
  }, [getErrors, handler, onClose, onSave, onSelect]);

  useEffect(() => {
    return handler.actionHandler?.subscribe(async (data) => {
      const { actionExecutor } = handler;
      await actionExecutor?.wait();
      if (data.type === "close") {
        onClose(true);
      }
    });
  }, [handler, onClose]);

  return (
    <Box d="flex" g={2}>
      <Button variant="secondary" onClick={handleClose}>
        {i18n.get("Close")}
      </Button>
      {hasOk && (
        <Button variant="primary" onClick={handleConfirm}>
          {i18n.get("OK")}
        </Button>
      )}
    </Box>
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
        record ? record.version !== originalVersion : result;

      const reloadOptions: Partial<EditorOptions> = {};
      let selected = false;

      if (popupCanReload) {
        reloadOptions.onSelect = (record) => {
          selected = true;
          onSelect?.(record);
        };

        reloadOptions.onClose = (result, record) => {
          const detail = parentId.current;
          if (detail && (selected || isChanged(result, record))) {
            const event = new CustomEvent("tab:refresh", { detail });
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
