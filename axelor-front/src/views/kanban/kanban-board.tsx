import clsx from "clsx";
import React, { useCallback, useMemo, useState } from "react";

import { Box, Button, CommandBar, CommandItemProps, Input } from "@axelor/ui";
import { Kanban } from "@axelor/ui/kanban";

import { Loader } from "@/components/loader/loader";
import { i18n } from "@/services/client/i18n";
import { legacyClassNames } from "@/styles/legacy";

import { KanbanColumn, KanbanRecord } from "./types";

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
  onCardEdit,
  onCardDelete,
  onCardClick,
  Card,
}: {
  record: KanbanRecord;
  column: KanbanColumn;
  onCardEdit: KanbanBoardProps["onCardEdit"];
  onCardDelete: KanbanBoardProps["onCardDelete"];
  onCardClick: KanbanBoardProps["onCardClick"];
  Card: KanbanBoardProps["components"]["Card"];
}) {
  const canEdit = Boolean(onCardEdit);
  const canDelete = Boolean(onCardDelete);

  const commandItems: CommandItemProps[] = [
    {
      key: "menu",
      iconProps: { icon: "arrow_drop_down" },
      items: [
        {
          key: "edit",
          text: i18n.get("Edit"),
          hidden: !canEdit,
          onClick: () => onCardEdit && onCardEdit({ record }),
        },
        {
          key: "delete",
          text: i18n.get("Delete"),
          hidden: !canDelete,
          onClick: () => onCardDelete && onCardDelete({ record, column }),
        },
      ],
    },
  ];

  const showActions = canEdit || canDelete;

  const handleClick = useCallback(
    (e: React.MouseEvent) => {
      if (canEdit && onCardClick) {
        onCardClick({ record });
      }
    },
    [canEdit, onCardClick, record],
  );

  return (
    <Box
      className={styles["record"]}
      d="flex"
      alignItems="flex-start"
      justifyContent="space-between"
      position="relative"
    >
      <Box w={100} onClick={handleClick}>
        <Card record={record} />
      </Box>
      {showActions && (
        <CommandBar className={styles["record-action"]} items={commandItems} />
      )}
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
        <Box d="flex" g={2}>
          <Input value={text} onChange={(e) => setText(e.target.value)} />
          <Button disabled={!text.trim()} variant="primary" onClick={handleAdd}>
            {i18n.get("Add")}
          </Button>
        </Box>
      )}
      <Box
        className={clsx(styles["record-list-wrapper"], {
          [styles["overflow"]]: overflow,
          [styles["no-data"]]: noData,
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
          <Box
            as="p"
            m={0}
            textAlign="center"
            className={styles["no-records-text"]}
          >
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

  const $columns = useMemo(
    () =>
      columns.map((c) => ({
        ...c,
        id: `column-${c.title}-${c.name}`,
        records: c.records?.map((record: KanbanRecord) => ({
          ...record,
          title: record.title || record.name,
          renderer: ({
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
    ],
  );

  const ColumnProps = useMemo(
    () => ({
      className: styles["column-wrapper"],
      style: { width: columnWidth },
    }),
    [columnWidth],
  );

  const className = clsx(styles.board, {
    [styles.responsive]: responsive,
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
