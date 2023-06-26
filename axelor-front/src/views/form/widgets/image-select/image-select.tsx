import { useAtom, useAtomValue } from "jotai";
import { FunctionComponent, useCallback, useMemo } from "react";
import { Box, Select, SelectComponents } from "@axelor/ui";

import { Schema, Selection as TSelection } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
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
  const image = option?.icon || option?.value;
  const text = option?.title;

  return (
    <>
      {image && image.includes("fa-") ? (
        <Box as={"i"} className={legacyClassNames("fa", image)} />
      ) : (
        <img
          style={showLabel === false ? { maxHeight: 18 } : { maxWidth: 18 }}
          src={image}
          alt={text}
        />
      )}
      {showLabel !== false && text}
    </>
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

  const handleClear = useCallback(() => {
    setValue(null);
  }, [setValue]);

  const handleChange = useCallback(
    (e: any) => setValue(e.value, true),
    [setValue]
  );

  const components: Record<string, FunctionComponent<any>> = useMemo(() => {
    return {
      SingleValue: (props: any) => {
        const { data } = props;
        return (
          <SelectComponents.SingleValue {...props}>
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
          autoFocus={focus}
          invalid={invalid}
          value={selectValue ?? null}
          onChange={handleChange}
          options={selectionList}
          optionLabel="title"
          optionValue="value"
          placeholder={placeholder}
          isSearchable={false}
          icons={
            hasValue
              ? [
                  {
                    icon: "close",
                    onClick: handleClear,
                  },
                ]
              : []
          }
          components={components}
        />
      )}
    </FieldControl>
  );
}
