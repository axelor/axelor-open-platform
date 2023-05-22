import React, { useCallback, useMemo, useState } from "react";
import { Box, Input, Button, Menu, MenuItem } from "@axelor/ui";
import { Kanban } from "@axelor/ui/kanban";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import clsx from "clsx";

import { Loader } from "@/components/loader/loader";
import { i18n } from "@/services/client/i18n";
import { KanbanColumn, KanbanRecord } from "./types";
import { legacyClassNames } from "@/styles/legacy";
import styles from "./kanban-board.module.scss";

interface KanbanBoardProps {
  columns: KanbanColumn[];
  columnWidth?: string | number;
  responsive?: boolean;
  readonly?: boolean;
  components: { Card: React.JSXElementConstructor<{ record: KanbanRecord }> };
  onLoadMore?: ({ column }: { column: KanbanColumn }) => void;
  onCardMove?: ({
    column,
    index,
    source,
    record,
  }: {
    column: KanbanColumn;
    index: number;
    source: KanbanColumn;
    record: KanbanRecord;
  }) => void;
  onCardAdd?: ({
    record,
    column,
  }: {
    column: KanbanColumn;
    record: KanbanRecord;
  }) => void;
  onCardClick?: ({ record }: { record: KanbanRecord }) => void;
  onCardDelete?: ({
    column,
    record,
  }: {
    column: KanbanColumn;
    record: KanbanRecord;
  }) => void;
  onCardEdit?: ({ record }: { record: KanbanRecord }) => void;
}

function RecordRenderer({
  record,
  column,
  disabled,
  onCardEdit,
  onCardDelete,
  onCardClick,
  Card,
}: {
  record: KanbanRecord;
  column: KanbanColumn;
  disabled?: boolean;
  onCardEdit: KanbanBoardProps["onCardEdit"];
  onCardDelete: KanbanBoardProps["onCardDelete"];
  onCardClick: KanbanBoardProps["onCardClick"];
  Card: KanbanBoardProps["components"]["Card"];
}) {
  const { canDelete = true, canEdit = true } = column;
  const [target, setTarget] = useState<HTMLElement | null>(null);

  const showMenu = useCallback((event: React.SyntheticEvent) => {
    event.preventDefault();
    setTarget(event.currentTarget as HTMLElement);
  }, []);

  const closeMenu = useCallback(() => {
    setTarget(null);
  }, []);

  const isMenuOpen = Boolean(target);

  const menuItems = [
    {
      active: canEdit,
      onClick: () => onCardEdit && onCardEdit({ record }),
      label: i18n.get("Edit"),
      show: Boolean(onCardEdit),
    },
    {
      active: canDelete,
      onClick: () => onCardDelete && onCardDelete({ record, column }),
      label: i18n.get("Delete"),
      show: Boolean(onCardDelete),
    },
  ].filter(({ show }) => show);

  return (
    <Box
      className={styles["record"]}
      ps={1}
      pe={1}
      d="flex"
      alignItems="flex-start"
      justifyContent="space-between"
      position="relative"
      onClick={(e) =>
        !e.defaultPrevented &&
        onCardClick &&
        canEdit &&
        !isMenuOpen &&
        onCardClick({ record })
      }
    >
      <Box w={100}>
        <Card record={record} />
      </Box>
      <Box
        className={styles["record-action"]}
        {...(isMenuOpen ? { d: "block" } : {})}
      >
        {!disabled && menuItems.length > 0 && (
          <>
            <Box as="a" onClick={showMenu}>
              <MaterialIcon icon="arrow_drop_down" />
            </Box>
            <Menu
              placement="bottom-end"
              target={target}
              offset={[0, 0]}
              show={isMenuOpen}
              onHide={closeMenu}
            >
              {menuItems.map(
                ({ active, onClick, label, show }) =>
                  active &&
                  show && (
                    <MenuItem
                      key={label}
                      onClick={() => {
                        closeMenu();
                        onClick();
                      }}
                    >
                      {label}
                    </MenuItem>
                  )
              )}
            </Menu>
          </>
        )}
      </Box>
    </Box>
  );
}

function ColumnRenderer({
  column,
  readonly,
  overflow,
  onLoadMore,
  onCardAdd,
  RecordList,
}: {
  column: KanbanColumn;
  readonly?: boolean;
  overflow?: boolean;
  RecordList: any;
  onLoadMore?: KanbanBoardProps["onLoadMore"];
  onCardAdd?: KanbanBoardProps["onCardAdd"];
}) {
  const { title, canCreate, loading, hasMore } = column;
  const noData = !loading && column.records?.length === 0;
  const [text, setText] = useState("");

  function handleAdd() {
    setText("");
    onCardAdd?.({
      record: { text } as KanbanRecord,
      column,
    });
  }

  return (
    <Box
      p={1}
      mx={1}
      h={100}
      d="flex"
      rounded={3}
      flexGrow={1}
      flexDirection="column"
      className={legacyClassNames(styles["column"], "kanban-column")}
    >
      <Box as="h6" mb={1} p={2} className={styles["column-title"]}>
        {title}
      </Box>
      {canCreate && onCardAdd && (
        <Box d="flex" px={1} mb={2}>
          <Input value={text} onChange={(e) => setText(e.target.value)} />
          &nbsp;
          <Button
            disabled={!text.trim()}
            bg="primary"
            mx={1}
            size="sm"
            color="white"
            onClick={handleAdd}
          >
            {i18n.get("Add")}
          </Button>
        </Box>
      )}
      <Box
        pt={2}
        className={clsx(styles["record-list-wrapper"], {
          [styles["record-list-wrapper-overflow"]]: overflow,
          [styles["record-list-wrapper-no-data"]]: noData,
        })}
      >
        <RecordList
          column={column}
          readonly={readonly}
          className={styles["record-list"]}
        />
        {hasMore && (
          <Box d="flex" justifyContent="center" pb={1}>
            <Button
              onClick={() => onLoadMore?.({ column })}
              outline
              size="sm"
              variant="secondary"
            >
              {i18n.get("Load more")}
            </Button>
          </Box>
        )}
        {noData && (
          <Box as="p" m={0} textAlign="center" className={styles.noRecordsText}>
            {i18n.get("No records found.")}
          </Box>
        )}
        {loading && (
          <Box d="flex" h={100}>
            <Loader />
          </Box>
        )}
      </Box>
    </Box>
  );
}

export function KanbanBoard({
  columns,
  columnWidth,
  readonly,
  responsive,
  components,
  onLoadMore,
  onCardAdd,
  onCardMove,
  onCardClick,
  onCardDelete,
  onCardEdit,
}: KanbanBoardProps) {
  const { Card } = components;

  const rtl = false;

  const $columns = useMemo(
    () =>
      columns.map((c) => ({
        ...c,
        id: `column-${c.title}-${c.name}`,
        records: c.records?.map((r: KanbanRecord) => ({
          ...r,
          title: r.title || r.name,
          renderer: ({
            record,
            column,
          }: {
            record: KanbanRecord;
            column: KanbanColumn;
          }) => (
            <RecordRenderer
              {...{
                record,
                column,
                onCardClick,
                onCardDelete,
                onCardEdit,
                Card,
              }}
            />
          ),
        })) as KanbanRecord[],
        disableDrag: true,
        renderer: ({
          column,
          readonly,
          RecordList,
        }: {
          column: KanbanColumn;
          readonly: boolean;
          RecordList: React.JSXElementConstructor<{
            column: KanbanColumn;
            readonly: boolean;
          }>;
        }) => (
          <ColumnRenderer
            {...{
              column,
              readonly,
              RecordList,
              overflow: !responsive,
              onCardAdd,
              onLoadMore,
            }}
          />
        ),
      })) as KanbanColumn[],
    [
      columns,
      responsive,
      onLoadMore,
      onCardAdd,
      onCardClick,
      onCardDelete,
      onCardEdit,
      Card,
    ]
  );

  const ColumnProps = useMemo(
    () => ({
      className: styles["column-wrapper"],
      style: { width: columnWidth },
    }),
    [columnWidth]
  );

  const className = clsx(styles.board, {
    [styles["board-responsive"]]: responsive,
    [styles.rtl]: rtl,
  });

  return (
    <Kanban
      readonly={readonly}
      columns={$columns}
      className={className}
      onCardMove={onCardMove}
      ColumnProps={ColumnProps}
    />
  );
}
