import { Box, Button, clsx } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import {
  KeyboardEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { selectAtom, useAtomCallback } from "jotai/utils";
import { ScopeProvider } from "bunshi/react";
import { SetStateAction, atom, useAtomValue, useSetAtom } from "jotai";
import merge from "lodash/merge";
import cloneDeep from "lodash/cloneDeep";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { usePerms } from "@/hooks/use-perms";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { FormView, GridView, Schema } from "@/services/client/meta.types";
import { diff, extractDummy } from "@/services/client/data-utils";
import { parseExpression } from "@/hooks/use-parser/utils";
import { i18n } from "@/services/client/i18n";
import { toKebabCase } from "@/utils/names";

import {
  FormEditableScope,
  FormValidityHandler,
  FormValidityScope,
  useFormEditableScope,
  useFormScope,
} from "@/views/form/builder/scope";
import { Layout, fetchRecord, showErrors, useGetErrors } from "@/views/form";
import { createFormAtom, createWidgetAtom } from "@/views/form/builder/atoms";
import { Form, FormState, useFormHandlers } from "@/views/form/builder";
import { processOriginal, processSaveValues } from "@/views/form/builder/utils";
import { useViewConfirmDirty, useViewTab } from "@/view-containers/views/scope";
import { useGridExpandableContext, useGridContext } from "./scope";
import formStyles from "@/views/form/form.module.scss";
import styles from "./expandable.module.scss";

export function ExpandIcon({ expand }: { expand?: boolean }) {
  return (
    <Box
      as="span"
      d="flex"
      className={clsx(styles.expandIcon, {
        [styles.expanded]: expand,
      })}
    >
      <MaterialIcon icon={"keyboard_arrow_down"} />
    </Box>
  );
}

export function ExpandableFormView({
  gridView,
  meta,
  record,
  onSave,
  onDiscard,
  onClose,
}: {
  gridView?: GridView;
  meta: ViewData<FormView>;
  record: DataRecord;
  readonly?: boolean;
  onDiscard?: (record: DataRecord) => void;
  onUpdate?: (record: DataRecord) => void | Promise<void> | undefined;
  onSave?: (record: DataRecord) => void | Promise<void> | undefined;
  onClose?: () => void;
}) {
  const { readonly } = useGridContext();
  const { formAtom: parentFormAtom } = useFormScope();
  const { selectAtom: selectStateAtom } = useGridExpandableContext();
  const { id: tabId } = useViewTab();
  const setSelectState = useSetAtom(selectStateAtom);

  const { hasButton } = usePerms(meta.view, meta.perms);

  const ds = useMemo(
    () => new DataStore(meta.view.model!, {}),
    [meta.view.model],
  );
  const schema = meta.view!;
  const isCollection = (gridView as Schema)?.serverType?.endsWith("_TO_MANY");
  const isTreeGrid =
    isCollection &&
    toKebabCase((gridView as Schema)?.widget ?? "") === "tree-grid";
  const isO2M = (gridView as Schema)?.serverType === "ONE_TO_MANY";

  const {
    onNew: onNewAction,
    onLoad: onLoadAction,
    onSave: onSaveAction,
  } = schema;

  const fetchedRef = useRef<DataRecord>();
  const closeRef = useRef(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const widgetsRef = useRef(new Set<FormValidityHandler>());
  const editableWidgetsRef = useRef(new Set<() => void>());
  const [loading, setLoading] = useState(true);
  const [edit, setEdit] = useState(false);

  const { add: addEditableWidget } = useFormEditableScope();

  const showConfirmDirty = useViewConfirmDirty();
  const getErrors = useGetErrors();
  const getWidgetErrors = useCallback(() => {
    return Array.from(widgetsRef.current).some((checkWidgetInvalid) =>
      checkWidgetInvalid(),
    );
  }, []);

  const editorRecord = useRef(record).current;
  const editorFormAtom = useMemo(
    () =>
      createFormAtom({
        meta,
        record: editorRecord,
        parent: parentFormAtom,
      }),
    [editorRecord, meta, parentFormAtom],
  );

  const editorAtom = useMemo(
    () =>
      atom(
        (get) => get(editorFormAtom),
        (get, set, update: SetStateAction<FormState>) => {
          const state =
            typeof update === "function" ? update(get(editorFormAtom)) : update;

          set(editorFormAtom, state);
        },
      ),
    [editorFormAtom],
  );

  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(meta, editorRecord, {
      formAtom: editorAtom,
    });

  const formReady = useAtomValue(
    useMemo(() => selectAtom(formAtom, (state) => state.ready), [formAtom]),
  );
  const formDirty = useAtomValue(
    useMemo(() => selectAtom(formAtom, (state) => state.dirty), [formAtom]),
  );
  const formSelect = useAtomValue(
    useMemo(() => selectAtom(formAtom, (state) => state.select), [formAtom]),
  );

  const scrollToForm = useCallback(() => {
    const row = containerRef.current?.parentNode
      ?.previousSibling as HTMLElement;
    const body = row?.parentNode as HTMLElement;
    const grid = body?.parentNode as HTMLElement; // row=>body=>grid
    if (row && body && grid) {
      grid.scrollTop = row.offsetTop - body.offsetTop;
    }
  }, []);

  const widgetAtom = useMemo(
    () => createWidgetAtom({ schema, formAtom }),
    [formAtom, schema],
  );

  const handleAddWidgetValidator = useCallback((fn: FormValidityHandler) => {
    widgetsRef.current.add(fn);
    return () => widgetsRef.current.delete(fn);
  }, []);

  const handleAddEditableWidget = useCallback((fn: () => void) => {
    editableWidgetsRef.current.add(fn);
    return () => editableWidgetsRef.current.delete(fn);
  }, []);

  const handleCommitEditableWidgets = useCallback(() => {
    return Promise.all(
      Array.from(editableWidgetsRef.current).map((fn) => fn()),
    );
  }, []);

  const doOnLoad = useAtomCallback(
    useCallback(
      async (
        get,
        set,
        rec: DataRecord,
        opts: { fromAction?: boolean } = {},
      ) => {
        let hasFetched = false;

        const { fromAction } = opts;

        const _record: DataRecord = await (async () => {
          const fields = Object.keys(meta.fields ?? {});
          const missing = fields.filter((field) => rec[field] === undefined);
          if (missing.length === 0) return rec;

          hasFetched = true;
          return fetchRecord(meta, ds, rec.id!);
        })();

        const record: DataRecord = (fetchedRef.current = {
          id: rec.id,
          ..._record,
          ...(rec._dirty
            ? {
                version: _record.version,
              }
            : {}),
        });

        setLoading(false);

        set(formAtom, (state) => ({
          ...state,
          ...(!fromAction &&
            hasFetched && {
              states: {},
              statesByName: {},
            }),
          record,
          dirty: false,
          original: record,
        }));

        if (hasFetched) {
          const isNew = !(record.id! > 0 || record._dirty);
          const action = isNew ? onNewAction : onLoadAction;

          if (action) {
            await actionExecutor.execute(action);
          }

          if (!isNew && fromAction) {
            const event = new CustomEvent("form:refresh", {
              detail: { tabId, formId: record.id },
            });
            document.dispatchEvent(event);
          }
        }

        if (!get(formAtom).ready) {
          set(formAtom, (state) => ({ ...state, ready: true }));
        }
      },
      [meta, ds, tabId, onNewAction, onLoadAction, actionExecutor, formAtom],
    ),
  );

  const doSave = useAtomCallback(
    useCallback(
      async (
        get,
        set,
        opts: {
          silent?: boolean;
          shouldClose?: boolean;
          callOnSave?: boolean;
        } = {},
      ) => {
        const { silent, shouldClose = false, callOnSave = true } = opts;

        await actionExecutor.waitFor();
        await actionExecutor.wait();
        await handleCommitEditableWidgets();

        const formState = get(formAtom);
        const errors = getErrors(formState);

        if (errors || getWidgetErrors()) {
          errors && silent && onDiscard?.({ id: formState.record.id });
          errors && !silent && showErrors(errors);
          return Promise.reject();
        }

        const doClose = () => {
          closeRef.current = true;
          onClose?.();
        };

        if (formState.dirty) {
          if (isO2M) {
            shouldClose && doClose();
            return onSave?.({
              ...formState.record,
              _dirty: true,
            });
          }
          const fieldNames = Object.keys(meta.fields ?? {});
          const dummy = extractDummy(formState.record, fieldNames);
          const dummyVals = Object.entries(dummy).reduce((acc, [k, v]) => {
            return k.startsWith("$")
              ? { ...acc, [k.substring(1)]: v }
              : { ...acc, [k]: v };
          }, {});

          if (onSaveAction && callOnSave) {
            await actionExecutor.execute(onSaveAction);
          }

          const { record: rec, original = {} } = get(formAtom); // record may have changed by actions
          const vals = diff(rec, original);

          const updated = await onSave?.({
            ...processSaveValues(dummyVals, formState.fields),
            ...processSaveValues(vals, formState.fields),
            _original: processOriginal(original, meta.fields ?? {}), // pass original values to check for concurrent updates
          });

          if (updated && !shouldClose) {
            doOnLoad(updated);
          }
        }

        shouldClose && doClose();
      },
      [
        isO2M,
        getErrors,
        getWidgetErrors,
        onSaveAction,
        actionExecutor,
        onDiscard,
        onSave,
        onClose,
        doOnLoad,
        formAtom,
        meta.fields,
        handleCommitEditableWidgets,
      ],
    ),
  );

  const doCancel = useAtomCallback(
    useCallback(
      (get) => {
        const state = get(formAtom);
        showConfirmDirty(
          async () => state.dirty ?? false,
          async () => {
            closeRef.current = true;
            onDiscard?.(record);
            onClose?.();
          },
        );
      },
      [formAtom, record, showConfirmDirty, onDiscard, onClose],
    ),
  );

  const doRefresh = useCallback(() => {
    record && doOnLoad(record);
  }, [record, doOnLoad]);

  const doActionReload = useCallback(async () => {
    record && doOnLoad(record, { fromAction: true });
  }, [record, doOnLoad]);

  const doAutoSave = useAtomCallback(
    useCallback(
      async (get, set, record: DataRecord) => {
        const formState = get(formAtom)!;
        if (!formState.dirty || getErrors(formState)) return;
        set(formAtom, { ...formState, dirty: false });
        await onSave?.({ ...record, ...formState.record, _dirty: true });
      },
      [formAtom, getErrors, onSave],
    ),
  );

  // auto save form
  useEffect(() => {
    if (formDirty && isTreeGrid) {
      doAutoSave(record);
    }
  }, [formDirty, isTreeGrid, record, doAutoSave]);

  actionHandler.setRefreshHandler(doActionReload);

  actionHandler.setSaveHandler(
    useAtomCallback(
      useCallback(
        async (get, set, record?: DataRecord) => {
          if (record) {
            await ds.save(record);
            return;
          }
          await doSave({
            callOnSave: false,
          });
        },
        [ds, doSave],
      ),
    ),
  );

  actionHandler.setValidateHandler(
    useAtomCallback(
      useCallback(
        async (get) => {
          const { record } = get(formAtom);
          const { id = 0, version = 0 } = record;
          if (id === null || version === null || id <= 0) return;
          if (await ds.verify({ id, version })) return;
          throw new Error(
            i18n.get(
              "The record has been updated or deleted by another action.",
            ),
          );
        },
        [ds, formAtom],
      ),
    ),
  );

  function handleKeyDown(e: KeyboardEvent<HTMLElement>) {
    e.stopPropagation();
  }

  useEffect(() => {
    return addEditableWidget?.(doSave);
  }, [addEditableWidget, doSave]);

  useAsyncEffect(async () => {
    if ("requestIdleCallback" in window) {
      const id = window.requestIdleCallback(() => doRefresh());
      return () => window.cancelIdleCallback(id);
    } else {
      const timer = setTimeout(() => doRefresh(), 100);
      return () => clearTimeout(timer);
    }
  }, [doRefresh]);

  useEffect(() => {
    if (formReady && !isCollection) {
      if ("requestIdleCallback" in window) {
        const id = window.requestIdleCallback(() => scrollToForm());
        return () => window.cancelIdleCallback(id);
      } else {
        const timer = setTimeout(() => scrollToForm(), 300);
        return () => clearTimeout(timer);
      }
    }
  }, [formReady, scrollToForm, isCollection]);

  useEffect(() => {
    if (formSelect) {
      setSelectState((state) => merge(cloneDeep(state), cloneDeep(formSelect)));
    }
  }, [formSelect, setSelectState]);

  const [readonlyExclusive, setReadonlyExclusive] = useState(
    Boolean(schema.readonlyIf),
  );

  useEffect(() => {
    const readonlyIf = schema.readonlyIf;
    if (!readonlyIf) return;
    return recordHandler.subscribe((rec) => {
      const value = Boolean(parseExpression(readonlyIf)(rec));
      setReadonlyExclusive(() => value);
    });
  }, [recordHandler, schema.readonlyIf]);

  const canSave = hasButton("save");
  const canEdit = hasButton("edit") && !readonlyExclusive;
  const formReadonly = !canEdit || (!edit && readonly);

  const { id } = record;
  useAsyncEffect(async () => {
    if (formReadonly) return;
    // discard on unmount
    return () => {
      if (!closeRef.current) {
        (async () => {
          try {
            if (!isCollection) {
              await onDiscard?.({ id });
            }
          } catch (err) {
            // handle errors
          }
        })();
      }
    };
  }, [formReadonly, isCollection, id]);

  return (
    <Box
      flexDirection="column"
      alignItems="center"
      className={clsx(formStyles.formViewScroller, styles.formViewScroller, {
        [styles.collection]: isCollection,
      })}
      ref={containerRef}
      onKeyDown={handleKeyDown}
    >
      {loading && !formReady ? null : (
        <ScopeProvider
          scope={FormEditableScope}
          value={{
            id: record.id,
            add: handleAddEditableWidget,
            commit: handleCommitEditableWidgets,
          }}
        >
          <ScopeProvider
            scope={FormValidityScope}
            value={{
              add: handleAddWidgetValidator,
            }}
          >
            <Form
              className={styles.formView}
              schema={meta.view}
              fields={meta.fields!}
              readonly={formReadonly}
              formAtom={formAtom}
              actionHandler={actionHandler}
              actionExecutor={actionExecutor}
              recordHandler={recordHandler}
              layout={Layout}
              widgetAtom={widgetAtom}
            />
            {!isTreeGrid && (
              <Box
                flex={1}
                d="flex"
                justifyContent="flex-end"
                gap={4}
                w={100}
                mt={2}
              >
                {canEdit && !readonly ? (
                  <>
                    <Button size="sm" variant="danger" onClick={doCancel}>
                      {i18n.get("Cancel")}
                    </Button>
                    {canSave && (
                      <Button
                        size="sm"
                        variant="primary"
                        onClick={() =>
                          doSave?.({
                            shouldClose: isCollection,
                          })
                        }
                      >
                        {i18n.get("Update")}
                      </Button>
                    )}
                  </>
                ) : (
                  <>
                    <Button
                      size="sm"
                      variant="light"
                      onClick={() => onClose?.()}
                    >
                      {i18n.get("Close")}
                    </Button>
                    {!isCollection && canEdit && canSave && (
                      <Button
                        size="sm"
                        variant="primary"
                        onClick={() => {
                          if (edit) {
                            doSave?.();
                          } else {
                            setEdit(true);
                          }
                        }}
                      >
                        {edit ? i18n.get("Save") : i18n.get("Edit")}
                      </Button>
                    )}
                  </>
                )}
              </Box>
            )}
          </ScopeProvider>
        </ScopeProvider>
      )}
    </Box>
  );
}
