import clsx from "clsx";
import { useAtomValue, useSetAtom } from "jotai";
import { useAtomCallback } from "jotai/utils";
import { useCallback, useEffect, useMemo, useRef } from "react";

import { dialogs } from "@/components/dialogs";
import { useAsync } from "@/hooks/use-async";
import { useContainerQuery } from "@/hooks/use-container-query";
import { DataStore } from "@/services/client/data-store";
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
} from "@/view-containers/views/scope";

import { ViewProps } from "../types";
import {
  Form as FormComponent,
  FormLayout,
  FormWidget,
  WidgetErrors,
  useFormHandlers,
} from "./builder";
import { createWidgetAtom } from "./builder/atoms";

import { alerts } from "@/components/alerts";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { extractDummy } from "@/services/client/data-utils";
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
    action.params?.forceReadonly ?? viewProps.readonly ?? Boolean(id);
  const recordId = id ?? action.context?._showRecord;
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

  const widgetAtom = useMemo(
    () => createWidgetAtom({ schema, formAtom }),
    [formAtom, schema]
  );

  const { attrs } = useAtomValue(widgetAtom);
  const setAttrs = useSetAtom(widgetAtom);

  const readonly = attrs.readonly ?? props.readonly;
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
        });
        if (action) {
          await actionExecutor.execute(action);
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
      async (get) => {
        const { record, states } = get(formAtom);
        const errors = Object.values(states)
          .map((s) => s.errors ?? {})
          .filter((x) => Object.keys(x).length > 0);

        if (errors.length > 0) {
          showErrors(errors);
          return Promise.reject();
        }

        const dummy = extractDummy(record);
        if (onSaveAction) await actionExecutor.execute(onSaveAction);
        let res = await dataStore.save(record);
        if (res.id) res = await doRead(res.id);
        res = { ...res, ...dummy }; // restore dummy values
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
        dialogs.confirmDirty(
          async () => isDirty,
          async () => {
            const cur = get(formAtom).record;
            const rec = await doRead(cur.id ?? "");
            await doEdit(rec);
          }
        );
      },
      [doEdit, doRead, formAtom, isDirty]
    )
  );

  const onDelete = useCallback(async () => {
    if (record.id) {
      const confirmed = await dialogs.confirm({
        content: i18n.get("Do you really want to delete the selected record?"),
      });
      if (confirmed) {
        const id = record.id!;
        const version = record.version!;
        await dataStore.delete({ id, version });
        switchTo("grid");
      }
    }
  }, [dataStore, record.id, record.version, switchTo]);

  const onCopy = useCallback(async () => {
    if (record.id) {
      const rec = await dataStore.copy(record.id);
      doEdit(rec);
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

  const pagination = usePagination(dataStore, record, doEdit);

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
      });
    }
  }, [getState, doEdit, doRead, onSave, popup, setPopupHandlers]);

  return (
    <div className={styles.formViewContainer}>
      {showToolbar && (
        <ViewToolBar
          meta={meta}
          actions={[
            {
              key: "new",
              text: i18n.get("New"),
              iconProps: {
                icon: "add",
              },
              onClick: onNew,
            },
            {
              key: "edit",
              text: i18n.get("Edit"),
              iconProps: {
                icon: "edit",
              },
              onClick: onEdit,
              hidden: !readonly,
            },
            {
              key: "save",
              text: i18n.get("Save"),
              iconProps: {
                icon: "save",
              },
              onClick: handleOnSave,
              hidden: readonly,
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
                  iconProps: {
                    icon: "delete",
                  },
                  onClick: onDelete,
                },
                {
                  key: "copy",
                  text: i18n.get("Duplicate"),
                  onClick: onCopy,
                  disabled: isDirty,
                },
                {
                  key: "s1",
                  divider: true,
                },
                {
                  key: "archive",
                  text: i18n.get("Archive"),
                  onClick: onArchive,
                },
                {
                  key: "s2",
                  divider: true,
                },
                {
                  key: "audit",
                  text: i18n.get("Last modified..."),
                  onClick: onAudit,
                },
              ],
            },
          ]}
          pagination={pagination}
        />
      )}
      <div className={styles.formViewScroller}>
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

function usePagination(
  dataStore: DataStore,
  record: DataRecord,
  doEdit: (rec: DataRecord) => any
) {
  const { offset = 0, limit = 0, totalCount = 0 } = dataStore.page;
  const index = dataStore.records.findIndex((x) => x.id === record.id);

  const onPrev = useCallback(async () => {
    let prev = dataStore.records[index - 1];
    if (prev === undefined) {
      const { records = [] } = await dataStore.search({
        offset: offset - limit,
      });
      prev = records[records.length - 1];
    }
    doEdit(prev);
  }, [dataStore, doEdit, index, limit, offset]);

  const onNext = useCallback(async () => {
    let next = dataStore.records[index + 1];
    if (next === undefined) {
      const { records = [] } = await dataStore.search({
        offset: offset + limit,
      });
      next = records[0];
    }
    doEdit(next);
  }, [dataStore, doEdit, index, limit, offset]);

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
