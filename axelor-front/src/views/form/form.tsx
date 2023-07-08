import clsx from "clsx";
import { useAtom, useAtomValue, useSetAtom, useStore } from "jotai";
import { focusAtom } from "jotai-optics";
import { useAtomCallback } from "jotai/utils";
import {
  MutableRefObject,
  RefObject,
  SyntheticEvent,
  memo,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { Block, Box, CommandItemProps } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { alerts } from "@/components/alerts";
import { dialogs } from "@/components/dialogs";
import { useAsync } from "@/hooks/use-async";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useContainerQuery } from "@/hooks/use-container-query";
import { parseExpression } from "@/hooks/use-parser/utils";
import { usePerms } from "@/hooks/use-perms";
import { useShortcut, useShortcuts } from "@/hooks/use-shortcut";
import { useTabs } from "@/hooks/use-tabs";
import { DataStore } from "@/services/client/data-store";
import { diff, extractDummy } from "@/services/client/data-utils";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { ViewData } from "@/services/client/meta";
import { FormView, Schema } from "@/services/client/meta.types";
import { isAdvancedSearchView } from "@/view-containers/advance-search/utils";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import {
  useSelectViewState,
  useViewAction,
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
  FormAtom,
  Form as FormComponent,
  FormLayout,
  FormState,
  FormWidget,
  WidgetErrors,
  WidgetState,
  useFormHandlers,
} from "./builder";
import { createWidgetAtom } from "./builder/atoms";
import { Collaboration } from "./widgets/collaboration";

import { session } from "@/services/client/session";
import { Formatters } from "@/utils/format";

import styles from "./form.module.scss";

export const fetchRecord = async (
  meta: ViewData<FormView>,
  dataStore: DataStore,
  id?: string | number
) => {
  if (id && +id > 0) {
    const fields = Object.keys(meta.fields ?? {});
    const related = meta.related;
    return dataStore.read(+id, { fields, related });
  }
  return getDefaultValues(meta);
};

export const showErrors = (errors: WidgetErrors[]) => {
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

export const useGetErrors = () => {
  const store = useStore();
  return useCallback(
    (formState: FormState, fieldName?: string) => {
      const { states, statesByName = {} } = formState;
      const isHidden = function isHidden(s: WidgetState): boolean {
        return Boolean(
          s.attrs.hidden ||
            (s.name && statesByName[s.name]?.attrs?.hidden) ||
            (s.parent && isHidden(store.get(s.parent)))
        );
      };
      const errors = Object.values(states)
        .filter((s) => fieldName === undefined || s.name === fieldName)
        .filter((s) => !isHidden(s))
        .filter((s) => Object.keys(s.errors ?? {}).length > 0)
        .map((s) => s.errors ?? {});
      return errors.length ? errors : null;
    },
    [store]
  );
};

export const useHandleFocus = (containerRef: RefObject<HTMLDivElement>) => {
  const handleFocus = useCallback(() => {
    const elem = containerRef.current;
    if (elem) {
      const selector = ["input", "select", "textarea"]
        .map(
          (name) =>
            `${name}[data-input]:not([readonly]), [data-input] ${name}:not([readonly]), ${name}[tabindex]:not([readonly])`
        )
        .join(", ");
      const input = elem.querySelector(selector) as HTMLInputElement;
      focusAndSelectInput(input);
    }
  }, [containerRef]);
  return handleFocus;
};

export const focusAndSelectInput = (input?: null | HTMLInputElement) => {
  if (input) {
    input.focus();
    input.select();
  }
};

function getDefaultValues(meta: ViewData<FormView>) {
  const { fields = {} } = meta;
  const result: DataRecord = Object.entries(fields).reduce(
    (acc, [key, { defaultValue }]) =>
      defaultValue === undefined || key.includes(".")
        ? acc
        : { ...acc, [key]: defaultValue },
    {}
  );
  return result;
}

const timeSymbol = Symbol("$$time");

export function Form(props: ViewProps<FormView>) {
  const { meta, dataStore } = props;

  const { id } = useViewRoute();
  const [viewProps = {}] = useViewProps();
  const { action } = useViewTab();
  const recordRef = useRef<DataRecord | null>(null);

  const { params } = action;
  const recordId = String(id || action.context?._showRecord || "");
  const readonly =
    !params?.forceEdit &&
    (params?.forceReadonly || (viewProps.readonly ?? Boolean(recordId)));

  const popupRecord = params?.["_popup-record"];

  const { state, data: record = {} } = useAsync(async () => {
    const record = recordRef.current;
    if (record) {
      recordRef.current = null;
      if (!recordId || String(record?.id) === recordId) {
        return { ...record };
      }
    }
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
      isLoading={isLoading}
      record={record}
      recordRef={recordRef}
      readonly={readonly}
    />
  );
}

const FormContainer = memo(function FormContainer({
  meta,
  dataStore,
  record,
  recordRef,
  searchAtom,
  isLoading,
  ...props
}: ViewProps<FormView> & {
  record: DataRecord;
  recordRef: MutableRefObject<DataRecord | null>;
  readonly?: boolean;
  isLoading?: boolean;
}) {
  const { view: schema } = meta;
  const {
    onNew: onNewAction,
    onLoad: onLoadAction,
    onSave: onSaveAction,
  } = schema;

  const { id: tabId, popup, popupOptions } = useViewTab();
  const { formAtom, actionHandler, recordHandler, actionExecutor } =
    useFormHandlers(meta, record);

  const { hasButton } = usePerms(meta.view, meta.perms);
  const attachmentItem = useFormAttachment(formAtom);

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
        options?: { readonly?: boolean; isNew?: boolean }
      ) => {
        const id = String(record?.id ?? "");
        const prev = get(formAtom);
        const action = record ? onLoadAction : onNewAction;
        const { isNew, ...props } = { readonly, ...options };
        const isNewFromUnsaved = isNew && record === null && !prev.record.id;

        record = record ?? {};
        record =
          record.id && record.id > 0
            ? record
            : { ...getDefaultValues(prev.meta), ...record };

        // this is required to trigger expression re-evaluation
        record = { ...record, [timeSymbol]: Date.now() };

        if (isNew) {
          recordRef.current = record;
        }

        switchTo("form", { route: { id }, props });
        setDirty(false);
        set(formAtom, {
          ...prev,
          dirty: false,
          states: {},
          statesByName: {},
          record,
          original: { ...record },
        });

        if (action && (!isNew || isNewFromUnsaved)) {
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

        if (!isNew) {
          const event = new CustomEvent("form:refresh", { detail: tabId });
          document.dispatchEvent(event);
        }
      },
      [
        actionExecutor,
        formAtom,
        tabId,
        onLoadAction,
        onNewAction,
        readonly,
        recordRef,
        setDirty,
        switchTo,
      ]
    )
  );

  const onNew = useCallback(async () => {
    dialogs.confirmDirty(
      async () => isDirty,
      async () => {
        doEdit(null, { readonly: false, isNew: true });
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

  const getErrors = useGetErrors();

  const onSave = useAtomCallback(
    useCallback(
      async (
        get,
        set,
        callOnSave: boolean = true,
        shouldSave: boolean = true
      ) => {
        const formState = get(formAtom);
        const errors = getErrors(formState);
        if (errors) {
          showErrors(errors);
          return Promise.reject();
        }

        const { record } = formState;
        const dummy = extractDummy(record);

        if (onSaveAction && callOnSave) {
          await actionExecutor.execute(onSaveAction);
        }

        if (!shouldSave) return record;

        const { record: rec, original = {} } = get(formAtom); // record may have changed by actions
        const vals = diff(rec, original);

        let res = await dataStore.save(vals);
        if (res.id) res = await doRead(res.id);

        res = { ...dummy, ...res }; // restore dummy values

        const isNew = vals.id !== res.id;

        doEdit(res, { readonly, isNew });

        return res;
      },
      [
        actionExecutor,
        dataStore,
        doEdit,
        doRead,
        formAtom,
        getErrors,
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
      doEdit(rec, { readonly: false, isNew: true });
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

  const onAudit = useAtomCallback(
    useCallback(
      async (get, set) => {
        const rec = get(formAtom).record;
        const id = rec.id;
        if (id && id > 0) {
          const res = await dataStore.read(id, {
            fields: ["createdBy", "createdOn", "updatedBy", "updatedOn"],
          });
          const name = session.info?.user.nameField ?? "name";
          dialogs.info({
            content: (
              <dl className={styles.dlist}>
                <dt>{i18n.get("Created By")}</dt>
                <dd>{(res.createdBy || {})[name]}</dd>
                <dt>{i18n.get("Created On")}</dt>
                <dd>{Formatters.datetime(res.createdOn)}</dd>
                <dt>{i18n.get("Updated By")}</dt>
                <dd>{(res.updatedBy || {})[name]}</dd>
                <dt>{i18n.get("Updated On")}</dt>
                <dd>{Formatters.datetime(res.updatedOn)}</dd>
              </dl>
            ),
          });
        }
      },
      [dataStore, formAtom]
    )
  );

  const pagination = usePagination(dataStore, record, readonly);
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
  }, [getState, doEdit, doRead, onSave, popup, setPopupHandlers, actionHandler]);

  const tab = useViewTab();
  const { active, close: closeTab } = useTabs();
  const currentViewType = useSelectViewState(useCallback((x) => x.type, []));

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
      await actionExecutor.waitFor();
      actionExecutor.wait().then(handleOnSave);
    },
    [actionExecutor, handleOnSave]
  );

  const containerRef = useRef<HTMLDivElement>(null);

  const handleFocus = useHandleFocus(containerRef);

  // register shortcuts
  useShortcuts({
    viewType: schema.type,
    onNew: canNew ? onNew : undefined,
    onEdit: canEdit ? onEdit : undefined,
    onSave: canSave ? handleSave : undefined,
    onCopy: canCopy ? onCopy : undefined,
    onDelete: canDelete ? onDelete : undefined,
    onRefresh: onRefresh,
    onFocus: handleFocus,
  });

  const { views = [] } = useViewAction();

  const searchViewType = useMemo(
    () =>
      prevType != null && isAdvancedSearchView(prevType)
        ? prevType
        : views.find((view) => isAdvancedSearchView(view.type))?.type,
    [prevType, views]
  );

  const setSearchFocusTabId = useSetAtom(
    useMemo(
      () => focusAtom(searchAtom!, (o) => o.prop("focusTabId")),
      [searchAtom]
    )
  );

  useShortcut({
    key: "f",
    altKey: true,
    canHandle: useCallback(
      () =>
        active === tab &&
        currentViewType === schema.type &&
        searchViewType != null,
      [searchViewType, active, tab, currentViewType, schema.type]
    ),
    action: useCallback(() => {
      void (async () => {
        await dialogs.confirmDirty(
          async () => isDirty,
          async () => {
            setSearchFocusTabId(tab.id);
            switchTo(searchViewType!);
          }
        );
      })();
    }, [isDirty, searchViewType, setSearchFocusTabId, tab.id, switchTo]),
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
              ...attachmentItem,
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
        >
          <Block className={styles.collaboration}>
            <Collaboration formAtom={formAtom} />
          </Block>
        </ViewToolBar>
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
});

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

  const style: React.CSSProperties = useMemo(() => {
    if (width === "*") return { maxWidth: "unset" };
    if (className) return { minWidth, maxWidth };
    return { width, minWidth, maxWidth };
  }, [className, maxWidth, minWidth, width]);

  const result = useMemo(
    () => (isPopup ? {} : { className, style }),
    [className, isPopup, style]
  );

  return result;
}

export const Layout: FormLayout = ({
  schema,
  formAtom,
  parentAtom,
  className,
  readonly,
}) => {
  const ref = useRef<HTMLDivElement | null>(null);
  const isSmall = useContainerQuery(ref, "width < 768px");

  const { main, side, small } = useMemo(() => {
    const items = schema.items ?? [];
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
            parentAtom={parentAtom}
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
              parentAtom={parentAtom}
              readonly={readonly}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export function useFormAttachment(formAtom: FormAtom) {
  const showDMSPopup = useDMSPopup();
  const [$attachments, setAttachmentCount] = useAtom(
    useMemo(
      () =>
        focusAtom(formAtom, (form) => form.prop("record").prop("$attachments")),
      [formAtom]
    )
  );

  const handleClick = useAtomCallback(
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

  return {
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
    onClick: handleClick,
  } as CommandItemProps;
}
