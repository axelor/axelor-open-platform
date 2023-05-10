import { ReactElement, useCallback, useEffect, useMemo } from "react";
import { produce } from "immer";
import { useAtom, useAtomValue } from "jotai";
import { focusAtom } from "jotai-optics";
import { selectAtom } from "jotai/utils";
import { sortBy } from "lodash";

import { Box, Button, Divider, Input } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { moment, Moment } from "@/services/client/l10n";
import { i18n } from "@/services/client/i18n";
import { BooleanRadio, RelationalWidget, Select } from "./components";
import { Criteria } from "./criteria";
import { Filter } from "@/services/client/data.types";
import {
  AdvancedSearchAtom,
  Field,
  JsonField,
  Property,
  SavedFilter,
  View,
} from "@/services/client/meta.types";
import { AdvancedSearchState } from "../types";
import { toKebabCase } from "@/utils/names";
import styles from "./editor.module.scss";

export const getEditorDefaultState = () =>
  ({
    title: "",
    operator: "and",
    shared: false,
    selected: false,
    criteria: [],
  } as AdvancedSearchState["editor"]);

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
  canExportFull?: boolean;
  canShare?: boolean;
  items?: View["items"];
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
  items,
  onClear,
  onApply,
  onSave,
  onExport,
  onDelete,
}: EditorProps) {
  const fields = useAtomValue(
    useMemo(() => selectAtom(stateAtom, (s) => s.fields), [stateAtom])
  );
  const jsonFields = useAtomValue(
    useMemo(() => selectAtom(stateAtom, (s) => s.jsonFields), [stateAtom])
  );
  const filters = useAtomValue(
    useMemo(() => selectAtom(stateAtom, (s) => s.filters), [stateAtom])
  );
  const [editor, setEditor] = useAtom(
    useMemo(() => focusAtom(stateAtom, (o) => o.prop("editor")), [stateAtom])
  );
  const [archived, setArchived] = useAtom(
    useMemo(() => focusAtom(stateAtom, (o) => o.prop("archived")), [stateAtom])
  );
  const [contextField, setContextField] = useAtom(
    useMemo(
      () => focusAtom(stateAtom, (o) => o.prop("contextField")),
      [stateAtom]
    )
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
    [setEditor]
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
    [setEditor]
  );

  const handleCriteriaRemove = useCallback(
    function handleCriteriaRemove(index: number) {
      setEditor((editor) => ({
        ...editor,
        criteria: editor?.criteria?.filter((c, ind) => ind !== index),
      }));
    },
    [setEditor]
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
      index: number
    ) {
      function getDefaultValue(fieldName: string) {
        const field = fields?.[fieldName];
        const type = field?.type.toLowerCase();
        if (!type) return "";
        if (
          ["one_to_one", "many_to_one", "many_to_many", "one_to_many"].includes(
            type
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
          (pair) => pair.includes(prevOp) && pair.includes(currentOp)
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
                value
              )
                ? "day"
                : "";
            }
          }
        })
      );
    },
    [setEditor, fields]
  );

  const handleFilterSave = useCallback(
    function handleFilterSave(savedAs?: boolean) {
      const { id, version, title, shared, operator, criteria } = editor!;
      let savedFilter: Partial<SavedFilter> = {
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
    [editor, onSave]
  );

  const handleFilterRemove = useCallback(
    function handleFilterRemove() {
      onDelete?.(editor as SavedFilter);
    },
    [editor, onDelete]
  );

  const $fields = useMemo(() => {
    const fieldList = Object.values(fields || {}).filter((field: Property) => {
      const { type, large } = field as any;
      if (
        type === "binary" ||
        large ||
        field.json ||
        field.encrypted ||
        ["id", "version", "archived", "selected"].includes(field.name!)
      ) {
        return false;
      }

      return true;
    });

    Object.keys(jsonFields || {}).forEach((prefix) => {
      const { title } = fields?.[prefix as any] || {};
      const keys = Object.keys(jsonFields?.[prefix] || {});

      keys?.forEach?.((name: string) => {
        const field = (jsonFields?.[prefix]?.[name] || {}) as JsonField;
        const type = toKebabCase(field.type);
        if (["button", "panel", "separator", "many-to-many"].includes(type))
          return;

        let key = prefix + "." + name;
        if (type !== "many-to-one") {
          key += "::" + (field.jsonType || "text");
        }
        fieldList.push({
          ...(field as any),
          name: key,
          title: `${field.title || field.autoTitle} ${
            title ? `(${title})` : ""
          }`,
        } as Property);
      });
    });

    return sortBy(fieldList, "title") as unknown as Field[];
  }, [fields, jsonFields]);

  const criteriaFields = useMemo(
    () =>
      $fields.filter((field) => {
        const { contextField: contextFieldName, contextFieldValue } =
          field as any;
        return (
          !contextFieldName ||
          (contextField?.name === contextFieldName &&
            String(contextField?.value?.id) === String(contextFieldValue))
        );
      }),
    [$fields, contextField]
  );

  const contextFields = useMemo(
    () =>
      $fields.reduce((ctxFields, field) => {
        const {
          contextField,
          contextFieldTitle,
          contextFieldValue,
          contextFieldTarget,
          contextFieldTargetName,
        } = field as any;
        if (contextField && !ctxFields.find((x) => x.name === contextField)) {
          ctxFields.push({
            name: contextField,
            title: `${contextField[0].toUpperCase()}${contextField.substr(1)}`,
            value: {
              id: contextFieldValue,
              [contextFieldTargetName]: contextFieldTitle,
            },
            target: contextFieldTarget,
            targetName: contextFieldTargetName,
          } as unknown as Field);
        }
        return ctxFields;
      }, [] as Field[]) as Field[],
    [$fields]
  );

  const defaultContextFieldName = contextFields?.[0]?.name;

  useEffect(() => {
    defaultContextFieldName &&
      setContextField((field) => ({
        ...field,
        name: field?.name || defaultContextFieldName,
      }));
  }, [defaultContextFieldName, setContextField]);

  const selectedContextField =
    contextField?.name &&
    contextFields.find((x) => x.name === contextField.name);
  const canSaveAs =
    id && title && filters?.find((f) => f.id === id)?.title !== title;

  return (
    <Box d="flex" flexDirection="column" alignItems="start" g={2}>
      {contextFields.length > 0 && (
        <Box d="flex" alignItems="center" gap={8}>
          <Box
            aria-label="close"
            onClick={() =>
              setContextField((data) => ({ ...data, value: null }))
            }
          >
            <MaterialIcon icon="close" />
          </Box>
          <Select
            name={"ctxField"}
            onChange={(value: any) =>
              setContextField((data) => ({ ...data, name: value }))
            }
            value={contextField?.name ?? null}
            options={contextFields}
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
      <Box d="flex" flexDirection="column" alignItems="flex-start" g={2}>
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
