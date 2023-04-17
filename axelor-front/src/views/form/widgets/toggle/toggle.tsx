import { legacyClassNames } from "@/styles/legacy";
import { Button } from "@axelor/ui";
import { useAtom, useAtomValue } from "jotai";
import { useCallback } from "react";
import { FieldContainer, FieldProps } from "../../builder";
import styles from "./toggle.module.scss";

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

  const handleClick = useCallback(() => {
    setValue(!value, true);
  }, [setValue, value]);

  const ico = value ? iconActive ?? icon : icon;

  return (
    <FieldContainer className={styles.container} readonly={readonly}>
      {showTitle && <label htmlFor={uid}>{title}</label>}
      <Button
        id={uid}
        variant="light"
        disabled={readonly}
        onClick={handleClick}
      >
        <i
          className={legacyClassNames("fa", ico, styles.icon, {
            [styles.active]: !!value,
          })}
        />
        <i
          className={legacyClassNames(
            "fa",
            iconHover ?? ico,
            styles.icon,
            styles.hoverIcon,
            {
              [styles.active]: !!value,
            }
          )}
        />
      </Button>
    </FieldContainer>
  );
}
