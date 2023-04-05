import clsx from "clsx";
import { useAtomCallback } from "jotai/utils";
import { useCallback, useEffect, useMemo, useRef } from "react";

import { dialogs } from "@/components/dialogs";
import { useAsync } from "@/hooks/use-async";
import { useContainerQuery } from "@/hooks/use-container-query";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { FormView } from "@/services/client/meta.types";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import { useViewRoute, useViewSwitch } from "@/view-containers/views/scope";

import { ViewProps } from "../types";
import {
  Form as FormComponent,
  FormLayout,
  FormWidget,
  useFormHandlers,
} from "./builder";
import { fallbackWidgetAtom } from "./builder/atoms";

import { DataStore } from "@/services/client/data-store";
import styles from "./form.module.scss";

export function Form({ meta, dataStore }: ViewProps<FormView>) {
  const { id } = useViewRoute("form");

  const fetchRecord = useCallback(async (): Promise<DataRecord> => {
    if (id) {
      const fields = Object.keys(meta.fields ?? {});
      const related = meta.related;
      return dataStore.read(+id, {
        fields,
        related,
      });
    }
    return {};
  }, [dataStore, id, meta.fields, meta.related]);

  const { data: record = {} } = useAsync(fetchRecord, [id, meta, dataStore]);

  const { formAtom, actionHandler, actionExecutor } = useFormHandlers(
    meta,
    record
  );

  const editRef = useRef<DataRecord | null>(null);

  const switchTo = useViewSwitch();

  const doEdit = useAtomCallback(
    useCallback(
      async (get, set, record: DataRecord | null) => {
        const lastId = id;
        const nextId = String(record?.id ?? "");

        editRef.current = record;

        if (nextId !== lastId) {
          switchTo({
            mode: "edit",
            id: nextId,
          });
        }

        set(formAtom, (prev) => ({
          ...prev,
          states: {},
          record: record ?? {},
        }));
      },
      [formAtom, id, switchTo]
    )
  );

  useEffect(() => {
    if (id) return;
    doEdit(editRef.current);
  }, [doEdit, id]);

  const onNew = useAtomCallback(
    useCallback(
      async (get, set) => {
        doEdit(null);
      },
      [doEdit]
    )
  );

  const onSave = useAtomCallback(
    useCallback(
      async (get, set) => {
        const rec = get(formAtom).record;
        const res = await dataStore.save(rec);
        doEdit(res);
      },
      [dataStore, doEdit, formAtom]
    )
  );

  const onRefresh = useAtomCallback(
    useCallback(
      async (get, set) => {
        const rec = await fetchRecord();
        await doEdit(rec);
      },
      [doEdit, fetchRecord]
    )
  );

  const onDelete = useAtomCallback(
    useCallback(
      async (get, set) => {
        if (record.id) {
          const confirmed = await dialogs.confirm({
            content: i18n.get(
              "Do you really want to delete the selected record?"
            ),
          });
          if (confirmed) {
            const id = record.id!;
            const version = record.version!;
            await dataStore.delete({ id, version });
            switchTo({ mode: "list" });
          }
        }
      },
      [dataStore, record.id, record.version, switchTo]
    )
  );

  const onCopy = useAtomCallback(
    useCallback(
      async (get, set) => {
        if (record.id) {
          const rec = await dataStore.copy(record.id);
          doEdit(rec);
        }
      },
      [dataStore, doEdit, record.id]
    )
  );

  const onArchive = useAtomCallback(
    useCallback(
      async (get, set) => {
        if (record.id) {
          const confirmed = await dialogs.confirm({
            content: i18n.get(
              "Do you really want to archive the selected record?"
            ),
          });
          if (confirmed) {
            const id = record.id!;
            const version = record.version!;
            await dataStore.save({ id, version, archived: true });
            switchTo({ mode: "list" });
          }
        }
      },
      [dataStore, record.id, record.version, switchTo]
    )
  );

  const onAudit = useAtomCallback(useCallback(async (get, set) => {}, []));

  const pagination = usePagination(dataStore, record, doEdit);

  return (
    <div className={styles.formViewContainer}>
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
            key: "save",
            text: i18n.get("Save"),
            iconProps: {
              icon: "save",
            },
            onClick: onSave,
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
      <div className={styles.formViewScroller}>
        <FormComponent
          className={styles.formView}
          schema={meta.view}
          fields={meta.fields!}
          record={record}
          formAtom={formAtom}
          actionHandler={actionHandler}
          actionExecutor={actionExecutor}
          layout={Layout}
          widgetAtom={fallbackWidgetAtom}
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

const Layout: FormLayout = ({ schema, formAtom, className }) => {
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

  return (
    <div
      className={clsx(className, styles.formLayout, {
        [styles.small]: isSmall,
      })}
      ref={ref}
    >
      <div className={styles.main}>
        {mainItems.map((item) => (
          <FormWidget key={item.uid} schema={item} formAtom={formAtom} />
        ))}
      </div>
      {sideItems.length > 0 && (
        <div className={styles.side}>
          {side.map((item) => (
            <FormWidget key={item.uid} schema={item} formAtom={formAtom} />
          ))}
        </div>
      )}
    </div>
  );
};
