import { useMemo } from "react";
import { useAtom } from "jotai";

import { clsx, Box } from "@axelor/ui";

import { Selection as TSelection } from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names";
import {
  getMultiValues,
  joinMultiValues,
} from "@/views/form/widgets/selection/utils";
import { useSelectionList } from "../selection/hooks";
import convert from "@/utils/convert";

import { FieldControl, FieldProps } from "../../builder";

import styles from "./radio-select.module.scss";

export function RadioSelect(props: FieldProps<string | number | null>) {
  const { schema, readonly, widgetAtom, valueAtom } = props;
  const { direction, nullable, widget } = schema;
  const [value, setValue] = useAtom(valueAtom);

  const values = useMemo(
    () => (value != null ? getMultiValues(value).filter((x) => x) : []),
    [value],
  );
  const selectionList = useSelectionList({ value: values, widgetAtom, schema });

  const isRadio = toKebabCase(widget) === "radio-select";

  function handleClick({ value }: TSelection, checked: boolean) {
    if (readonly) return;
    if (checked) {
      if (isRadio) {
        nullable && setValue(null, true);
      } else {
        setValue(
          joinMultiValues(values.filter((x) => x !== String(value))),
          true,
        );
      }
    } else {
      setValue(
        isRadio
          ? convert(value, { props: schema })
          : joinMultiValues([...values, value] as string[]),
        true,
      );
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
        flexWrap={"wrap"}
        className={clsx({
          [styles.pointer]: !readonly,
          [styles.radio]: isRadio,
          [styles.vertical]: vertical,
        })}
      >
        {selectionList.map((option) => {
          const $values = Array.isArray(values) ? values : `${values ?? ""}`;
          const checked = $values.includes(String(option.value!));
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
