import { useAtom, useAtomValue } from "jotai";
import { MouseEvent, useCallback, useMemo, useState } from "react";
import { Box, Select, SelectProps } from "@axelor/ui";

import { EditorOptions, useCompletion, useEditor } from "@/hooks/use-relation";
import { DataRecord } from "@/services/client/data.types";

import { FieldContainer, FieldProps } from "../../builder";
import { Chip } from "../selection";
import { Schema } from "@/services/client/meta.types";
import { i18n } from "@/services/client/i18n";
import { DataStore } from "@/services/client/data-store";

export function TagSelectComponent({
  schema,
  ...props
}: { schema: Schema } & SelectProps) {
  const { targetName } = schema;
  const components = useMemo(
    () => ({
      MultiValue: (props: any) => {
        const { data, removeProps } = props;
        return (
          <Box me={1}>
            <Chip
              color={"indigo"}
              title={data?.[targetName]}
              onRemove={removeProps.onClick}
            />
          </Box>
        );
      },
    }),
    [targetName]
  );

  return (
    <Select
      isMulti
      optionLabel={targetName}
      optionValue={"id"}
      components={components}
      {...props}
    />
  );
}

const ADD_ON_OPTIONS = {
  Create: -1,
  CreateInput: -2,
  CreateAndSelectInput: -3,
} as const;

export function TagSelect(props: FieldProps<DataRecord[]>) {
  const { schema, valueAtom, widgetAtom, readonly } = props;
  const {
    uid,
    target,
    targetName,
    targetSearch,
    placeholder,
    showTitle = true,
  } = schema;

  const { attrs } = useAtomValue(widgetAtom);

  const [value, setValue] = useAtom(valueAtom);
  const [inputText, setInputText] = useState("");
  const showEditor = useEditor();

  const { title } = attrs;
  const canNew = schema.canNew !== false;
  const optionLabel = targetName;
  const createNames = useMemo<string[]>(
    () => (schema.create ?? "")?.split(/\s*,\s*/),
    [schema.create]
  );

  const search = useCompletion({
    target,
    targetName,
    targetSearch,
  });

  const handleEdit = useCallback(
    async (
      value: DataRecord,
      readonly = false,
      onSelect?: EditorOptions["onSelect"]
    ) => {
      return showEditor({
        title: title ?? "",
        model: target,
        record: value,
        readonly,
        onSelect,
      });
    },
    [showEditor, target, title]
  );

  const handleView = useCallback(
    (e: MouseEvent<HTMLAnchorElement>, value: DataRecord) => {
      e.preventDefault();
      return handleEdit(value, true);
    },
    [handleEdit]
  );

  const handleCompletion = useCallback(
    async (value: string) => {
      const res = await search(value);
      const { records } = res;
      return records;
    },
    [search]
  );

  const handleChange = useCallback(
    async (value: any[]) => {
      const addOnOption = value?.find?.((v) =>
        Object.values(ADD_ON_OPTIONS).includes(v?.id)
      );
      if (addOnOption) {
        const record: DataRecord = {};
        const updateValue = (record: DataRecord) =>
          setValue(value.map((v) => (v === addOnOption ? record : v)));
        if (
          [
            ADD_ON_OPTIONS.CreateInput,
            ADD_ON_OPTIONS.CreateAndSelectInput,
          ].includes(addOnOption.id)
        ) {
          createNames?.forEach((name) => {
            record[name] = addOnOption._text;
          });
        }

        if (addOnOption.id === ADD_ON_OPTIONS.CreateAndSelectInput) {
          const ds = new DataStore(target);
          const newRecord = await ds.save(record);
          newRecord && updateValue(newRecord);
        } else {
          await handleEdit(record, false, updateValue);
        }
      } else {
        setValue(value);
      }
    },
    [target, createNames, handleEdit, setValue]
  );

  const addOnOptions = useMemo(() => {
    if (!canNew) return [];
    if (inputText && createNames.length) {
      return [
        {
          id: ADD_ON_OPTIONS.CreateInput,
          _text: inputText,
          [optionLabel]: i18n.get('Create "{0}"...', inputText),
        },
        {
          id: ADD_ON_OPTIONS.CreateAndSelectInput,
          _text: inputText,
          [optionLabel]: i18n.get('Create "{0}" and select...', inputText),
        },
      ];
    }
    return [
      { id: ADD_ON_OPTIONS.Create, [optionLabel]: i18n.get("Create...") },
    ];
  }, [canNew, optionLabel, createNames, inputText]);

  return (
    <FieldContainer>
      {showTitle && <label htmlFor={uid}>{title}</label>}
      {readonly ? (
        <Box d="flex" flexWrap="wrap">
          {(value || []).map((val) => (
            <Box
              m={1}
              key={val?.id}
              as="a"
              href="#"
              onClick={(e) => handleView(e, val)}
            >
              <Chip title={val[targetName]} color={"indigo"} />
            </Box>
          ))}
        </Box>
      ) : (
        <TagSelectComponent
          schema={schema}
          onChange={handleChange}
          value={value}
          fetchOptions={handleCompletion}
          placeholder={placeholder}
          {...(canNew && {
            addOnOptions,
          })}
          {...(createNames.length > 0 && {
            onInputChange: setInputText,
          })}
        />
      )}
    </FieldContainer>
  );
}
