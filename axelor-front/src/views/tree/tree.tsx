import { useSetAtom } from "jotai";
import uniq from "lodash/uniq";
import { useCallback, useEffect, useMemo, useReducer, useRef, useState, type JSX } from "react";

import {
  Box,
  Tree as TreeComponent,
  DndProvider as TreeProvider,
  TreeHandle,
  TreeNode as TreeRecord,
  TreeSortColumn,
} from "@axelor/ui";

import { PageText } from "@/components/page-text";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useShortcuts } from "@/hooks/use-shortcut";
import { SearchOptions, SearchResult } from "@/services/client/data";
import { DataStore } from "@/services/client/data-store";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { TreeField, TreeNode, TreeView } from "@/services/client/meta.types";
import { DEFAULT_PAGE_SIZE, getDefaultPageSize } from "@/utils/app-settings.ts";
import { toKebabCase, toTitleCase } from "@/utils/names";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { useSetPopupHandlers } from "@/view-containers/view-popup/handler";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import {
  useViewTabRefresh,
  useViewContext,
  useViewTab,
} from "@/view-containers/views/scope";

import { useActionExecutor, useAfterActions } from "../form/builder/scope";
import { ViewProps } from "../types";
import { Node as NodeComponent, NodeProps } from "./renderers/node";
import {
  NodeText as NodeTextComponent,
  NodeTextProps,
} from "./renderers/node-text";
import { LoadMoreTreeRow } from "./types";
import { createContextParams } from "../form/builder/utils";
import { getNodeOfTreeRecord } from "./utils";

import styles from "./tree.module.scss";

function isTreeFieldItem(item: TreeField | { type?: string }): item is TreeField {
  return item.type === "field";
}

type ChildrenCacheEntry = {
  records: TreeRecord[];
  totalCount: number;
  parentRecord: TreeRecord;
  node: TreeNode;
};

export function Tree({ meta }: ViewProps<TreeView>) {
  const { view } = meta;
  const { action, dashlet, popup, popupOptions } = useViewTab();
  const [records, setRecords] = useState<TreeRecord[]>([]);
  const [sortColumns, setSortColumns] = useState<TreeSortColumn[]>([]);
  const [resetCount, doReset] = useReducer((c) => c + 1, 0);
  const treeRef = useRef<TreeHandle | null>(null);
  const childrenCacheRef = useRef<Map<string, ChildrenCacheEntry>>(new Map());
  const loadingMoreRef = useRef<Set<string>>(new Set());
  const getViewContext = useViewContext();

  const getContext = useCallback(
    (actions?: boolean) => {
      const ctx = getViewContext(actions);
      const params = createContextParams(view, action);
      return {
        ...params,
        ...ctx,
        _model: ctx?._model || view.nodes?.[0]?.model,
      } as DataContext;
    },
    [getViewContext, action, view],
  );

  // check parent/child is of same model
  const isSameModelTree = useMemo(() => {
    const model = view?.nodes?.[0]?.model;
    return view?.nodes?.every?.((n) => n.model === model);
  }, [view]);

  const getSearchOptions = useCallback(
    (node: TreeNode): Partial<SearchOptions> => {
      const { items = [] } = node;
      const treeFields = items.filter(isTreeFieldItem);
      if (sortColumns.length === 0) return {};
      return {
        sortBy: sortColumns
          .map((col) => {
            const $item = treeFields.find(
              (item) => item.as === col.name,
            );
            return $item
              ? `${col.order === "desc" ? "-" : ""}${$item.name}`
              : null;
          })
          .filter((c) => c) as string[],
      };
    },
    [sortColumns],
  );

  const toTreeData = useCallback(
    (node: TreeNode, treeRecords: DataRecord[]) => {
      const { model, draggable, items = [] } = node;
      const treeFields = items.filter(isTreeFieldItem);
      return treeRecords.map(
        (treeRecord) =>
          ({
            ...treeFields.reduce(
              (nextRecord, item) =>
                item.as
                  ? { ...nextRecord, [item.as]: nextRecord[item.name] }
                  : nextRecord,
              treeRecord,
            ),
            _draggable: draggable && isSameModelTree,
            _droppable: node === view.nodes?.[0] || isSameModelTree,
            $key: `${model}:${treeRecord.id}`,
          }) as unknown as TreeRecord,
      );
    },
    [view, isSameModelTree],
  );

  const dataStore = useMemo<DataStore | null>(() => {
    const { nodes } = view;
    const { model, items = [] } = nodes?.[0] || {};
    const fields = uniq(items.map((item) => item.name)) as string[];
    return model
      ? new DataStore(model, {
          fields,
        })
      : null;
  }, [view]);

  const columns = useMemo(() => {
    return (view.columns || []).map((column) => {
      const attrs: { title: string; width?: number } = {
        title: column.title || column.autoTitle || toTitleCase(column.name),
      };
      const type = toKebabCase(column.type ?? "");

      if (type === "button") {
        attrs.title = "";
        attrs.width = 50;
      }

      return { ...column, ...attrs };
    });
  }, [view]);

  const doSearch = useCallback(
    async (options: Partial<SearchOptions> = {}) => {
      if (dataStore) {
        const { nodes } = view;
        const rootNode = nodes?.[0];
        const { _domainAction, ..._domainContext } = getContext() || {};

        let _domain = rootNode?.domain;

        if (action.domain) {
          _domain = `${action.domain.trim()}${
            _domain ? ` AND (${_domain})` : ""
          }`;
        }

        return dataStore.search({
          ...(rootNode && getSearchOptions(rootNode)),
          ...options,
          filter: {
            ...options.filter,
            ...(_domain && { _domain }),
            _domainAction,
            _domainContext: {
              ...options?.filter?._domainContext,
              ..._domainContext,
              ...(isSameModelTree
                ? { _countOn: nodes?.[1]?.parent }
                : {
                    _childOn: {
                      model: nodes?.[1]?.model,
                      parent: nodes?.[1]?.parent,
                    },
                  }),
            },
          },
        });
      }
    },
    [
      dataStore,
      view,
      action.domain,
      isSameModelTree,
      getSearchOptions,
      getContext,
    ],
  );

  const onSearch = useAfterActions(doSearch);

  const onRefresh = useCallback(async () => {
    childrenCacheRef.current.clear();
    loadingMoreRef.current.clear();
    doReset();
    await onSearch({});
  }, [doReset, onSearch]);

  const getActionContext = useCallback(() => getContext(true), [getContext]);

  const actionExecutor = useActionExecutor(view, {
    getContext: getActionContext,
    onRefresh,
  });

  const searchChildNode = useCallback(
    async (
      treeNode: TreeRecord,
      node: TreeNode,
      options: Partial<SearchOptions> = {},
    ) => {
      const { _domainAction, ..._domainContext } = getContext() ?? {};
      const { model, domain, parent, items = [] } = node;
      const countOn = isSameModelTree ? parent : "";
      const fields = uniq(items.map((item) => item.name)) as string[];
      const ds = new DataStore(model!, {
        fields,
        filter: {
          ...(parent && {
            _domain: `self.${parent}.id = :_parentId ${
              domain ? `AND (${domain})` : ""
            }`,
          }),
          _domainAction,
          _domainContext: {
            ..._domainContext,
            ...(countOn && { _countOn: countOn }),
            _parentId: treeNode.id,
          },
        },
      });

      return ds.search({
        ...getSearchOptions(node),
        limit: getDefaultPageSize(),
        ...options,
      });
    },
    [isSameModelTree, getContext, getSearchOptions],
  );

  const makeLoadMoreNode = useCallback(
    (parentKey: string, loaded: number, total: number): TreeRecord => {
      const loadMoreData: LoadMoreTreeRow = {
        $key: `load-more:${parentKey}`,
        _children: false,
        _loadMore: true,
        _parentKey: parentKey,
        _loadMoreLoaded: loaded,
        _loadMoreTotal: total,
      };
      return {
        ...loadMoreData,
        data: loadMoreData,
        children: false,
      };
    },
    [],
  );

  const onSearchNode = useCallback(
    async (treeNode: TreeRecord) => {
      const node = getNodeOfTreeRecord(view, treeNode, true);
      if (!node) return [];

      const parentKey = treeNode.$key as string;
      const cached = childrenCacheRef.current.get(parentKey);

      if (cached) {
        const result = [...cached.records];
        if (cached.totalCount > cached.records.length) {
          result.push(
            makeLoadMoreNode(
              parentKey,
              cached.records.length,
              cached.totalCount,
            ),
          );
        }
        return result;
      }

      const searchResult = await searchChildNode(treeNode, node);
      const treeData = toTreeData(node, searchResult.records);
      const total = searchResult.page.totalCount ?? 0;

      childrenCacheRef.current.set(parentKey, {
        records: treeData,
        totalCount: total,
        parentRecord: treeNode,
        node,
      });

      const result = [...treeData];
      if (total > treeData.length) {
        result.push(makeLoadMoreNode(parentKey, treeData.length, total));
      }
      return result;
    },
    [view, toTreeData, searchChildNode, makeLoadMoreNode],
  );

  const onLoadMoreChildren = useCallback(
    async (parentKey: string) => {
      if (loadingMoreRef.current.has(parentKey)) return;

      const cached = childrenCacheRef.current.get(parentKey);
      if (!cached) return;
      if (cached.records.length >= cached.totalCount) return;

      loadingMoreRef.current.add(parentKey);
      try {
        const searchResult = await searchChildNode(
          cached.parentRecord,
          cached.node,
          {
            offset: cached.records.length,
          },
        );
        const newTreeData = toTreeData(cached.node, searchResult.records);
        const totalCount = searchResult.page.totalCount ?? cached.totalCount;

        childrenCacheRef.current.set(parentKey, {
          ...cached,
          records: [...cached.records, ...newTreeData],
          totalCount: Math.max(totalCount, cached.records.length + newTreeData.length),
        });

        await treeRef.current?.reloadChildren(parentKey);
      } finally {
        loadingMoreRef.current.delete(parentKey);
      }
    },
    [toTreeData, searchChildNode],
  );

  const nodeRenderer = useMemo(
    () => (props: NodeProps) => (
      <NodeComponent
        {...props}
        view={view}
        actionExecutor={actionExecutor}
        onLoadMore={onLoadMoreChildren}
      />
    ),
    [view, actionExecutor, onLoadMoreChildren],
  );

  const nodeTextRenderer = useMemo(
    () => (props: NodeTextProps) => (
      <NodeTextComponent
        {...props}
        view={view}
        actionExecutor={actionExecutor}
      />
    ),
    [view, actionExecutor],
  );

  const handleNodeMove = useCallback(
    async (record: TreeRecord, parentRecord: TreeRecord) => {
      const data = record.data;
      const node = getNodeOfTreeRecord(view, record);

      if (!node) return record;

      const [lastNode] = view.nodes?.slice(-1) ?? [];
      const {
        parent = isSameModelTree ? lastNode?.parent : "",
        model,
        items = [],
      } = node;

      let saveData = {
        id: data.id,
        version: data.version,
        ...(parent && {
          [parent]: parentRecord.data
            ? {
                id: parentRecord.data?.id,
                version: parentRecord.data?.version,
              }
            : null,
        }),
      };

      if (node?.onMove) {
        const res = await actionExecutor.execute(node?.onMove, {
          context: { ...saveData, _model: node.model },
        });
        // save any value changes through action
        saveData = {
          ...saveData,
          ...res?.reduce?.(
            (obj, { values }) => ({
              ...obj,
              ...values,
            }),
            {},
          ),
        };
      }

      const ds = new DataStore(model!);
      const result = await ds.save(saveData);

      if (result) {
        return {
          ...record,
          data: (items as TreeField[]).reduce(
            (itemData, item) =>
              item.as
                ? { ...itemData, [item.as]: itemData[item.name] }
                : itemData,
            { ...data, ...result },
          ),
        };
      }
      return record;
    },
    [view, isSameModelTree, actionExecutor],
  );

  const handleSort = useCallback((cols?: TreeSortColumn[]) => {
    if (!cols) return;
    childrenCacheRef.current.clear();
    loadingMoreRef.current.clear();
    setSortColumns(cols);
  }, []);

  useAsyncEffect(async () => {
    await onSearch();
  }, [onSearch]);

  const rootNode = view.nodes?.[0];
  useEffect(() => {
    if (dataStore && rootNode) {
      const setTreeData = () =>
        setRecords(() => toTreeData(rootNode, dataStore.records));

      setTreeData();
      return dataStore.subscribe(() => {
        setTreeData();
      });
    }
  }, [rootNode, dataStore, toTreeData, resetCount]);

  const setPopupHandlers = useSetPopupHandlers();
  const setDashletHandlers = useSetAtom(useDashletHandlerAtom());

  useEffect(() => {
    if (popup && dataStore) {
      setPopupHandlers({
        dataStore: dataStore,
        onSearch: onSearch as (
          options?: SearchOptions,
        ) => Promise<SearchResult>,
      });
    }
  }, [onSearch, popup, dataStore, setPopupHandlers]);

  useEffect(() => {
    if (dashlet && dataStore) {
      setDashletHandlers({
        dataStore,
        view,
        onRefresh,
      });
    }
  }, [dashlet, view, dataStore, onRefresh, setDashletHandlers]);

  const showToolbar = popupOptions?.showToolbar !== false;

  const {
    offset = 0,
    limit = DEFAULT_PAGE_SIZE,
    totalCount = 0,
  } = dataStore?.page || {};
  const canPrev = offset > 0;
  const canNext = offset + limit < totalCount;

  const handlePrev = useCallback(
    () => onSearch({ offset: offset - limit }),
    [limit, offset, onSearch],
  );

  const handleNext = useCallback(
    () => onSearch({ offset: offset + limit }),
    [limit, offset, onSearch],
  );

  useShortcuts({
    viewType: view.type,
    onRefresh,
  });

  // register tab:refresh
  useViewTabRefresh("tree", onRefresh);

  return (
    <Box
      className={styles.container}
      d="flex"
      flexDirection="column"
      overflow="auto"
      w={100}
    >
      {showToolbar && (
        <ViewToolBar
          meta={meta}
          actions={[]}
          pagination={{
            canPrev,
            canNext,
            onPrev: handlePrev,
            onNext: handleNext,
            text: () =>
              (dataStore && <PageText dataStore={dataStore} />) as JSX.Element,
            actions: [
              {
                key: "refresh",
                text: i18n.get("Refresh"),
                iconOnly: true,
                iconProps: {
                  icon: "refresh",
                },
                onClick: onRefresh,
              },
            ],
          }}
        >
          <Box fontWeight="bold">{view.title}</Box>
        </ViewToolBar>
      )}
      <TreeProvider>
        <TreeComponent
          ref={treeRef}
          className={styles.tree}
          columns={columns}
          records={records}
          sortable
          {...(isSameModelTree && {
            droppable: isSameModelTree,
            droppableText: <DroppableRoot />,
          })}
          nodeRenderer={nodeRenderer}
          textRenderer={nodeTextRenderer}
          onSort={handleSort}
          onLoad={onSearchNode}
          onNodeMove={handleNodeMove}
        />
      </TreeProvider>
    </Box>
  );
}

function DroppableRoot() {
  return (
    <Box
      className={styles.placeholder}
      d="flex"
      flex={1}
      alignItems={"center"}
      justifyContent={"center"}
    >
      {i18n.get("Drop here")}
    </Box>
  );
}
