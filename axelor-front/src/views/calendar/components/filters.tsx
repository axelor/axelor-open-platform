import React from 'react';
import { Input, Box } from '@axelor/ui';

import { Filter } from './types';
import styles from '../calendar.module.scss';

export default function FilterList({
  data,
  onChange,
}: {
  data: Filter[];
  onChange(ind: number): void;
}) {
  return (
    <Box d="flex" flexDirection="column">
      {data.map(({ label, value, color, checked = false }, ind) => (
        <Box d="flex" alignItems="center" key={value} m={1}>
          <Input
            className={styles.checkbox}
            type="checkbox"
            style={{ ...(checked ? { backgroundColor: color } : {}), borderColor: color }}
            onChange={() => onChange(ind)}
            value={value}
            checked={checked}
            m={0}
            me={1}
          />
          <Box
            as="p"
            mb={0}
            style={{ color }}
            className={styles['checkbox-label']}
            onClick={() => onChange(ind)}
          >
            {label}
          </Box>
        </Box>
      ))}
    </Box>
  );
}
