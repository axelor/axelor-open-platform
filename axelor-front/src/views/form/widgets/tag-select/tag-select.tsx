import { useAtom, useAtomValue } from "jotai";
import { MouseEvent, useCallback, useMemo } from "react";

import { Box, SelectProps } from "@axelor/ui";

import {
  EditorOptions,
  useBeforeSelect,
  useCompletion,
  useEditor,
} from "@/hooks/use-relation";
import { DataRecord } from "@/services/client/data.types";

import { FieldControl, FieldProps, usePrepareContext } from "../../builder";
import { Chip } from "../selection";
import { CreatableSelect, CreatableSelectProps } from "./creatable-select";

export function TagSelectComponent({
  onView,
  ...props
}: CreatableSelectProps & {
  onView?: (e: any, value: DataRecord) => void;
}) {
  const { targetName } = props.schema;

  const getOptionLabel = useCallback(
    (option: any) => {
      const trKey = `$t:${targetName}`;
      return option[trKey] ?? option[targetName];
    },
    [targetName]
  );

  const components = useMemo(
    () => ({
      MultiValue: (props: any) => {
        const { data, removeProps } = props;
        return (
          <Box me={1} onMouseDown={(e) => onView?.(e, data)}>
            <Chip
              color={"indigo"}
              title={getOptionLabel(data)}
              onRemove={removeProps.onClick}
            />
          </Box>
        );
      },
    }),
    [getOptionLabel, onView]
  );

  return (
    <CreatableSelect
      isMulti
      optionLabel={getOptionLabel}
      optionValue={"id"}
      components={components}
      {...props}
    />
  );
}

export function TagSelect(
  props: FieldProps<DataRecord[]> & {
    selectProps?: Partial<SelectProps>;
  }
) {
  const { schema, valueAtom, formAtom, widgetAtom, readonly, selectProps } =
    props;
  const { target, targetName, targetSearch, placeholder } = schema;

  const { attrs } = useAtomValue(widgetAtom);

  const [value, setValue] = useAtom(valueAtom);
  const showEditor = useEditor();
  const getContext = usePrepareContext(formAtom);

  const { title, focus, domain } = attrs;
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

  const [beforeSelect, beforeSelectProps] = useBeforeSelect(schema);

  const handleCompletion = useCallback(
    async (value: string) => {
      const _domain = (await beforeSelect(true)) ?? domain;
      const _domainContext = _domain ? getContext() : {};
      const options = {
        _domain,
        _domainContext,
      };
      const { records } = await search(value, options);
      return records;
    },
    [search, domain, beforeSelect, getContext]
  );

  const handleChange = useCallback(
    (value: any[]) =>
      setValue(
        value?.map?.((v) => ({
          id: v.id,
          [targetName]: v[targetName],
        })),
        true
      ),
    [setValue, targetName]
  );

  const getOptionLabel = useCallback(
    (option: any) => {
      const trKey = `$t:${targetName}`;
      return option[trKey] ?? option[targetName];
    },
    [targetName]
  );

  return (
    <FieldControl {...props}>
      {readonly ? (
        <Box d="flex" flexWrap="wrap">
          {(value || []).map((val) => (
            <Box
              m={1}
              key={val?.id}
              as="a"
              href="#"
              onClick={(e: any) => handleView(e, val)}
            >
              <Chip title={getOptionLabel(val)} color={"indigo"} />
            </Box>
          ))}
        </Box>
      ) : (
        <TagSelectComponent
          {...(focus && { key: "focused" })}
          autoFocus={focus}
          schema={schema}
          placeholder={placeholder}
          value={value}
          canCreate={canNew}
          fetchOptions={handleCompletion}
          {...beforeSelectProps}
          {...selectProps}
          onView={handleView}
          onCreate={handleEdit as CreatableSelectProps["onCreate"]}
          onChange={handleChange}
          optionLabel={getOptionLabel}
        />
      )}
    </FieldControl>
  );
}
