import { Box, Button } from "@axelor/ui";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtom, useAtomValue } from "jotai";
import { legacyClassNames } from "@/styles/legacy";
import classes from "./toggle.module.scss";

export function Toggle({
  schema,
  readonly,
  widgetAtom,
  valueAtom,
}: FieldProps<boolean>) {
  const { uid, icon, iconActive, iconHover, showTitle = true } = schema;
  const [value = false, setValue] = useAtom(valueAtom);
  const {
    attrs: { title },
  } = useAtomValue(widgetAtom);

  return (
    <FieldContainer className={classes.container} readonly={readonly}>
      {showTitle && <label htmlFor={uid}>{title}</label>}
      <Button
        variant="light"
        id={uid}
        onClick={() => !readonly && setValue(!value, true)}
      >
        <Box
          as="i"
          me={2}
          className={legacyClassNames(
            "fa",
            (value ? iconActive : icon) || icon,
            { [classes.icon]: iconHover }
          )}
        />
        {iconHover && (
          <Box
            as="i"
            me={2}
            className={legacyClassNames(classes.hoverIcon, "fa", iconHover)}
          />
        )}
      </Button>
    </FieldContainer>
  );
}
