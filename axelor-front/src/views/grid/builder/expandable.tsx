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
import { SetStateAction, atom, useAtomValue, useSetAtom } from "jotai";
import merge from "lodash/merge";
import cloneDeep from "lodash/cloneDeep";
import isEqual from "lodash/isEqual";

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
import { SaveOptions } from "@/services/client/data";

import { useFormEditableScope, useFormScope } from "@/views/form/builder/scope";
import {
  FormWidgetProviders,
  FormWidgetsHandler,
} from "@/views/form/builder/form-providers";
import { Layout, fetchRecord, showErrors, useGetErrors } from "@/views/form";
import { createFormAtom, createWidgetAtom } from "@/views/form/builder/atoms";
import { Form, FormState, useFormHandlers } from "@/views/form/builder";
import { processOriginal, processSaveValues } from "@/views/form/builder/utils";
import { useViewConfirmDirty, useViewTab } from "@/view-containers/views/scope";
import { useGridExpandableContext, useGridContext } from "./scope";
import { useSingleClickHandler } from "@/hooks/use-button";
import formStyles from "@/views/form/form.module.scss";
import styles from "./expandable.module.scss";

export function ExpandIcon({
  expand,
  children,
}: {
  expand?: boolean;
  disable?: boolean;
  children?: boolean;
}) {
  return (
    <Box
      as="span"
      d="flex"
      className={clsx(styles.expandIcon, {
        [styles.expanded]: expand,
      })}
    >
      <MaterialIcon
        icon={children ? "keyboard_double_arrow_down" : "keyboard_arrow_down"}
      />
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
  onSave?: (
    record: DataRecord,
    options?: SaveOptions<DataRecord>,
  ) => void | Promise<void> | undefined;
  onClose?: () => void;
}) {
  const { readonly, type: parentType } = useGridContext();
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
    toKebabCase((gridView as Schema)?.widget ?? "") === "tree-grid";
  const hasSummaryView = Boolean((gridView as Schema)?.summaryView);
  const isTopGridTree = parentType === "grid" && isTreeGrid;
  const isO2M = (gridView as Schema)?.serverType === "ONE_TO_MANY";

  const {
    onNew: onNewAction,
    onLoad: onLoadAction,
    onSave: onSaveAction,
  } = schema;

  const fetchedRef = useRef<DataRecord>();
  const closeRef = useRef(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const widgetHandler = useRef<FormWidgetsHandler | null>(null);

  const [loading, setLoading] = useState(true);
  const [edit, setEdit] = useState(false);

  const { add: addEditableWidget } = useFormEditableScope();

  const showConfirmDirty = useViewConfirmDirty();
  const getErrors = useGetErrors();

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

          if (rec._dirty && fetchedRef.current) {
            return {
              ...fetchedRef.current,
              ...rec,
            };
          }

          hasFetched = true;
          return fetchRecord(meta, ds, rec.id!).then((fetchedRecord) => ({
            ...fetchedRecord,
            ...(rec._dirty && { ...rec }),
          }));
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

  const doValidate = useAtomCallback(
    useCallback(
      (get, set, { silent }: { silent?: boolean } = {}) => {
        const formState = get(formAtom);
        const errors = getErrors(formState);

        if (errors || widgetHandler.current?.invalid?.()) {
          if (errors) {
            if (silent) {
              onDiscard?.({ id: formState.record.id });
            } else {
              showErrors(errors);
            }
          }
          return Promise.reject();
        }
      },
      [formAtom, getErrors, onDiscard],
    ),
  );

  const doSave = useAtomCallback(
    useCallback(
      async (
        get,
        set,
        opts: {
          silent?: boolean;
          autoSave?: boolean;
          shouldClose?: boolean;
          shouldReload?: boolean;
          callOnSave?: boolean;
        } = {},
      ) => {
        const {
          silent,
          autoSave = false,
          shouldClose = false,
          shouldReload = true,
          callOnSave = true,
        } = opts;

        // XXX: Need to find a way to coordinate top save and grid commit.
        // case for top level tree-grid auto save
        if (!isCollection && autoSave) {
          await actionExecutor.waitFor(300);
        }

        await actionExecutor.wait();

        if (!autoSave) {
          await widgetHandler.current?.commit?.();
        }

        await doValidate({ silent });

        const formState = get(formAtom);

        const doClose = () => {
          closeRef.current = true;
          onClose?.();
        };

        if (formState.dirty) {
          if (isO2M) {
            if (shouldClose) doClose();

            set(formAtom, (draft) => ({ ...draft, dirty: false }));

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

          const updated = await onSave?.(
            {
              ...processSaveValues({ ...dummyVals, ...vals }, formState.meta),
              _original: processOriginal(original, meta.fields ?? {}), // pass original values to check for concurrent updates
            },
            isTopGridTree ? { select: get(selectStateAtom) } : {},
          );

          if (updated && shouldReload && !shouldClose) {
            doOnLoad(updated);
          }
        }

        if (shouldClose) {
          doClose();
        }
      },
      [
        isCollection,
        isTopGridTree,
        isO2M,
        doValidate,
        doOnLoad,
        onSaveAction,
        actionExecutor,
        onSave,
        onClose,
        formAtom,
        selectStateAtom,
        meta.fields,
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
    if (record) doOnLoad(record);
  }, [record, doOnLoad]);

  const doActionReload = useCallback(async () => {
    if (record) doOnLoad(record, { fromAction: true });
  }, [record, doOnLoad]);

  const handleUpdateClick = useSingleClickHandler(
    useCallback(() => {
      doSave({
        shouldClose: isCollection,
      });
    }, [doSave, isCollection]),
  );

  const handleSaveClick = useSingleClickHandler(
    useCallback(() => {
      doSave();
    }, [doSave]),
  );

  useEffect(() => {
    // auto save whenever form is dirty
    if (formDirty && record && isTreeGrid) {
      doSave({
        autoSave: true,
        shouldReload: isTopGridTree,
      });
    }
  }, [formDirty, isTreeGrid, record, doSave, isTopGridTree]);

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
    useCallback(async () => await doValidate(), [doValidate]),
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
    if (formReady && !isCollection && !isTreeGrid) {
      if ("requestIdleCallback" in window) {
        const id = window.requestIdleCallback(() => scrollToForm());
        return () => window.cancelIdleCallback(id);
      } else {
        const timer = setTimeout(() => scrollToForm(), 300);
        return () => clearTimeout(timer);
      }
    }
  }, [formReady, scrollToForm, isCollection, isTreeGrid]);

  useEffect(() => {
    if (formSelect) {
      setSelectState((state) => {
        const newState = merge(cloneDeep(state), cloneDeep(formSelect));
        return isEqual(state, newState) ? state : newState;
      });
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
        [styles.summaryView]: isTreeGrid && hasSummaryView,
      })}
      ref={containerRef}
      onKeyDown={handleKeyDown}
    >
      {loading && !formReady ? null : (
        <FormWidgetProviders ref={widgetHandler} record={record}>
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
                      onClick={handleUpdateClick}
                    >
                      {i18n.get("Update")}
                    </Button>
                  )}
                </>
              ) : (
                <>
                  <Button size="sm" variant="light" onClick={() => onClose?.()}>
                    {i18n.get("Close")}
                  </Button>
                  {!isCollection && canEdit && canSave && (
                    <Button
                      size="sm"
                      variant="primary"
                      onClick={(e) => {
                        if (edit) {
                          handleSaveClick(e);
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
        </FormWidgetProviders>
      )}
    </Box>
  );
}
