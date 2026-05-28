import { useSetAtom } from "jotai";
import uniq from "lodash/uniq";
import {
  useCallback,
  useEffect,
  useMemo,
  useReducer,
  useRef,
  useState,
  type JSX,
} from "react";

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
import { DEFAULT_PAGE_SIZE, getDefaultPageSize } from "@/utils/app-settings.ts";
import {
  Property,
  TreeField,
  TreeNode,
  TreeView,
} from "@/services/client/meta.types";
import { toKebabCase, toTitleCase } from "@/utils/names";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { useSetPopupHandlers } from "@/view-containers/view-popup/handler";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import {
  useViewTabRefresh,
  useViewContext,
  useViewTab,
} from "@/view-containers/views/scope";

import {
  expandJsonFieldValues,
  updateJsonFieldValue,
} from "@/services/client/data-utils";
import { useActionExecutor, useAfterActions } from "../form/builder/scope";
import { ViewProps } from "../types";
import { Node as NodeComponent, NodeProps } from "./renderers/node";
import {
  NodeText as NodeTextComponent,
  NodeTextProps,
} from "./renderers/node-text";
import { LoadMoreTreeRow } from "./types";
import { createContextParams } from "../form/builder/utils";
import { findJsonFieldItem } from "@/utils/schema";
import { getNodeOfTreeRecord } from "./utils";

import styles from "./tree.module.scss";

function isTreeFieldItem(
  item: TreeField | { type?: string },
): item is TreeField {
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

  const getJsonField = useCallback(
    (fieldName?: string) =>
      fieldName ? findJsonFieldItem(meta, fieldName) : undefined,
    [meta],
  );

  const getJsonParentField = useCallback(
    (fieldName?: string, node?: TreeNode) => {
      const field = getJsonField(fieldName);
      if (!fieldName) {
        return {
          field,
          countOn: fieldName,
        };
      }
      if (field?.jsonField && field?.jsonPath) {
        return {
          field,
          countOn: `${field.jsonField}.${field.jsonPath}`,
          jsonField: field.jsonField,
          jsonPath: field.jsonPath,
        };
      }
      // For JSON model nodes (MetaJsonRecord), fields not found in meta.jsonFields
      // (e.g. child node with a different jsonModel) are stored in attrs
      if (node?.jsonModel) {
        return {
          field,
          countOn: `attrs.${fieldName}`,
          jsonField: "attrs",
          jsonPath: fieldName,
        };
      }
      return {
        field,
        countOn: fieldName,
      };
    },
    [getJsonField],
  );

  const expandNodeJsonFields = useCallback(
    (record: DataRecord, node?: TreeNode) => {
      const next = { ...record };
      const fieldNames = [
        node?.parent,
        ...(node?.items ?? []).map((item) => item.name),
      ];
      const fields = fieldNames.reduce<Record<string, Property>>(
        (acc, name) => {
          if (!name) return acc;
          const field = getJsonField(name);
          if (field) {
            acc[name] = field as unknown as Property;
          } else if (node?.jsonModel) {
            // For JSON model nodes whose fields aren't in meta.jsonFields
            // (e.g. child nodes with a different jsonModel), infer jsonField
            // and jsonPath from the dotted name so the value can be expanded
            const dotIndex = name.indexOf(".");
            if (dotIndex > 0) {
              acc[name] = {
                jsonField: name.slice(0, dotIndex),
                jsonPath: name.slice(dotIndex + 1),
              } as Property;
            }
          }
          return acc;
        },
        {},
      );

      expandJsonFieldValues(next, fieldNames, fields);
      return next;
    },
    [getJsonField],
  );

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

  // check parent/child is of same model (including jsonModel)
  const isSameModelTree = useMemo(() => {
    const model = view?.nodes?.[0]?.model;
    const jsonModel = view?.nodes?.[0]?.jsonModel;
    return view?.nodes?.every?.(
      (n) => n.model === model && n.jsonModel === jsonModel,
    );
  }, [view]);

  const getSearchOptions = useCallback(
    (node: TreeNode): Partial<SearchOptions> => {
      const { items = [], orderBy } = node;
      const treeFields = items.filter(isTreeFieldItem);
      if (sortColumns.length === 0) {
        return orderBy ? { sortBy: orderBy.split(/\s*,\s*/) } : {};
      }
      return {
        sortBy: sortColumns
          .map((col) => {
            const $item = treeFields.find((item) => item.as === col.name);
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
      return treeRecords.map((record) => {
        const expanded = expandNodeJsonFields(record, node);
        return {
          ...(items as TreeField[])
            .filter(isTreeFieldItem)
            .reduce(
              (treeNode, item) =>
                item.as
                  ? { ...treeNode, [item.as]: treeNode[item.name] }
                  : treeNode,
              expanded,
            ),
          _draggable: draggable && isSameModelTree,
          _droppable: node === view.nodes?.[0] || isSameModelTree,
          $key: `${model}:${record.id}`,
        } as unknown as TreeRecord;
      });
    },
    [view, isSameModelTree, expandNodeJsonFields],
  );

  const getNodeFieldNames = useCallback(
    (node?: TreeNode) => {
      if (!node) return [] as string[];

      const names = new Set<string>();
      [node.parent, ...(node.items ?? []).map((item) => item.name)].forEach(
        (name) => {
          const jsonField = getJsonParentField(name, node).jsonField;
          if (jsonField) {
            names.add(jsonField);
          }
        },
      );
      return [...names];
    },
    [getJsonParentField],
  );

  const dataStore = useMemo<DataStore | null>(() => {
    const { nodes } = view;
    const { model, items = [] } = nodes?.[0] || {};
    const limit = +((action.params?.limit as string) || 0);
    const fields = uniq([
      ...items.map((item) => item.name),
      ...getNodeFieldNames(nodes?.[0]),
    ]) as string[];
    return model
      ? new DataStore(model, {
          fields,
          ...(limit && { limit }),
        })
      : null;
  }, [view, action.params, getNodeFieldNames]);

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

        if (rootNode?.jsonModel) {
          const jsonModelFilter = "self.jsonModel = :jsonModel";
          _domain = _domain
            ? `${jsonModelFilter} AND (${_domain})`
            : jsonModelFilter;
        }

        if (action.domain) {
          _domain = `${action.domain.trim()}${
            _domain ? ` AND (${_domain})` : ""
          }`;
        }

        const childParent = getJsonParentField(nodes?.[1]?.parent, nodes?.[1]);

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
              ...(rootNode?.jsonModel && { jsonModel: rootNode.jsonModel }),
              ...(isSameModelTree
                ? { _countOn: childParent.countOn }
                : {
                    _childOn: {
                      model: nodes?.[1]?.model,
                      parent: childParent.countOn,
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
      getJsonParentField,
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
      const { model, domain, parent, jsonModel, items = [] } = node;
      const parentField = getJsonParentField(parent, node);
      const countOn = isSameModelTree ? parentField.countOn : "";
      const fields = uniq([
        ...items.map((item) => item.name),
        ...getNodeFieldNames(node),
      ]) as string[];

      const domainParts: string[] = [];
      if (jsonModel) {
        domainParts.push("self.jsonModel = :jsonModel");
      }
      if (parent) {
        if (parentField.jsonField && parentField.jsonPath) {
          domainParts.push(
            `json_extract_text(self.${parentField.jsonField}, '${parentField.jsonPath}', 'id') = :_parentId`,
          );
        } else {
          domainParts.push(`self.${parent}.id = :_parentId`);
        }
      }
      if (domain) {
        domainParts.push(`(${domain})`);
      }
      const _domain = domainParts.join(" AND ");

      const nodeIndex = view.nodes?.indexOf(node) ?? -1;
      const nextNode = nodeIndex >= 0 ? view.nodes?.[nodeIndex + 1] : undefined;
      const nextParent = getJsonParentField(nextNode?.parent, nextNode);

      const ds = new DataStore(model!, {
        fields,
        filter: {
          ...(_domain && { _domain }),
          _domainAction,
          _domainContext: {
            ..._domainContext,
            ...(jsonModel && { jsonModel }),
            ...(countOn && { _countOn: countOn }),
            ...(nextNode &&
              (isSameModelTree
                ? { _countOn: nextParent.countOn }
                : {
                    _childOn: {
                      model: nextNode.model,
                      parent: nextParent.countOn,
                    },
                  })),
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
    [
      view,
      isSameModelTree,
      getContext,
      getSearchOptions,
      getNodeFieldNames,
      getJsonParentField,
    ],
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
          totalCount: Math.max(
            totalCount,
            cached.records.length + newTreeData.length,
          ),
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

      const parentValue = parentRecord.data
        ? {
            id: parentRecord.data?.id,
            version: parentRecord.data?.version,
          }
        : null;

      let saveData: DataRecord = {
        id: data.id,
        version: data.version,
      };

      if (parent) {
        const parentField = getJsonParentField(parent, node);
        if (parentField.jsonField && parentField.jsonPath) {
          updateJsonFieldValue(saveData, parentField.jsonField, data, {
            [parentField.jsonPath]: parentValue,
          });
        } else {
          saveData[parent] = parentValue;
        }
      }

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
        const merged = expandNodeJsonFields({ ...data, ...result }, node);
        return {
          ...record,
          data: (items as TreeField[]).reduce(
            (itemData, item) =>
              item.as
                ? { ...itemData, [item.as]: itemData[item.name] }
                : itemData,
            merged,
          ),
        };
      }
      return record;
    },
    [
      view,
      isSameModelTree,
      actionExecutor,
      getJsonParentField,
      expandNodeJsonFields,
    ],
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
