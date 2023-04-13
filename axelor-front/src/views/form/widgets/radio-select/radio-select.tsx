import { useAtom, useAtomValue } from "jotai";
import { Box } from "@axelor/ui";
import { FieldContainer, FieldProps } from "../../builder";
import { Selection as TSelection } from "@/services/client/meta.types";
import classes from "./radio-select.module.scss";
import clsx from "clsx";
import { toKebabCase } from "@/utils/names";

export function RadioSelect({
  schema,
  readonly,
  widgetAtom,
  valueAtom,
}: FieldProps<string | number | null>) {
  const { uid, showTitle = true, direction, nullable, widget } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const {
    attrs: { title },
  } = useAtomValue(widgetAtom);
  const selectionList = schema.selectionList as TSelection[];

  const isRadio = toKebabCase(widget) === "radio-select";
  const values = value
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
    <FieldContainer readonly={readonly}>
      {showTitle && <label htmlFor={uid}>{title}</label>}
      <Box
        ps={1}
        pt={1}
        m={0}
        d="flex"
        flexDirection={vertical ? "column" : "row"}
        className={clsx({
          [classes.pointer]: !readonly,
          [classes.radio]: isRadio,
          [classes.vertical]: vertical,
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
                className={clsx(classes.ibox, {
                  [classes.checked]: checked,
                })}
                me={vertical ? 0 : 2}
              >
                <Box as="span" className={classes.box} me={2} />
                <Box as="span">{option.title}</Box>
              </Box>
            </Box>
          );
        })}
      </Box>
    </FieldContainer>
  );
}
