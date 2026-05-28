import { useId, useCallback, useState } from "react";

import { clsx, Box, Menu, MenuItem } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { i18n } from "@/services/client/i18n";
import { session } from "@/services/client/session";

import { Message, MessageFlag } from "./types";

import styles from "./message-menu.module.scss";

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
  const [show, setShow] = useState(false);

  const baseId = useId();
  const menuId = `${baseId}-menu`;
  const _t = i18n.get;
  const { $flags, $thread, createdBy } = data;
  const $canDelete = createdBy?.id === session.info?.user?.id;
  const { isRead, isStarred, isArchived } = ($flags || {}) as MessageFlag;

  const showMenu = useCallback(() => {
    setShow(true);
  }, []);

  const hideMenu = useCallback(() => {
    setShow(false);
  }, []);

  function onRead() {
    onAction({ isRead: !isRead });
    hideMenu();
  }

  function onStarred() {
    onAction({ isStarred: !isStarred });
    hideMenu();
  }

  function onArchived() {
    onAction({ isArchived: !isArchived }, true);
    hideMenu();
  }

  function onDelete() {
    onRemove && onRemove(data);
    hideMenu();
  }

  return (
    <>
      <Box
        d="flex"
        alignItems="center"
        position="absolute"
        className={styles.icons}
        data-testid={"message-actions"}
      >
        {$thread && (
          <Box
            px={1}
            as="span"
            aria-label="reply"
            className={styles.icon}
            onClick={onReply}
            data-testid={"btn-message-reply"}
          >
            <MaterialIcon icon="reply" />
          </Box>
        )}
        <Box
          id={baseId}
          ref={setTarget}
          px={1}
          as="span"
          className={clsx(styles.icon, styles["pull-right"])}
          onClick={showMenu}
          aria-label={_t("Message actions")}
          aria-haspopup="menu"
          aria-expanded={show}
          aria-controls={menuId}
          data-testid={"btn-message-actions"}
        >
          <MaterialIcon icon="arrow_drop_down" />
        </Box>
      </Box>
      <Menu
        show={show}
        target={target}
        onHide={hideMenu}
        placement="bottom-end"
        data-testid={"message-menu"}
      >
        <MenuItem onClick={onRead} data-testid={"btn-mark-as-read"}>
          {isRead ? _t("Mark as unread") : _t("Mark as read")}
        </MenuItem>
        <MenuItem onClick={onStarred} data-testid={"btn-mark-as-important"}>
          {isStarred ? _t("Mark as not important") : _t("Mark as important")}
        </MenuItem>
        {$thread && (
          <MenuItem onClick={onArchived} data-testid={"btn-archive"}>
            {isArchived ? _t("Move to inbox") : _t("Move to archive")}
          </MenuItem>
        )}
        {$canDelete && (
          <MenuItem onClick={onDelete} data-testid={"btn-delete"}>
            {_t("Delete")}
          </MenuItem>
        )}
      </Menu>
    </>
  );
}
