import { SchedulerEvent } from "@/components/scheduler";
import { DataRecord } from "@/services/client/data.types";
import { Box, Input } from "@axelor/ui";

import styles from "./calendar.module.scss";

export interface Filter {
  color?: string;
  title?: string;
  value?: number | string;
  match?(event: SchedulerEvent<DataRecord>): boolean;
  checked?: boolean;
}

export function Filters({
  data,
  onChange,
}: {
  data: Filter[];
  onChange(ind: number): void;
}) {
  return (
    <Box d="flex" flexDirection="column">
      {data.map(({ title, value, color, checked = false }, ind) => (
        <Box d="flex" alignItems="center" key={value} m={1}>
          <Input
            className={styles.checkbox}
            type="checkbox"
            style={{
              ...(checked ? { backgroundColor: color } : {}),
              borderColor: color,
            }}
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
            className={styles["checkbox-label"]}
            onClick={() => onChange(ind)}
          >
            {title}
          </Box>
        </Box>
      ))}
    </Box>
  );
}
