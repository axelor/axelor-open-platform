import { ReactElement } from 'react';
import { Box } from '@axelor/ui/core';
import clsx from 'clsx';

import { ChartGroupType } from '../../builder';
import { i18n } from '@/services/client/i18n';
import styles from './bar-group.module.scss';

const TYPES: Record<ChartGroupType, ChartGroupType> = {
  stack: "stack",
  group: "group",
};

function RadioButton({
  active,
  children,
  onClick,
}: {
  active?: boolean;
  children?: ReactElement | string;
  onClick?: (e: React.SyntheticEvent) => void;
}) {
  return (
    <Box
      d="flex"
      alignItems="center"
      me={3}
      className={styles['radio-button']}
      onClick={onClick}
    >
      <Box
        rounded="circle"
        className={clsx(styles.radio, { [styles['radio-active']]: active })}
      />
      <Box as="span" ms={1}>
        {children}
      </Box>
    </Box>
  );
}

export function BarGroup({
  value,
  onChange,
}: {
  value: ChartGroupType;
  onChange: (type: ChartGroupType) => void;
}) {
  return (
    <Box d="flex" position="absolute" className={styles.actions}>
      <RadioButton
        active={value === TYPES.group}
        onClick={() => onChange(TYPES.group)}
      >
        {i18n.get("Grouped")}
      </RadioButton>
      <RadioButton
        active={value === TYPES.stack}
        onClick={() => onChange(TYPES.stack)}
      >
        {i18n.get("Stacked")}
      </RadioButton>
    </Box>
  );
}