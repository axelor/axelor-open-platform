import { useAtom } from "jotai";
import { useCallback } from "react";
import { Box } from "@axelor/ui";
import { clsx } from "@axelor/ui";

import { SelectOptionProps } from "@/components/select";
import { Selection as SelectionType } from "@/services/client/meta.types";

import { FieldProps } from "../../builder";
import { Selection, SelectionTag } from "../selection";
import { getMultiValues, joinMultiValues } from "../selection/utils";
import styles from "./multi-select.module.scss";

export function MultiSelect(props: FieldProps<string | number | null>) {
  const { schema, readonly, valueAtom } = props;
  const { widgetAttrs } = schema;
  const { selectionShowCheckbox = false } = widgetAttrs || {};
  const [value, setValue] = useAtom(valueAtom);

  const removeItem = useCallback(
    (item: SelectionType) => {
      const items = getMultiValues(value);
      const next = joinMultiValues(
        items.filter((x) => String(x) !== String(item.value)),
      );
      setValue(next, true);
    },
    [setValue, value],
  );

  const renderValue = useCallback(
    ({ option }: SelectOptionProps<SelectionType>) => {
      return (
        <SelectionTag
          title={option.title}
          color={option.color}
          onRemove={readonly ? undefined : () => removeItem(option)}
        />
      );
    },
    [readonly, removeItem],
  );

  const deselectOption = useCallback(
    (item: SelectionType) => {
      const items = getMultiValues(value);
      const next = joinMultiValues(
        items.filter((x) => String(x) !== String(item.value)),
      );
      setValue(next, true);
    },
    [setValue, value],
  );

  const renderOption = useCallback(
    ({ option }: SelectOptionProps<SelectionType>) => {
      if (!selectionShowCheckbox) {
        return <SelectionTag title={option.title} color={option.color} />;
      }
      const checked = getMultiValues(value).includes(option.value!);
      return (
        <Box
          key={option.value}
          {...(checked && {
            onClick: () => deselectOption(option),
          })}
          mb={1}
        >
          <Box
            d="flex"
            alignItems="center"
            className={clsx(styles.ibox, {
              [styles.checked]: checked,
            })}
            me={2}
          >
            <Box as="span" className={styles.box} me={2} />
            <SelectionTag title={option.title} color={option.color} />
          </Box>
        </Box>
      );
    },
    [selectionShowCheckbox, deselectOption, value],
  );

  return (
    <Selection
      {...props}
      closeOnSelect={false}
      multiple={true}
      renderValue={renderValue}
      renderOption={renderOption}
    />
  );
}
