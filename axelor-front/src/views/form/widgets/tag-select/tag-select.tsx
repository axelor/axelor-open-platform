import { useAtom, useAtomValue } from "jotai";
import { MouseEvent, useCallback, useMemo } from "react";
import { Box } from "@axelor/ui";

import { EditorOptions, useCompletion, useEditor } from "@/hooks/use-relation";
import { DataRecord } from "@/services/client/data.types";

import { FieldContainer, FieldProps } from "../../builder";
import { Chip } from "../selection";
import { CreatableSelect, CreatableSelectProps } from "./creatable-select";

export function TagSelectComponent(props: CreatableSelectProps) {
  const { targetName } = props.schema;
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
    <CreatableSelect
      isMulti
      optionLabel={targetName}
      optionValue={"id"}
      components={components}
      {...props}
    />
  );
}

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
  const showEditor = useEditor();

  const { title } = attrs;
  const canNew = schema.canNew !== false;
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
    (value: any[]) => setValue(value, true),
    [setValue]
  );

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
          placeholder={placeholder}
          value={value}
          canCreate={canNew}
          onCreate={handleEdit as CreatableSelectProps["onCreate"]}
          onChange={handleChange}
          fetchOptions={handleCompletion}
        />
      )}
    </FieldContainer>
  );
}
