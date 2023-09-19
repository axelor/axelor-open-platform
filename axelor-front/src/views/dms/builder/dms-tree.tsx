import { Fragment, memo, useMemo, useRef } from "react";
import { Box, useDrop } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { GridRowProps } from "@axelor/ui/grid";
import clsx from "clsx";

import { DMS_NODE_TYPE, TreeRecord } from "./types";
import { DataRecord } from "@/services/client/data.types";
import { isEqual } from "lodash";
import styles from "./dms-tree.module.scss";
import { useAtomValue } from "jotai";
import { useDMSGridHandlerAtom } from "./handler";

interface TreeProps {
  root: TreeRecord;
  data: TreeRecord[];
  expanded?: TreeRecord["id"][];
  selected?: TreeRecord["id"];
  onDrop?: (data: TreeRecord, records: DataRecord[]) => void;
  onSelect?: (data: TreeRecord) => void;
  onExpand?: (data: TreeRecord) => void;
}

const TreeNode = memo(function TreeNode({
  data,
  onDrop,
  onSelect,
  onExpand,
}: Pick<TreeProps, "onDrop" | "onSelect" | "onExpand"> & { data: TreeRecord }) {
  const { getSelectedDocuments } = useAtomValue(useDMSGridHandlerAtom());
  const [{ hovered }, dropRef] = useDrop({
    accept: DMS_NODE_TYPE,
    drop({ data: { record } }: GridRowProps) {
      const docs = getSelectedDocuments?.() ?? [];
      if (!docs.some((d) => d.id === record.id)) {
        docs.push(record);
      }
      onDrop?.(data, docs);
    },
    canDrop(props: GridRowProps) {
      const {
        data: { record },
      } = props;
      return record?.["parent.id"] !== data.id && record?.id !== data.id;
    },
    collect: function (monitor) {
      return {
        hovered: monitor.isOver({ shallow: true }) && monitor.canDrop(),
      };
    },
  });

  return (
    <Box as="li">
      <Box
        ref={dropRef}
        d="flex"
        as="span"
        className={clsx(styles.node, {
          [styles.active]: data._selected,
          [styles.hovered]: hovered,
        })}
        style={{ paddingLeft: `${(data._level ?? 0) * 0.75}rem` }}
        onClick={() => onSelect?.(data)}
      >
        <MaterialIcon
          icon={data._expand ? "arrow_drop_down" : "arrow_right"}
          className={clsx(styles.icon, {
            [styles.hide]: !data._children,
          })}
          {...(data._children && {
            onClick: (e) => {
              e.preventDefault();
              e.stopPropagation();
              onExpand?.(data);
            },
          })}
        />
        <MaterialIcon icon="folder" fill={true} />
        <Box as="span" mx={2} title={data.fileName}>
          {data.fileName}
        </Box>
      </Box>
      {data._children && data._expand && (
        <TreeNodeList
          data={data._children}
          onDrop={onDrop}
          onSelect={onSelect}
          onExpand={onExpand}
        />
      )}
    </Box>
  );
});

const TreeNodeList = memo(function TreeNodeList({
  data = [],
  onDrop,
  onSelect,
  onExpand,
}: Pick<TreeProps, "data" | "onDrop" | "onSelect" | "onExpand">) {
  return (
    <Box as="ul" flexGrow={1} className={styles.list}>
      {data.map((record, ind) => (
        <Fragment key={record.id ?? `ROOT_${ind}`}>
          <TreeNode
            data={record}
            onDrop={onDrop}
            onSelect={onSelect}
            onExpand={onExpand}
          />
        </Fragment>
      ))}
    </Box>
  );
});

export const DmsTree = memo(function DmsTree({
  data,
  root,
  selected,
  expanded,
  ...props
}: TreeProps) {
  const recordsRef = useRef<Record<number, TreeRecord>>({});

  const treeData = useMemo(() => {
    function toTreeNode(record: TreeRecord, level = 0) {
      const _children: TreeRecord[] = data
        .filter((rec) => rec["parent.id"] === record.id)
        .map((record) => toTreeNode(record, level + 1));

      const $record = {
        ...record,
        _level: level,
        _selected: selected === record.id,
        _expand: expanded?.includes(record.id),
        ...(_children.length && { _children }),
      } as TreeRecord;

      const _record = recordsRef.current[record.id!];

      if (_record && isEqual($record, _record)) {
        return _record;
      }

      return (recordsRef.current[record.id!] = $record);
    }
    return data.filter((record) => record.id === root.id).map(toTreeNode);
  }, [data, root, selected, expanded]);

  return <TreeNodeList {...props} data={treeData} />;
});
