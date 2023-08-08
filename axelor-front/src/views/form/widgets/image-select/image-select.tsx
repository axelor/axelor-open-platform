import { Box, Select, SelectComponents } from "@axelor/ui";
import { useAtom, useAtomValue } from "jotai";
import { FunctionComponent, useCallback, useMemo } from "react";

import {Icon} from "@/components/icon";
import { Schema, Selection as TSelection } from "@/services/client/meta.types";
import { FieldControl, FieldProps } from "../../builder";
import { useSelectionList } from "../selection/hooks";
import styles from "./image-select.module.scss";

function Image({
  showLabel,
  option,
}: {
  showLabel?: boolean;
  option: TSelection;
}) {
  const icon = option?.icon || option?.value;
  const text = option?.title;

  return (
    <Box d="flex" gap={6} alignItems="center">
      {icon && !icon.includes(".") ? (
        <Icon icon={icon} />
      ) : (
        <img
          style={
            showLabel === false
              ? { maxHeight: 18 }
              : { maxWidth: 18, height: "fit-content" }
          }
          src={icon}
          alt={text}
        />
      )}
      {showLabel !== false && text}
    </Box>
  );
}

export function ImageSelectText({
  schema,
  value,
  className,
}: {
  schema: Schema;
  className?: string;
  value?: string | number | null;
}) {
  const { labels } = schema;
  const selectionList = (schema.selectionList ?? []) as TSelection[];
  const selected = selectionList.find(
    (item) => String(item.value) === String(value)
  );
  return (
    <Box className={className} d="flex" alignItems="center">
      {selected && <Image showLabel={labels} option={selected} />}
    </Box>
  );
}

export function ImageSelect(props: FieldProps<string | number | null>) {
  const { schema, readonly, invalid, widgetAtom, valueAtom } = props;
  const { labels: showLabel, placeholder } = schema;
  const {
    attrs: { focus },
  } = useAtomValue(widgetAtom);
  const [value, setValue] = useAtom(valueAtom);
  const selectionList = useSelectionList({ schema, widgetAtom, value });

  const hasValue = (value ?? null) !== null;
  const selectValue = hasValue
    ? selectionList.find((item) => String(item.value) === String(value))
    : null;

  const handleChange = useCallback(
    (e: any) => setValue(e?.value, true),
    [setValue]
  );

  const components: Record<string, FunctionComponent<any>> = useMemo(() => {
    return {
      SingleValue: (props: any) => {
        const { data } = props;
        return (
          <SelectComponents.SingleValue {...props} className={styles.value}>
            <Image option={data} showLabel={showLabel} />
          </SelectComponents.SingleValue>
        );
      },
      Option: (props: any) => {
        const { data } = props;
        return (
          <SelectComponents.Option {...props}>
            <Image option={data} showLabel={showLabel} />
          </SelectComponents.Option>
        );
      },
    };
  }, [showLabel]);

  return (
    <FieldControl {...props}>
      {readonly && (
        <ImageSelectText
          className={styles.label}
          schema={schema}
          value={value}
        />
      )}
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
          isSearchable={false}
          icons={[{ icon: "arrow_drop_down" }]}
          components={components}
        />
      )}
    </FieldControl>
  );
}
