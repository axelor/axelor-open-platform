import { useSetAtom } from "jotai";
import { uniq } from "lodash";
import { useCallback, useEffect, useMemo, useState } from "react";

import {
  Box,
  Tree as TreeComponent,
  DndProvider as TreeProvider,
  TreeNode as TreeRecord,
  TreeSortColumn,
} from "@axelor/ui";

import { PageText } from "@/components/page-text";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useDataStore } from "@/hooks/use-data-store";
import { useShortcuts } from "@/hooks/use-shortcut";
import { SearchOptions, SearchResult } from "@/services/client/data";
import { DataStore } from "@/services/client/data-store";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { TreeField, TreeNode, TreeView } from "@/services/client/meta.types";
import { toKebabCase, toTitleCase } from "@/utils/names";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import { useViewContext, useViewTab } from "@/view-containers/views/scope";

import { useActionExecutor, useAfterActions } from "../form/builder/scope";
import { ViewProps } from "../types";
import { Node as NodeComponent, NodeProps } from "./renderers/node";
import {
  NodeText as NodeTextComponent,
  NodeTextProps,
} from "./renderers/node-text";
import { getNodeOfTreeRecord } from "./utils";

import styles from "./tree.module.scss";

export function Tree({ meta }: ViewProps<TreeView>) {
  const { view } = meta;
  const { dashlet, popup, popupOptions } = useViewTab();
  const [sortColumns, setSortColumns] = useState<TreeSortColumn[]>([]);
  const getViewContext = useViewContext();

  const getContext = useCallback(
    (actions?: boolean) => {
      const ctx = getViewContext(actions);
      return {
        ...ctx,
        _model: ctx?._model || view.nodes?.[0]?.model,
      } as DataContext;
    },
    [getViewContext, view],
  );

  const getActionContext = useCallback(() => getContext(true), [getContext]);

  const actionExecutor = useActionExecutor(view, {
    getContext: getActionContext,
    onRefresh: () => doSearch({}),
  });

  // check parent/child is of same model
  const isSameModelTree = useMemo(() => {
    const model = view?.nodes?.[0]?.model;
    return view?.nodes?.every?.((n) => n.model === model);
  }, [view]);

  const getSearchOptions = useCallback(
    (node: TreeNode): Partial<SearchOptions> => {
      const { items = [] } = node;
      if (sortColumns.length === 0) return {};
      return {
        sortBy: sortColumns
          .map((col) => {
            const $item = items.find((item: any) => item.as === col.name);
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
    (node: TreeNode, records: DataRecord[]) => {
      const { model, draggable, items = [] } = node;
      return records.map(
        (record) =>
          ({
            ...(items as TreeField[]).reduce(
              (node, item) =>
                item.as ? { ...node, [item.as]: node[item.name] } : node,
              record,
            ),
            _draggable: draggable || isSameModelTree,
            _droppable: node === view.nodes?.[0] || isSameModelTree,
            $key: `${model}:${record.id}`,
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
      const attrs: any = {
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
        return dataStore.search({
          ...(rootNode && getSearchOptions(rootNode)),
          ...options,
          filter: {
            ...options.filter,
            _domain: rootNode?.domain,
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
    [dataStore, view, isSameModelTree, getSearchOptions, getContext],
  );

  const onSearch = useAfterActions(doSearch);

  const onSearchNode = useCallback(
    async (treeNode: TreeRecord) => {
      const node = getNodeOfTreeRecord(view, treeNode, true);
      const { _domainAction, ..._domainContext } = getContext() ?? {};

      if (!node) return [];

      const { model, domain, parent, items = [] } = node;
      const countOn = isSameModelTree ? parent : "";
      const fields = uniq(items.map((item) => item.name)) as string[];
      const ds = new DataStore(model!, {
        fields,
        filter: {
          ...(parent && {
            _domain: `self.${parent}.id = :_parentId ${
              domain ? `AND ${domain}` : ""
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

      return toTreeData(
        node,
        (await ds.search(getSearchOptions(node))).records,
      );
    },
    [view, isSameModelTree, toTreeData, getContext, getSearchOptions],
  );

  const handleNodeMove = useCallback(
    async (record: TreeRecord, parentRecord: TreeRecord) => {
      const data = record.data;
      const node = getNodeOfTreeRecord(view, record);

      if (!node) return record;

      const {
        parent = isSameModelTree ? view.nodes?.pop?.()?.parent : "",
        model,
        items = [],
      } = node;

      const ds = new DataStore(model!);
      const result = await ds.save({
        id: data.id,
        version: data.version,
        ...(parent && {
          [parent]: {
            id: parentRecord.data?.id,
            version: parentRecord.data?.version,
          },
        }),
      });

      if (result) {
        return {
          ...record,
          data: (items as TreeField[]).reduce(
            (data, item) =>
              item.as ? { ...data, [item.as]: data[item.name] } : data,
            { ...data, ...result },
          ),
        };
      }
      return record;
    },
    [view, isSameModelTree],
  );

  const handleSort = useCallback((cols?: TreeSortColumn[]) => {
    cols && setSortColumns(cols);
  }, []);

  useAsyncEffect(async () => {
    await onSearch();
  }, [onSearch]);

  const rootNode = view.nodes?.[0];
  const records = useDataStore(
    dataStore!,
    useCallback(
      (ds) => toTreeData(rootNode!, ds.records),
      [rootNode, toTreeData],
    ),
  );

  const setPopupHandlers = useSetAtom(usePopupHandlerAtom());
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
        onRefresh: () => doSearch({}),
      });
    }
  }, [dashlet, view, dataStore, doSearch, setDashletHandlers]);

  const showToolbar = popupOptions?.showToolbar !== false;

  const { offset = 0, limit = 40, totalCount = 0 } = dataStore?.page || {};
  const canPrev = offset > 0;
  const canNext = offset + limit < totalCount;

  const nodeRenderer = useMemo(
    () => (props: NodeProps) => (
      <NodeComponent {...props} view={view} actionExecutor={actionExecutor} />
    ),
    [view, actionExecutor],
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

  const handleRefresh = useCallback(async () => {
    await onSearch({});
  }, [onSearch]);

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
    onRefresh: handleRefresh,
  });

  return (
    <Box d="flex" flexDirection="column" overflow="auto" w={100}>
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
                onClick: handleRefresh,
              },
            ],
          }}
        >
          <Box fontWeight="bold">{view.title}</Box>
        </ViewToolBar>
      )}
      <TreeProvider>
        <TreeComponent
          className={styles.tree}
          columns={columns}
          records={records}
          sortable
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
