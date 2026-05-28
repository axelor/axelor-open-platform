import { useAtom } from "jotai";
import { useCallback, useId } from "react";

import { Button, clsx } from "@axelor/ui";

import { Icon } from "@/components/icon";
import { FieldControl, FieldProps } from "../../builder";

import styles from "./toggle.module.scss";

export function Toggle(props: FieldProps<boolean>) {
  const { schema, readonly, valueAtom } = props;
  const { icon, iconActive, iconHover } = schema;
  const [value = false, setValue] = useAtom(valueAtom);

  const id = useId();

  const handleClick = useCallback(
    () => setValue(!value, true),
    [setValue, value]
  );

  const ico = value ? (iconActive ?? icon ?? "square-fill") : (icon ?? "square");

  if (readonly) {
    return (
      <FieldControl {...props} inputId={id} className={styles.container}>
        <Icon
          icon={ico}
          className={clsx(styles.readonlyIcon, {
            [styles.active]: !!value,
          })}
          data-testid="input"
        />
      </FieldControl>
    );
  }

  return (
    <FieldControl {...props} inputId={id} className={styles.container}>
      <Button
        id={id}
        variant="light"
        className={styles.toggle}
        onClick={handleClick}
        data-testid="input"
        aria-pressed={value || undefined}
      >
        <Icon
          icon={ico}
          className={clsx(styles.icon, {
            [styles.active]: !!value,
          })}
        />
        <Icon
          icon={iconHover ?? ico}
          className={clsx(styles.icon, styles.hoverIcon, {
            [styles.active]: !!value,
          })}
        />
      </Button>
    </FieldControl>
  );
}
