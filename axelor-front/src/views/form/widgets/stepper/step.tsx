import { useMemo } from "react";

import { Box, clsx } from "@axelor/ui";
import { Icon } from "@/components/icon";

import styles from "./step.module.scss";

type StepType = {
  width: number;
  index: number;
  selectedIndex: number;
  label?: string;
  icon?: string;
  description?: string;
  completed?: boolean;
  stepperType?: "numeric" | "icon";
  showDescription?: boolean;
  remainingCount?: number;
  readonly?: boolean;
};

export function Step({
  width,
  index,
  selectedIndex,
  label = "",
  icon,
  description,
  completed,
  stepperType,
  showDescription,
  remainingCount,
  readonly,
}: StepType) {
  const isCurrent = useMemo(
    () => index === selectedIndex,
    [index, selectedIndex],
  );

  const isValidated = useMemo(
    () => index < selectedIndex || (completed && isCurrent),
    [index, isCurrent, selectedIndex, completed],
  );

  return (
    <Box className={styles.container} style={{ width }}>
      {(index > 1 || remainingCount) && (
        <Box
          className={clsx(styles.separator, [
            { [styles.active]: isValidated || isCurrent },
          ])}
        />
      )}
      <Box
        className={clsx(styles.stepContainer, [
          { [styles.readonly]: readonly },
        ])}
      >
        <Box
          className={clsx(styles.stepIndicator, [
            {
              [styles.fill]: isValidated,
              [styles.outline]: isCurrent,
            },
          ])}
        >
          {remainingCount ? (
            `+${remainingCount}`
          ) : stepperType === "icon" ? (
            <Icon icon={icon ?? "apps"} fontSize={16} />
          ) : (
            index
          )}
        </Box>
        <Box textAlign="center" textWrap={true}>
          {label}
        </Box>
        {showDescription && (
          <Box className={styles.description}>{description}</Box>
        )}
      </Box>
    </Box>
  );
}
