import { Box, Button } from "@axelor/ui";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtom } from "jotai";
import { legacyClassNames } from "@/styles/legacy";
import classes from "./toggle.module.scss";

export function Toggle({ schema, readonly, valueAtom }: FieldProps<boolean>) {
  const { uid, title, icon, iconActive, iconHover } = schema;
  const [value = false, setValue] = useAtom(valueAtom);
  return (
    <FieldContainer className={classes.container} readonly={readonly}>
      <label htmlFor={uid}>{title}</label>
      <Button variant="light" id={uid} onClick={() => !readonly && setValue(!value, true)}>
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
