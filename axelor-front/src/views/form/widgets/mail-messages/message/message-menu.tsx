import { i18n } from "@/services/client/i18n";
import { useState } from "react";
import { Message, MessageFlag } from "./types";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import { legacyClassNames } from "@/styles/legacy";
import { Box, Menu, MenuItem } from "@axelor/ui";
import styles from "./message-menu.module.scss";
import clsx from "clsx";

export function MessageMenu({
  data,
  onReply,
  onAction,
  onRemove,
}: {
  data: Message;
  onAction: ($flags: Partial<MessageFlag>, reload?: boolean) => void;
  onReply?: (e: React.SyntheticEvent) => void;
  onRemove?: (data: Message) => void;
}) {
  const [target, setTarget] = useState<HTMLElement | null>(null);

  const open = Boolean(target);
  const t = i18n.get;
  const id = open ? `actions-popover-${data.id}` : undefined;
  const { $flags, $thread, $canDelete } = data;
  const { isRead, isStarred, isArchived } = ($flags || {}) as MessageFlag;

  function onOpen({ target }: React.MouseEvent<HTMLSpanElement>) {
    setTarget(target as HTMLElement);
  }

  function onClose() {
    setTarget(null);
  }

  function onRead() {
    onAction({ ...$flags!, isRead: !isRead });
    onClose();
  }

  function onStarred() {
    onAction({ ...$flags!, isStarred: !isStarred });
    onClose();
  }

  function onArchived() {
    onAction({ ...$flags!, isArchived: !isArchived }, true);
    onClose();
  }

  function onDelete() {
    onRemove && onRemove(data);
    onClose();
  }

  return (
    <>
      <Box
        d="flex"
        alignItems="center"
        position="absolute"
        className={styles.icons}
      >
        {$thread && (
          <Box
            px={1}
            as="span"
            aria-label="reply"
            className={styles.icon}
            onClick={onReply}
          >
            <MaterialIcon icon="reply" />
          </Box>
        )}
        <Box
          px={1}
          as="span"
          aria-describedby={id}
          aria-label="open"
          className={clsx(styles.icon, styles['pull-right'])}
          onClick={onOpen}
        >
          <MaterialIcon icon="arrow_drop_down" />
        </Box>
      </Box>
      <Menu show={open} target={target} onHide={onClose} placement="bottom-end">
        <MenuItem onClick={onRead}>
          {t(isRead ? "Mark as unread" : "Mark as read")}
        </MenuItem>
        <MenuItem onClick={onStarred}>
          {t(isStarred ? "Mark as not important" : "Mark as important")}
        </MenuItem>
        {$thread && (
          <MenuItem onClick={onArchived}>
            {t(isArchived ? "Move to inbox" : "Move to archive")}
          </MenuItem>
        )}
        {$canDelete && <MenuItem onClick={onDelete}>Delete</MenuItem>}
      </Menu>
    </>
  );
}
