import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import {
  Box,
  Button,
  clsx,
  CommandBar,
  CommandItemProps,
  Input,
} from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";
import { Kanban } from "@axelor/ui/kanban";

import { Loader } from "@/components/loader/loader";
import { i18n } from "@/services/client/i18n";

import { KanbanColumn, KanbanRecord } from "./types";

import styles from "./kanban-board.module.scss";

interface KanbanBoardProps {
  columns: KanbanColumn[];
  columnWidth?: string | number;
  responsive?: boolean;
  readonly?: boolean;
  components: { Card: React.JSXElementConstructor<{ record: KanbanRecord }> };
  onCollapse?: (column: KanbanColumn) => void;
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
  onCardClick?: ({
    record,
    column,
  }: {
    record: KanbanRecord;
    column?: KanbanColumn;
  }) => void;
  onCardDelete?: ({
    column,
    record,
  }: {
    column: KanbanColumn;
    record: KanbanRecord;
  }) => void;
  onCardEdit?: ({
    record,
    column,
  }: {
    record: KanbanRecord;
    column?: KanbanColumn;
  }) => void;
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
          onClick: () => onCardEdit && onCardEdit({ record, column }),
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
        onCardClick({ record, column });
      }
    },
    [canEdit, onCardClick, record, column],
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
  onCollapse,
  onLoadMore,
  onCardAdd,
  RecordList,
  scrollTop,
  handleScroll,
}: {
  column: KanbanColumn;
  readonly?: boolean;
  overflow?: boolean;
  RecordList: any;
  onCollapse?: KanbanBoardProps["onCollapse"];
  onLoadMore?: KanbanBoardProps["onLoadMore"];
  onCardAdd?: KanbanBoardProps["onCardAdd"];
  scrollTop?: number;
  handleScroll?: (e: React.SyntheticEvent) => void;
}) {
  const [text, setText] = useState("");

  const { title, canCreate, loading } = column;
  const page = column.dataStore.page;
  const hasMore = (page.totalCount ?? 0) > (column?.records?.length ?? 0);
  const noData = !loading && !page.totalCount;

  function handleAdd() {
    setText("");
    onCardAdd?.({
      record: { text } as KanbanRecord,
      column,
    });
  }

  const handleCollapse = useCallback(() => {
    onCollapse?.(column);
  }, [column, onCollapse]);

  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (container && scrollTop != null) {
      container.scrollTop = scrollTop;
    }
  }, [scrollTop]);

  if (column.collapsed) {
    return (
      <Box
        d="flex"
        flexDirection="column"
        rounded={3}
        p={3}
        className={styles.collapsed}
      >
        <Button onClick={handleCollapse} border={false} m={0} p={2}>
          <BootstrapIcon icon="chevron-right" />
        </Button>
        <Box
          as="h6"
          className={`${styles["column-title"]} ${styles["collapsed-column-title"]}`}
        >
          {title}
        </Box>
      </Box>
    );
  }

  return (
    <Box
      p={3}
      gap="0.5rem"
      h={100}
      d="flex"
      rounded={3}
      flexGrow={1}
      flexDirection="column"
      className={styles.column}
    >
      <Box d="flex" mb={1} p={2} justifyContent="space-between">
        <Box d="flex">
          <Button d="flex" onClick={handleCollapse} border={false} p={0} me={2}>
            <BootstrapIcon icon="chevron-down" />
          </Button>
          <Box as="h6" className={styles["column-title"]}>
            {title}
          </Box>
        </Box>
        <Box
          alignSelf="center"
          style={{ fontSize: "small" }}
        >{`${column?.records?.length}/${page.totalCount ?? 0}`}</Box>
      </Box>
      {canCreate && onCardAdd && (
        <Box d="flex" g={2}>
          <Input
            ps={2}
            pe={2}
            border
            rounded
            value={text}
            onChange={(e) => setText(e.target.value)}
          />
          <Button
            disabled={!text.trim()}
            variant="primary"
            onClick={handleAdd}
            flexShrink={0}
          >
            {i18n.get("Add")}
          </Button>
        </Box>
      )}
      <Box
        className={clsx(styles["record-list-wrapper"], {
          [styles["overflow"]]: overflow,
          [styles["no-data"]]: noData,
        })}
        onScroll={handleScroll}
        ref={containerRef}
      >
        <RecordList
          column={column}
          readonly={readonly}
          className={styles["record-list"]}
        />
        {hasMore && !noData && (
          <Box d="flex" flexDirection="column" justifyContent="center" py={2}>
            <Box
              d="flex"
              justifyContent="center"
              py={2}
              className={styles["bottom-column-text"]}
            >
              {i18n.get(
                "Showing {0} of {1} items.",
                column.records?.length,
                page.totalCount,
              )}
            </Box>

            <Button
              onClick={() => onLoadMore?.({ column })}
              outline
              size="sm"
              variant="secondary"
              alignSelf="center"
            >
              {i18n.get("Load more")}
            </Button>
          </Box>
        )}
        {!hasMore && !noData && !loading && (
          <Box
            d="flex"
            justifyContent="center"
            py={2}
            className={styles["bottom-column-text"]}
          >
            {i18n.get("Showing all items.")}
          </Box>
        )}
        {noData && (
          <Box
            py={2}
            textAlign="center"
            className={styles["bottom-column-text"]}
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
  onCollapse,
  onLoadMore,
  onCardAdd,
  onCardMove,
  onCardClick,
  onCardDelete,
  onCardEdit,
}: KanbanBoardProps) {
  const { Card } = components;

  const scrollTopsRef = useRef<Record<string, number>>();

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
              onCollapse,
              onCardAdd,
              onLoadMore,
              scrollTop: scrollTopsRef.current?.[column.name ?? ""],
              handleScroll: (e) => {
                if (!scrollTopsRef.current) {
                  scrollTopsRef.current = {};
                }
                scrollTopsRef.current[column.name ?? ""] = (
                  e.target as HTMLElement
                ).scrollTop;
              },
            }}
          />
        ),
      })) as unknown as KanbanColumn[],
    [
      columns,
      responsive,
      onCollapse,
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
