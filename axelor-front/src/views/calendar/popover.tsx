import { useCallback, useMemo } from "react";

import { ClickAwayListener, Divider, Popper } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { SchedulerEvent } from "@/components/scheduler";
import { DataRecord } from "@/services/client/data.types";
import { moment } from "@/services/client/l10n";

import styles from "./popover.module.scss";

export type PopoverProps = {
  event: SchedulerEvent<DataRecord>;
  element: HTMLElement;
  onEdit?: (record: DataRecord) => void;
  onDelete?: (record: DataRecord) => void;
  onClose: () => void;
};

export function Popover({
  element,
  event,
  onEdit,
  onDelete,
  onClose,
}: PopoverProps) {
  const { title, allDay, start, end, backgroundColor, data: record } = event;
  const subtitle = useMemo(() => {
    const startDate = moment(start).format("LL");
    const startTitle = moment(start).format("HH:mm");
    const endDate = moment(end).format("LL");
    const endTitme = moment(end).format("HH:mm");
    return allDay
      ? startDate === endDate
        ? `${startDate}`
        : `${startDate} ⋅ ${endDate}`
      : startDate === endDate
        ? `${startDate} ⋅ ${startTitle} – ${endTitme}`
        : `${startDate} ${startTitle} ⋅ ${endDate} – ${endTitme}`;
  }, [allDay, end, start]);

  const handleEdit = useCallback(() => {
    record && onEdit?.(record);
  }, [onEdit, record]);

  const handleDelete = useCallback(() => {
    record && onDelete?.(record);
  }, [onDelete, record]);

  return (
    <Popper open shadow rounded arrow target={element} placement="bottom">
      <ClickAwayListener onClickAway={onClose}>
        <div className={styles.popover}>
          <div className={styles.header}>
            <div className={styles.actions}>
              {onEdit && <MaterialIcon icon="edit" onClick={handleEdit} />}
              {onDelete && (
                <MaterialIcon icon="delete" onClick={handleDelete} />
              )}
              {(onEdit || onDelete) && <Divider vertical />}
              <MaterialIcon icon="close" onClick={onClose} />
            </div>
          </div>
          <div className={styles.body}>
            <div className={styles.item}>
              <div className={styles.icon}>
                <div className={styles.color} style={{ backgroundColor }} />
              </div>
              <div className={styles.title}>{title}</div>
            </div>
            <div className={styles.item}>
              <div className={styles.icon} />
              <div className={styles.subtitle}>{subtitle}</div>
            </div>
          </div>
        </div>
      </ClickAwayListener>
    </Popper>
  );
}
