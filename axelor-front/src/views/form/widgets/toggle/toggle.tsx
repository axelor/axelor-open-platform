import { useAtom } from "jotai";
import { useCallback } from "react";

import { Button, clsx } from "@axelor/ui";

import { Icon } from "@/components/icon";
import { FieldControl, FieldProps } from "../../builder";

import styles from "./toggle.module.scss";

export function Toggle(props: FieldProps<boolean>) {
  const { schema, readonly, valueAtom } = props;
  const { uid, icon, iconActive, iconHover } = schema;
  const [value = false, setValue] = useAtom(valueAtom);

  const handleClick = useCallback(
    () => setValue(!value, true),
    [setValue, value],
  );

  const ico = value ? iconActive ?? icon ?? "square-fill" : icon ?? "square";

  return (
    <FieldControl {...props} className={styles.container} pointerEvents="none">
      <Button
        id={uid}
        variant="light"
        className={styles.toggle}
        disabled={readonly}
        onClick={handleClick}
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
