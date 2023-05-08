import clsx from "clsx";
import { useAtom, useAtomValue, useSetAtom } from "jotai";
import { focusAtom } from "jotai-optics";
import { useAtomCallback } from "jotai/utils";
import {
  SyntheticEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { alerts } from "@/components/alerts";
import { dialogs } from "@/components/dialogs";
import { useAsync } from "@/hooks/use-async";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useContainerQuery } from "@/hooks/use-container-query";
import { usePerms } from "@/hooks/use-perms";
import { useShortcut } from "@/hooks/use-shortcut";
import { useTabs } from "@/hooks/use-tabs";
import { DataStore } from "@/services/client/data-store";
import { diff, extractDummy } from "@/services/client/data-utils";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { ViewData } from "@/services/client/meta";
import { FormView, Schema } from "@/services/client/meta.types";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import {
  useSelectViewState,
  useViewDirtyAtom,
  useViewProps,
  useViewRoute,
  useViewSwitch,
  useViewTab,
  useViewTabRefresh,
} from "@/view-containers/views/scope";

import { useDMSPopup } from "../dms/builder/hooks";
import { ViewProps } from "../types";
import {
  Form as FormComponent,
  FormLayout,
  FormWidget,
  WidgetErrors,
  useFormHandlers,
} from "./builder";
import { createWidgetAtom } from "./builder/atoms";

import { parseExpression } from "@/hooks/use-parser/utils";
import styles from "./form.module.scss";

const fetchRecord = async (
  meta: ViewData<FormView>,
  dataStore: DataStore,
  id?: string | number
) => {
  if (id && +id > 0) {
    const fields = Object.keys(meta.fields ?? {});
    const related = meta.related;
    return dataStore.read(+id, { fields, related });
  }
  return {};
};

const showErrors = (errors: WidgetErrors[]) => {
  const titles = Object.values(errors).flatMap((e) => Object.values(e));
  alerts.error({
    message: (
      <ul>
        {titles.map((title, i) => (
          <li key={i}>{title}</li>
        ))}
      </ul>
    ),
  });
};

export function Form(props: ViewProps<FormView>) {
  const { meta, dataStore } = props;

  const { id } = useViewRoute();
  const [viewProps = {}] = useViewProps();

  const { action } = useViewTab();

  const readonly =
    action.params?.forceReadonly || (viewProps.readonly ?? Boolean(id));
  const recordId = id || action.context?._showRecord;
  const popupRecord = action.params?.["_popup-record"];

  const { state, data: record = {} } = useAsync(async () => {
    if (popupRecord) {
      if (popupRecord._dirty) return popupRecord;
      const res = await fetchRecord(meta, dataStore, popupRecord.id);
      return { ...popupRecord, ...res };
    }
    return await fetchRecord(meta, dataStore, recordId);
  }, [popupRecord, recordId, meta, dataStore]);

  const isLoading = state !== "hasData";

  return (
    <FormContainer
      {...props}
      record={record}
      readonly={readonly}
      isLoading={isLoading}
    />
  );
}

function FormContainer({
  meta,
  dataStore,
  record,
  isLoading,
  ...props
}: ViewProps<FormView> & {
  record: DataRecord;
  readonly?: boolean;
  isLoading?: boolean;
}) {
  const { view: schema } = meta;
  const {
    onNew: onNewAction,
    onLoad: onLoadAction,
    onSave: onSaveAction,
  } = schema;

  const { formAtom, actionHandler, recordHandler, actionExecutor } =
    useFormHandlers(meta, record);

  const { hasButton } = usePerms(meta.view, meta.perms);
  const showDMSPopup = useDMSPopup();
  const [$attachments, setAttachmentCount] = useAtom(
    useMemo(
      () =>
        focusAtom(formAtom, (form) => form.prop("record").prop("$attachments")),
      [formAtom]
    )
  );

  const widgetAtom = useMemo(
    () => createWidgetAtom({ schema, formAtom }),
    [formAtom, schema]
  );

  const { attrs } = useAtomValue(widgetAtom);
  const setAttrs = useSetAtom(widgetAtom);

  const [readonlyExclusive, setReadonlyExclusive] = useState(false);

  useEffect(() => {
    const readonlyIf = schema.readonlyIf;
    if (!readonlyIf) return;
    return recordHandler.subscribe((record) => {
      const value = Boolean(parseExpression(readonlyIf)(record));
      setReadonlyExclusive((prev) => value);
    });
  }, [recordHandler, schema.readonlyIf]);

  const readonly = readonlyExclusive || (attrs.readonly ?? props.readonly);
  const prevType = useSelectViewState(
    useCallback((state) => state.prevType, [])
  );

  const switchTo = useViewSwitch();

  const dirtyAtom = useViewDirtyAtom();
  const isDirty = useAtomValue(dirtyAtom);
  const setDirty = useSetAtom(dirtyAtom);

  const doRead = useCallback(
    async (id: number | string) => {
      return await fetchRecord(meta, dataStore, id);
    },
    [dataStore, meta]
  );

  const doEdit = useAtomCallback(
    useCallback(
      async (
        get,
        set,
        record: DataRecord | null,
        options?: { readonly?: boolean }
      ) => {
        const id = String(record?.id ?? "");
        const prev = get(formAtom);
        const action = record ? onLoadAction : onNewAction;
        const props = { readonly, ...options };
        switchTo("form", { route: { id }, props });
        setDirty(false);
        set(formAtom, {
          ...prev,
          dirty: false,
          states: {},
          statesByName: {},
          record: record ?? {},
          original: record ? { ...record } : {},
        });

        if (action) {
          // execute action
          await actionExecutor.execute(action);

          // fix undefined values set by action
          let { record: current, original = {} } = get(formAtom);
          let changed = false;
          let res = Object.entries(current).reduce(
            (acc, [key, value]) => {
              if (value === undefined && original[key] === null) {
                changed = true;
                acc[key] = null;
              }
              return acc;
            },
            { ...current }
          );

          if (changed) {
            set(formAtom, { ...prev, record: res });
          }
        }
      },
      [
        actionExecutor,
        formAtom,
        onLoadAction,
        onNewAction,
        readonly,
        setDirty,
        switchTo,
      ]
    )
  );

  const onNew = useCallback(async () => {
    dialogs.confirmDirty(
      async () => isDirty,
      async () => {
        doEdit(null, { readonly: false });
      }
    );
  }, [doEdit, isDirty]);

  const onEdit = useCallback(async () => {
    setAttrs((prev) => ({
      ...prev,
      attrs: { ...prev.attrs, readonly: false },
    }));
  }, [setAttrs]);

  const onBack = useAtomCallback(
    useCallback(
      async (get) => {
        const record = get(formAtom).record;
        const recordId = record.id || -1;
        if (readonly || recordId < 0) {
          if (prevType) {
            switchTo(prevType);
          }
        } else {
          setAttrs((prev) => ({
            ...prev,
            attrs: { ...prev.attrs, readonly: true },
          }));
        }
      },
      [formAtom, prevType, readonly, setAttrs, switchTo]
    )
  );

  const onSave = useAtomCallback(
    useCallback(
      async (get, set, callOnSave: boolean = true) => {
        const { record, states } = get(formAtom);
        const errors = Object.values(states)
          .map((s) => s.errors ?? {})
          .filter((x) => Object.keys(x).length > 0);

        if (errors.length > 0) {
          showErrors(errors);
          return Promise.reject();
        }

        const dummy = extractDummy(record);
        if (onSaveAction && callOnSave) {
          await actionExecutor.execute(onSaveAction);
        }

        const { record: rec, original = {} } = get(formAtom); // record may have changed by actions
        const vals = diff(rec, original);

        let res = await dataStore.save(vals);
        if (res.id) res = await doRead(res.id);

        res = { ...dummy, ...res }; // restore dummy values

        doEdit(res, { readonly });

        return res;
      },
      [
        actionExecutor,
        dataStore,
        doEdit,
        doRead,
        formAtom,
        onSaveAction,
        readonly,
      ]
    )
  );

  const handleOnSave = useCallback(async () => {
    try {
      await onSave();
    } catch (error) {
      // TODO: show error notification
    }
  }, [onSave]);

  const onRefresh = useAtomCallback(
    useCallback(
      async (get) => {
        const id = get(formAtom).record.id ?? 0;
        if (id <= 0) {
          return onNew();
        }
        dialogs.confirmDirty(
          async () => isDirty,
          async () => {
            const rec = await doRead(id);
            await doEdit(rec);
          }
        );
      },
      [doEdit, doRead, formAtom, isDirty, onNew]
    )
  );

  actionHandler.setRefreshHandler(onRefresh);
  actionHandler.setSaveHandler(
    useCallback(
      async (record?: DataRecord) => {
        if (record) {
          await dataStore.save(record);
        }
        await onSave(false);
      },
      [dataStore, onSave]
    )
  );

  const onDelete = useAtomCallback(
    useCallback(
      async (get) => {
        const record = get(formAtom).record;
        const id = record.id || 0;
        const version = record.version || 0;
        if (id > 0 && version >= 0) {
          const confirmed = await dialogs.confirm({
            content: i18n.get(
              "Do you really want to delete the selected record?"
            ),
            yesTitle: i18n.get("Delete"),
          });
          if (confirmed) {
            await dataStore.delete({ id, version });
            if (prevType) {
              switchTo(prevType);
            } else {
              onNew();
            }
          }
        }
      },
      [dataStore, formAtom, onNew, prevType, switchTo]
    )
  );

  const onCopy = useCallback(async () => {
    if (record.id) {
      const rec = await dataStore.copy(record.id);
      doEdit(rec, { readonly: false });
    }
  }, [dataStore, doEdit, record.id]);

  const onArchive = useCallback(async () => {
    if (record.id) {
      const confirmed = await dialogs.confirm({
        content: i18n.get("Do you really want to archive the selected record?"),
      });
      if (confirmed) {
        const id = record.id!;
        const version = record.version!;
        await dataStore.save({ id, version, archived: true });
        switchTo("grid");
      }
    }
  }, [dataStore, record.id, record.version, switchTo]);

  const onAudit = useAtomCallback(useCallback(async (get, set) => {}, []));

  const onAttachment = useAtomCallback(
    useCallback(
      (get) => {
        const { record, model, fields } = get(formAtom);
        return showDMSPopup({
          record,
          model,
          fields,
          onCountChanged: (totalCount) => setAttachmentCount(totalCount),
        });
      },
      [formAtom, showDMSPopup, setAttachmentCount]
    )
  );

  const pagination = usePagination(dataStore, record, readonly);

  const { popup, popupOptions } = useViewTab();
  const popupHandlerAtom = usePopupHandlerAtom();
  const setPopupHandlers = useSetAtom(popupHandlerAtom);

  const showToolbar = popupOptions?.showToolbar !== false;

  const getState = useAtomCallback(
    useCallback((get) => get(formAtom), [formAtom])
  );

  const doOnLoad = useAtomCallback(
    useCallback(
      async (get) => {
        if (isLoading) return;
        const rec = get(formAtom).record;
        const recId = rec.id ?? 0;
        const action = recId > 0 ? onLoadAction : onNewAction;

        if (action) {
          await actionExecutor.execute(action);
        }
      },
      [actionExecutor, formAtom, isLoading, onLoadAction, onNewAction]
    )
  );

  useAsyncEffect(doOnLoad, [doOnLoad]);

  useEffect(() => {
    if (popup) {
      setPopupHandlers({
        getState,
        onSave,
        onEdit: doEdit,
        onRead: doRead,
        actionHandler,
      });
    }
  }, [
    getState,
    doEdit,
    doRead,
    onSave,
    popup,
    setPopupHandlers,
    actionHandler,
  ]);

  const tab = useViewTab();
  const { close: closeTab } = useTabs();

  useEffect(() => {
    if (popup) return;
    return actionHandler.subscribe((data) => {
      if (data.type === "close") {
        closeTab(tab.action);
      }
    });
  }, [actionHandler, closeTab, isDirty, popup, tab.action]);

  const canNew = hasButton("new");
  const canEdit = readonly && !readonlyExclusive && hasButton("edit");
  const canSave = !readonly && hasButton("save");
  const canDelete = hasButton("delete") && record.id;
  const canCopy = !isDirty && canNew && hasButton("copy") && record.id;
  const canArchive = hasButton("archive") && record.id;
  const canAudit = hasButton("log") && record.id;
  const canAttach = hasButton("attach") && record.id;

  const containerRef = useRef<HTMLDivElement>(null);
  const handleFocus = useCallback(() => {
    const elem = containerRef.current;
    if (elem) {
      const selector = ["input", "select", "textarea"]
        .map(
          (name) =>
            `${name}[data-input]:not([readonly]), [data-input] ${name}:not([readonly])`
        )
        .join(", ");
      const input = elem.querySelector(selector) as HTMLInputElement;
      if (input) {
        input.focus();
        input.select();
      }
    }
  }, []);

  const handleSave = useCallback(
    async (e?: SyntheticEvent) => {
      const onSaveClick = e?.type === "click";
      if (!onSaveClick) {
        const input = document.activeElement as HTMLInputElement;
        const elem = containerRef.current;

        // fake click to trigger outside click logic of any editable row
        elem?.click?.();

        if (input && elem?.contains(input)) {
          input.blur?.();
          input.focus?.();
        }
      }
      await new Promise<void>((resolve) => {
        setTimeout(() => resolve(), 50);
      });
      actionExecutor.wait().then(handleOnSave);
    },
    [actionExecutor, handleOnSave]
  );

  // register shortcuts
  useShortcuts({
    onNew: canNew ? onNew : undefined,
    onEdit: canEdit ? onEdit : undefined,
    onSave: canSave ? handleSave : undefined,
    onDelete: canDelete ? onDelete : undefined,
    onRefresh: onRefresh,
    onFocus: handleFocus,
    onPrev: pagination.canPrev ? pagination.onPrev : undefined,
    onNext: pagination.canNext ? pagination.onNext : undefined,
  });

  // register tab:refresh
  useViewTabRefresh("form", onRefresh);

  return (
    <div className={styles.formViewContainer}>
      {showToolbar && (
        <ViewToolBar
          meta={meta}
          actionExecutor={actionExecutor}
          recordHandler={recordHandler}
          actions={[
            {
              key: "new",
              text: i18n.get("New"),
              hidden: !canNew,
              iconProps: {
                icon: "add",
              },
              onClick: onNew,
            },
            {
              key: "edit",
              text: i18n.get("Edit"),
              hidden: !canEdit,
              iconProps: {
                icon: "edit",
              },
              onClick: onEdit,
            },
            {
              key: "save",
              text: i18n.get("Save"),
              iconProps: {
                icon: "save",
              },
              onClick: handleSave,
              hidden: !canSave,
            },
            {
              key: "cancel",
              text: i18n.get("Cancel"),
              iconProps: {
                icon: "refresh",
              },
              onClick: onRefresh,
              hidden: !isDirty,
            },
            {
              key: "back",
              text: i18n.get("Back"),
              iconProps: {
                icon: "arrow_back",
              },
              onClick: onBack,
              hidden: isDirty,
            },
            {
              key: "attachment",
              text: i18n.get("Attachment"),
              icon: (props: any) => (
                <Box as="span" d="flex" position="relative">
                  <MaterialIcon icon="attach_file" {...props} />
                  {$attachments ? (
                    <Box d="flex" as="small" alignItems="flex-end">
                      {$attachments}
                    </Box>
                  ) : null}
                </Box>
              ),
              iconProps: {
                icon: "attach_file",
              },
              onClick: onAttachment,
              hidden: !canAttach,
            },
            {
              key: "more",
              iconOnly: true,
              iconProps: {
                icon: "arrow_drop_down",
              },
              items: [
                {
                  key: "refresh",
                  text: i18n.get("Refresh"),
                  onClick: onRefresh,
                },
                {
                  key: "delete",
                  text: i18n.get("Delete"),
                  disabled: !canDelete,
                  iconProps: {
                    icon: "delete",
                  },
                  onClick: onDelete,
                },
                {
                  key: "copy",
                  text: i18n.get("Duplicate"),
                  onClick: onCopy,
                  disabled: !canCopy,
                },
                {
                  key: "s1",
                  divider: true,
                  hidden: !canArchive,
                },
                {
                  key: "archive",
                  text: i18n.get("Archive"),
                  onClick: onArchive,
                  hidden: !canArchive,
                },
                {
                  key: "s2",
                  divider: true,
                  hidden: !canAudit,
                },
                {
                  key: "audit",
                  text: i18n.get("Last modified..."),
                  onClick: onAudit,
                  hidden: !canAudit,
                },
              ],
            },
          ]}
          pagination={pagination}
        />
      )}
      <div className={styles.formViewScroller} ref={containerRef}>
        <FormComponent
          className={styles.formView}
          readonly={readonly}
          schema={meta.view}
          fields={meta.fields!}
          formAtom={formAtom}
          recordHandler={recordHandler}
          actionHandler={actionHandler}
          actionExecutor={actionExecutor}
          layout={Layout}
          widgetAtom={widgetAtom}
        />
      </div>
    </div>
  );
}

function useShortcuts({
  onNew,
  onEdit,
  onSave,
  onDelete,
  onRefresh,
  onFocus,
  onNext,
  onPrev,
}: {
  onNew?: () => void;
  onEdit?: () => void;
  onSave?: () => void;
  onDelete?: () => void;
  onRefresh?: () => void;
  onFocus?: () => void;
  onPrev?: () => void;
  onNext?: () => void;
}) {
  const { active } = useTabs();
  const tab = useViewTab();
  const type = useSelectViewState(useCallback((x) => x.type, []));

  const canHandle = useCallback(
    () => active === tab && type === "form",
    [active, tab, type]
  );

  useShortcut({
    key: "Insert",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onNew?.(), [onNew]),
  });

  useShortcut({
    key: "e",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onEdit?.(), [onEdit]),
  });

  useShortcut({
    key: "s",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onSave?.(), [onSave]),
  });

  useShortcut({
    key: "d",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onDelete?.(), [onDelete]),
  });

  useShortcut({
    key: "r",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onRefresh?.(), [onRefresh]),
  });

  useShortcut({
    key: "g",
    altKey: true,
    canHandle,
    action: useCallback(() => onFocus?.(), [onFocus]),
  });

  useShortcut({
    key: "j",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onPrev?.(), [onPrev]),
  });

  useShortcut({
    key: "k",
    ctrlKey: true,
    canHandle,
    action: useCallback(() => onNext?.(), [onNext]),
  });
}

function usePagination(
  dataStore: DataStore,
  record: DataRecord,
  readonly?: boolean
) {
  const { offset = 0, limit = 0, totalCount = 0 } = dataStore.page;
  const index = dataStore.records.findIndex((x) => x.id === record.id);

  const switchTo = useViewSwitch();

  const onPrev = useCallback(async () => {
    let prev = dataStore.records[index - 1];
    if (prev === undefined) {
      const { records = [] } = await dataStore.search({
        offset: offset - limit,
      });
      prev = records[records.length - 1];
    }
    const id = String(prev.id);
    const props = { readonly };
    switchTo("form", { route: { id }, props });
  }, [dataStore, index, limit, offset, readonly, switchTo]);

  const onNext = useCallback(async () => {
    let next = dataStore.records[index + 1];
    if (next === undefined) {
      const { records = [] } = await dataStore.search({
        offset: offset + limit,
      });
      next = records[0];
    }
    const id = String(next.id);
    const props = { readonly };
    switchTo("form", { route: { id }, props });
  }, [dataStore, index, limit, offset, readonly, switchTo]);

  const canPrev = index > -1 && offset + index > 0;
  const canNext = index > -1 && offset + index < totalCount - 1;

  const text =
    index > -1 ? i18n.get("{0} of {1}", offset + index + 1, totalCount) : "";

  return {
    canPrev,
    canNext,
    onPrev,
    onNext,
    text,
  };
}

function useFormWidth(
  schema: Schema,
  hasSide: boolean,
  isPopup: boolean = false
) {
  const { width, minWidth, maxWidth } = schema;

  const className = useMemo(
    () => styles[width] ?? (hasSide ? undefined : styles.mid),
    [hasSide, width]
  );

  const style = useMemo(() => {
    const result = {
      width,
      minWidth,
      maxWidth,
    } as React.CSSProperties;
    if (width === "*" || className) delete result.width;
    return result;
  }, [className, maxWidth, minWidth, width]);

  const result = useMemo(
    () => (isPopup ? {} : { className, style }),
    [className, isPopup, style]
  );

  return result;
}

const Layout: FormLayout = ({ schema, formAtom, className, readonly }) => {
  const ref = useRef<HTMLDivElement | null>(null);
  const isSmall = useContainerQuery(ref, "width < 768px");

  const { main, side, small } = useMemo(() => {
    const items = [...(schema.items ?? [])];
    const head = items.filter((x) => x.widget === "wkf-status");
    const side = items.filter((x) => x.sidebar);
    const mail = items.filter((x) => x.type === "panel-mail");
    const rest = items.filter(
      (x) => !head.includes(x) && !side.includes(x) && !mail.includes(x)
    );

    const sideTop = side.slice(0, 1);
    const sideAll = side.slice(1);

    const small = [...head, ...sideTop, ...rest, ...sideAll, ...mail];
    const main = [...head, ...rest, ...mail];

    return {
      main,
      side,
      small,
    };
  }, [schema]);

  const mainItems = isSmall ? small : main;
  const sideItems = isSmall ? [] : side;

  const { popup } = useViewTab();
  const { style, className: clsName } = useFormWidth(
    schema,
    side.length > 0,
    popup
  );

  return (
    <div
      className={clsx(className, clsName, styles.formLayout, {
        [styles.small]: isSmall,
      })}
      style={style}
      ref={ref}
    >
      <div className={styles.main}>
        {mainItems.map((item) => (
          <FormWidget
            key={item.uid}
            schema={item}
            formAtom={formAtom}
            readonly={readonly}
          />
        ))}
      </div>
      {sideItems.length > 0 && (
        <div className={styles.side}>
          {side.map((item) => (
            <FormWidget
              key={item.uid}
              schema={item}
              formAtom={formAtom}
              readonly={readonly}
            />
          ))}
        </div>
      )}
    </div>
  );
};
