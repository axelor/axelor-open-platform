import { produce } from "immer";
import { useAtom, useAtomValue } from "jotai";
import { focusAtom } from "jotai-optics";
import { selectAtom } from "jotai/utils";
import { ReactElement, useCallback, useEffect, useMemo } from "react";

import { Box, Button, Divider, Input } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { Select } from "@/components/select";
import { Filter } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { moment, Moment } from "@/services/client/l10n";
import {
  AdvancedSearchAtom,
  Field,
  SavedFilter,
} from "@/services/client/meta.types";

import { AdvancedSearchState } from "../types";
import { getContextFieldFilter } from "../utils";
import { BooleanRadio, RelationalWidget } from "./components";
import { Criteria } from "./criteria";

import styles from "./editor.module.scss";

export const getEditorDefaultState = () =>
  ({
    title: "",
    operator: "and",
    shared: false,
    selected: false,
    criteria: [],
  }) as AdvancedSearchState["editor"];

function FormControl({
  title,
  children,
}: {
  title?: string;
  children: ReactElement;
}) {
  return (
    <Box d="flex" alignItems="center" gap={8}>
      {children}
      <Box as="p" mb={0}>
        {title}
      </Box>
    </Box>
  );
}

const getFilterName = (title?: string) =>
  title?.replace(" ", "_").toLowerCase();

export interface EditorProps {
  stateAtom: AdvancedSearchAtom;
  fields: Field[];
  contextFields: Field[];
  canExportFull?: boolean;
  canShare?: boolean;
  onClear?: () => void;
  onApply?: () => void;
  onExport?: (exportFull?: boolean) => void;
  onDelete?: (data: SavedFilter) => void;
  onSave?: (data: SavedFilter) => void;
}

export function Editor({
  canExportFull = true,
  canShare = true,
  stateAtom,
  fields,
  contextFields,
  onClear,
  onApply,
  onSave,
  onExport,
  onDelete,
}: EditorProps) {
  const filters = useAtomValue(
    useMemo(() => selectAtom(stateAtom, (s) => s.filters), [stateAtom]),
  );
  const [editor, setEditor] = useAtom(
    useMemo(() => focusAtom(stateAtom, (o) => o.prop("editor")), [stateAtom]),
  );
  const [archived, setArchived] = useAtom(
    useMemo(() => focusAtom(stateAtom, (o) => o.prop("archived")), [stateAtom]),
  );
  const [contextField, setContextField] = useAtom(
    useMemo(
      () => focusAtom(stateAtom, (o) => o.prop("contextField")),
      [stateAtom],
    ),
  );
  const { id, title = "", operator = "and", shared } = editor || {};
  const criteria = editor?.criteria?.length ? editor.criteria : [{}];

  const handleChange = useCallback(
    function handleChange(name: string, value: any) {
      setEditor((criteria) => ({
        ...criteria,
        [name]: value,
      }));
    },
    [setEditor],
  );

  const handleCriteriaAdd = useCallback(
    function handleCriteriaAdd() {
      setEditor((editor) => {
        if (editor?.criteria?.slice()?.pop()?.operator) {
          return {
            ...editor,
            criteria: [...editor.criteria, {}],
          };
        }
        return editor;
      });
    },
    [setEditor],
  );

  const handleCriteriaRemove = useCallback(
    function handleCriteriaRemove(index: number) {
      setEditor((editor) => ({
        ...editor,
        criteria: editor?.criteria?.filter((c, ind) => ind !== index),
      }));
    },
    [setEditor],
  );

  const handleCriteriaChange = useCallback(
    function handleCriteriaChange(
      {
        name,
        value,
      }: {
        name: string;
        value: any;
      },
      index: number,
    ) {
      function getDefaultValue(fieldName: string) {
        const field = fields?.find((f) => f.name === fieldName);
        const type = field?.type.toLowerCase();
        if (!type) return "";
        if (
          ["one_to_one", "many_to_one", "many_to_many", "one_to_many"].includes(
            type,
          )
        ) {
          return null;
        } else if (["integer", "long", "decimal"].includes(type)) {
          return 0;
        } else if (["date", "time", "dateTime"].includes(type)) {
          return null;
        } else {
          return "";
        }
      }

      if (value instanceof moment) {
        value = moment(value as Moment).format("YYYY-MM-DD");
      }

      function shouldClearValue(prevOp: string, currentOp: string) {
        const pairs = [
          ["=", "!="],
          ["like", "notLike"],
          ["in", "notIn"],
        ];
        return !pairs.some(
          (pair) => pair.includes(prevOp) && pair.includes(currentOp),
        );
      }

      setEditor(
        produce((draft) => {
          if (!draft) return;

          !draft.criteria && (draft.criteria = []);
          !draft.criteria[index] && (draft.criteria[index] = {});

          const $prevValue = (draft?.criteria?.[index] as any)?.[name];

          (draft.criteria[index] as any)[name] = value;

          const filter = draft.criteria[index] as any;
          if (filter) {
            if (name === "fieldName") {
              const defaultValue = getDefaultValue(value);
              filter.operator = "";
              filter.value = defaultValue;
              filter.value2 = defaultValue;
              filter.timeUnit = "";
            }
            if (name === "operator" && shouldClearValue($prevValue, value)) {
              filter.value = null;
              filter.value2 = null;
              filter.timeUnit = ["$inPast", "$inNext", "$inCurrent"].includes(
                value,
              )
                ? "day"
                : "";
            }
          }
        }),
      );
    },
    [setEditor, fields],
  );

  const handleFilterSave = useCallback(
    function handleFilterSave(savedAs?: boolean) {
      const {
        id,
        version,
        title,
        shared,
        operator: editorOperator,
        criteria: editorCriteria,
      } = editor!;
      let operator = editorOperator ?? "and";
      let criteria = editorCriteria;
      const contextFieldFilter = getContextFieldFilter(contextField);
      if (contextFieldFilter) {
        criteria = [
          contextFieldFilter,
          ...(criteria?.length ? [{ operator, criteria }] : []),
        ];
        operator = "and";
      }
      const savedFilter: Partial<SavedFilter> = {
        shared: shared || false,
        title: title,
        filterCustom: JSON.stringify({
          operator,
          criteria: criteria?.map((c: any) => ({
            ...c,
            value: c.value === null ? undefined : c.value,
            value2: c.value2 === null ? undefined : c.value2,
            timeUnit: !c.timeUnit ? undefined : c.timeUnit,
          })),
        }),
      };
      if (id && !savedAs) {
        savedFilter.id = id;
        savedFilter.version = version;
      } else {
        savedFilter.name = getFilterName(title);
      }
      onSave?.(savedFilter as SavedFilter);
    },
    [editor, contextField, onSave],
  );

  const handleFilterRemove = useCallback(
    function handleFilterRemove() {
      onDelete?.(editor as SavedFilter);
    },
    [editor, onDelete],
  );

  const criteriaFields = useMemo(
    () =>
      fields.filter((field) => {
        const { contextField: contextFieldName, contextFieldValue } =
          field as any;
        return (
          !contextFieldName ||
          (contextField?.field?.name === contextFieldName &&
            String(contextField?.value?.id) === String(contextFieldValue))
        );
      }),
    [fields, contextField],
  );

  const hasContextField = Boolean(contextField?.field?.name);
  const defaultContextField = contextFields?.[0];

  useEffect(() => {
    !hasContextField &&
      defaultContextField &&
      setContextField((state) => ({
        ...state,
        field: defaultContextField,
      }));
  }, [hasContextField, defaultContextField, setContextField]);

  const selectedContextField =
    contextField?.field?.name &&
    contextFields?.find((x) => x.name === contextField.field?.name);
  const canSaveAs =
    id && title && filters?.find((f) => f.id === id)?.title !== title;

  return (
    <Box d="flex" flexDirection="column" alignItems="start" pt={2} g={2}>
      {(contextFields?.length ?? 0) > 0 && (
        <Box d="flex" alignItems="center" g={2}>
          <Box d="flex" alignItems="center">
            <MaterialIcon
              icon="close"
              className={styles.icon}
              onClick={() =>
                setContextField((data) => ({ ...data, value: null }))
              }
            />
          </Box>
          <Select
            className={styles.select}
            multiple={false}
            options={contextFields}
            optionKey={(x) => x.name}
            optionLabel={(x) => x.title ?? x.autoTitle ?? x.name}
            optionEqual={(x, y) => x.name === y.name}
            onChange={(field) => {
              setContextField((prev) => {
                return {
                  ...prev,
                  field: field as Field,
                  value: null,
                  name: field?.name,
                };
              });
            }}
            value={contextField?.field ?? null}
          />
          {selectedContextField && (
            <RelationalWidget
              operator="in"
              isMulti={false}
              onChange={({ value }: any) =>
                setContextField((data) => ({ ...data, value }))
              }
              value={contextField.value}
              field={selectedContextField}
            />
          )}
        </Box>
      )}
      <Box d="flex" alignItems="center" g={2}>
        <BooleanRadio
          name="operator"
          onChange={(e: any) => handleChange("operator", e.target.value)}
          value={operator}
          data={[
            { label: i18n.get("and"), value: "and" },
            { label: i18n.get("or"), value: "or" },
          ]}
        />
        <FormControl title={i18n.get("Show archived")}>
          <Input
            type="checkbox"
            checked={Boolean(archived)}
            onChange={({ target: { checked } }) => setArchived(checked)}
            name={"archived"}
            m={0}
          />
        </FormControl>
      </Box>
      <Box
        d="flex"
        flexDirection="column"
        alignItems="flex-start"
        g={2}
        w={100}
      >
        {criteria.map((item: any, index: number) => (
          <Criteria
            key={index}
            index={index}
            value={item as Filter}
            fields={criteriaFields}
            onRemove={handleCriteriaRemove}
            onChange={handleCriteriaChange}
          />
        ))}
      </Box>
      <Box d="flex" alignItems="center">
        <Button variant="link" size="sm" onClick={handleCriteriaAdd}>
          {i18n.get("Add filter")}
        </Button>
        <Box className={styles.divider}>
          <Divider vertical />
        </Box>
        <Button variant="link" size="sm" onClick={() => onClear?.()}>
          {i18n.get("Clear")}
        </Button>
        <Box className={styles.divider}>
          <Divider vertical />
        </Box>
        <Button variant="link" size="sm" onClick={() => onExport?.()}>
          {i18n.get("Export")}
        </Button>
        <Box className={styles.divider}>
          <Divider vertical />
        </Box>
        {canExportFull && (
          <>
            <Button variant="link" size="sm" onClick={() => onExport?.(true)}>
              {i18n.get("Export full")}
            </Button>
            <Box className={styles.divider}>
              <Divider vertical />
            </Box>
          </>
        )}
        <Button variant="link" size="sm" onClick={() => onApply?.()}>
          {i18n.get("Apply")}
        </Button>
      </Box>

      <Box d="flex" flexDirection="column" p={1} g={2}>
        <Box d="flex" g={2} alignItems="center">
          <Input
            name="title"
            className="title"
            placeholder={i18n.get("Save filter as")}
            value={title}
            onChange={(e) => handleChange("title", e.target.value)}
          />
          {canShare && (
            <FormControl title={i18n.get("Share")}>
              <Input
                type="checkbox"
                checked={Boolean(shared)}
                onChange={({ target: { checked } }) =>
                  handleChange("shared", checked)
                }
                name={"shared"}
                m={0}
              />
            </FormControl>
          )}
        </Box>
        <Box d="flex" alignItems="center" gap={8}>
          {title && (
            <Button
              outline
              size="sm"
              variant="primary"
              onClick={() => handleFilterSave()}
            >
              {id ? i18n.get("Update") : i18n.get("Save")}
            </Button>
          )}
          {canSaveAs && (
            <Button
              outline
              size="sm"
              variant="primary"
              onClick={() => handleFilterSave(true)}
            >
              {i18n.get("Save as")}
            </Button>
          )}
          {id && (
            <Button
              outline
              size="sm"
              variant="danger"
              onClick={() => handleFilterRemove()}
            >
              {i18n.get("Delete")}
            </Button>
          )}
        </Box>
      </Box>
    </Box>
  );
}
