import { useAtom, useAtomValue } from "jotai";
import { FunctionComponent, useCallback, useMemo } from "react";

import { Box, Select, SelectComponents, SelectProps } from "@axelor/ui";

import { Schema } from "@/services/client/meta.types";

import { FieldControl, FieldProps } from "../../builder";
import { useSelectionList } from "./hooks";
import { SelectionTag } from "./selection-tag";
import { SelectionText } from "./selection-text";

export function Selection({
  selectComponents = [],
  selectProps,
  ...props
}: FieldProps<string | number | null> & {
  selectProps?: Partial<SelectProps>;
  selectComponents?: SelectComponent[];
}) {
  const { schema, readonly, invalid, widgetAtom, valueAtom } = props;
  const { placeholder } = schema;
  const { isMulti } = selectProps || {};
  const {
    attrs: { focus },
  } = useAtomValue(widgetAtom);
  const [value, setValue] = useAtom(valueAtom);
  const selectionList = useSelectionList({ schema, widgetAtom, value });

  const hasValue = (value ?? null) !== null;
  const selectValue = hasValue
    ? (() => {
        if (isMulti) {
          const values = String(value ?? "").split(",");
          return selectionList.filter((item) =>
            values.includes(String(item.value)),
          );
        }
        return selectionList.find(
          (item) => String(item.value) === String(value),
        );
      })()
    : null;

  const handleClear = useCallback(() => {
    setValue(null);
  }, [setValue]);

  const handleChange = useCallback(
    (e: any) => {
      const value = isMulti
        ? Array.isArray(e)
          ? e.map((e) => e?.value).join(",")
          : e
        : e && e.value;
      setValue(value, true);
    },
    [isMulti, setValue],
  );

  const components = useComponents(schema, ["Option", ...selectComponents]);

  return (
    <FieldControl {...props}>
      {readonly && <SelectionText schema={schema} value={value} />}
      {readonly || (
        <Select
          {...(focus && { key: "focused" })}
          autoFocus={focus}
          invalid={invalid}
          value={selectValue ?? null}
          onChange={handleChange}
          options={selectionList}
          optionLabel="title"
          optionValue="value"
          placeholder={placeholder}
          icons={[
            {
              icon: "close",
              hidden: !hasValue,
              onClick: handleClear,
            },
            { icon: "arrow_drop_down" },
          ]}
          {...selectProps}
          components={components}
        />
      )}
    </FieldControl>
  );
}

type SelectComponent = "Option" | "SingleValue" | "MultiValue";

function useComponents(schema: Schema, componentList: SelectComponent[]) {
  const { colorField = "color" } = schema;
  const componentListAsStr = componentList.join(",");

  return useMemo(() => {
    const components: Record<SelectComponent, FunctionComponent<any>> = {
      MultiValue: (props: any) => {
        const { data, removeProps } = props;
        return (
          <Box me={1}>
            <SelectionTag
              color={data?.[colorField]}
              title={data?.title}
              onRemove={removeProps.onClick}
            />
          </Box>
        );
      },
      SingleValue: (props: any) => {
        const { data } = props;
        return (
          <SelectComponents.SingleValue {...props}>
            <SelectionTag color={data?.[colorField]} title={data?.title} />
          </SelectComponents.SingleValue>
        );
      },
      Option: (props: any) => {
        const { data } = props;
        const color = data?.[colorField];
        if (!color) {
          return <SelectComponents.Option {...props} />;
        }
        return (
          <SelectComponents.Option {...props}>
            <SelectionTag color={color} title={data?.title} />
          </SelectComponents.Option>
        );
      },
    };
    return componentListAsStr.split(",").reduce(
      (obj, key) => ({
        ...obj,
        [key]: components[key as SelectComponent],
      }),
      {},
    );
  }, [colorField, componentListAsStr]);
}
