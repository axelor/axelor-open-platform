import { Box, Button, TreeNode as TreeRecord } from "@axelor/ui";
import { ReactElement, cloneElement, useCallback, useState } from "react";

import { i18n } from "@/services/client/i18n";
import { TreeView } from "@/services/client/meta.types";
import { ActionExecutor } from "@/view-containers/action";
import { LoadMoreRowData } from "../../types";
import { getNodeOfTreeRecord } from "../../utils";

import styles from "../../tree.module.scss";

export interface NodeProps {
  data: TreeRecord;
  view: TreeView;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  children: ReactElement<any>;
  actionExecutor: ActionExecutor;
  onLoadMore?: (parentKey: string) => Promise<void> | void;
}

export function Node({
  data,
  view,
  actionExecutor,
  onLoadMore,
  children,
}: NodeProps) {
  const loadMoreData = data.data as Partial<LoadMoreRowData> | undefined;
  const isLoadMore = loadMoreData?._loadMore === true;
  const node = getNodeOfTreeRecord(view, data);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  const handleLoadMore = useCallback(
    async (e: React.MouseEvent) => {
      e.stopPropagation();
      const parentKey = loadMoreData?._parentKey;
      if (isLoadingMore || !parentKey || !onLoadMore) return;
      setIsLoadingMore(true);
      try {
        await onLoadMore(parentKey);
      } finally {
        setIsLoadingMore(false);
      }
    },
    [loadMoreData, isLoadingMore, onLoadMore],
  );

  const onDoubleClick = useCallback<React.MouseEventHandler>(
    () => {
      if (node?.onClick) {
        actionExecutor.execute(node.onClick, {
          context: {
            ...data.data,
            _model: node?.model,
          },
        });
      }
    },
    [actionExecutor, data.data, node],
  );

  if (isLoadMore) {
    const loaded = loadMoreData?._loadMoreLoaded ?? 0;
    const total = loadMoreData?._loadMoreTotal ?? 0;
    return (
      <Box
        className={styles["load-more-row"]}
        d="flex"
        alignItems="center"
        justifyContent="space-between"
        gap={2}
      >
        <Button
          className={styles["load-more-button"]}
          outline
          size="sm"
          variant="secondary"
          style={{
            marginInlineStart: `calc(${data.level ?? 0}rem + 1.5rem)`,
          }}
          disabled={isLoadingMore}
          onClick={handleLoadMore}
        >
          {isLoadingMore ? i18n.get("Loading...") : i18n.get("Load more")}
        </Button>
        <Box className={styles["load-more-text"]}>
          {i18n.get("Showing {0} of {1} items.", loaded, total)}
        </Box>
      </Box>
    );
  }

  return node?.onClick ? cloneElement(children, { onDoubleClick }) : children;
}
