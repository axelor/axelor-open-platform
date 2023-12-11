import clsx from "clsx";
import { useAtom } from "jotai";

import { Box } from "@axelor/ui";

import { Selection as TSelection } from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names";

import { FieldControl, FieldProps } from "../../builder";

import styles from "./radio-select.module.scss";

export function RadioSelect(props: FieldProps<string | number | null>) {
  const { schema, readonly, valueAtom } = props;
  const { direction, nullable, widget } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const selectionList = (schema.selectionList as TSelection[]) ?? [];

  const isRadio = toKebabCase(widget) === "radio-select";
  const values = value != null
    ? String(value)
        .split(",")
        .filter((x) => x)
    : [];

  function handleClick({ value }: TSelection, checked: boolean) {
    if (readonly) return;
    if (checked) {
      if (isRadio) {
        nullable && setValue(null, true);
      } else {
        setValue(values.filter((x) => x !== value).join(","), true);
      }
    } else {
      setValue(isRadio ? value : [...values, value].join(","), true);
    }
  }
  const vertical = direction === "vertical";

  return (
    <FieldControl {...props}>
      <Box
        ps={1}
        pt={1}
        m={0}
        d="flex"
        flexDirection={vertical ? "column" : "row"}
        className={clsx({
          [styles.pointer]: !readonly,
          [styles.radio]: isRadio,
          [styles.vertical]: vertical,
        })}
      >
        {selectionList.map((option) => {
          const $values = Array.isArray(values) ? values : `${values ?? ""}`;
          const checked = $values.includes(option.value!);
          return (
            <Box
              key={option.value}
              onClick={() => handleClick(option, checked)}
              mb={1}
            >
              <Box
                d="flex"
                alignItems="center"
                className={clsx(styles.ibox, {
                  [styles.checked]: checked,
                })}
                me={vertical ? 0 : 2}
              >
                <Box as="span" className={styles.box} me={2} />
                <Box as="span">{option.title}</Box>
              </Box>
            </Box>
          );
        })}
      </Box>
    </FieldControl>
  );
}
