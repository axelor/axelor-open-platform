import { useCallback, useState, useMemo, useEffect } from "react";
import {
  Box,
  Tree as TreeComponent,
  TreeNode as TreeRecord,
  TreeSortColumn,
} from "@axelor/ui";
import { useSetAtom } from "jotai";
import { uniq } from "lodash";
import { GridProvider as TreeProvider } from "@axelor/ui/grid";

import { DataStore } from "@/services/client/data-store";
import { TreeField, TreeNode, TreeView } from "@/services/client/meta.types";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { SearchOptions, SearchResult } from "@/services/client/data";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import { ViewProps } from "../types";
import { PageText } from "@/components/page-text";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useViewTab } from "@/view-containers/views/scope";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { useDataStore } from "@/hooks/use-data-store";
import { useGridActionExecutor } from "../grid/builder/utils";
import { toKebabCase, toTitleCase } from "@/utils/names";
import { Formatters } from "@/utils/format";
import { i18n } from "@/services/client/i18n";
import { TreeNode as TreeNodeComponent, TreeNodeProps } from "./tree-node";
import { getNodeOfTreeRecord } from "./utils";

export function Tree({ meta }: ViewProps<TreeView>) {
  const { view } = meta;
  const { action, dashlet, popup, popupOptions } = useViewTab();
  const [sortColumns, setSortColumns] = useState<TreeSortColumn[]>([]);

  const actionExecutor = useGridActionExecutor(
    view,
    useCallback<() => DataContext>(
      () => ({
        ...action.context,
        _viewName: action.name,
        _viewType: action.viewType,
        _views: action.views,
      }),
      [action]
    )
  );

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
    [sortColumns]
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
              record
            ),
            _draggable: draggable || isSameModelTree,
            _droppable: node === view.nodes?.[0] || isSameModelTree,
            $key: `${model}:${record.id}`,
          } as unknown as TreeRecord)
      );
    },
    [view, isSameModelTree]
  );

  const dataStore = useMemo<DataStore | null>(() => {
    const { nodes } = view;
    const { model, domain, items = [] } = nodes?.[0] || {};
    const fields = uniq(items.map((item) => item.name)) as string[];
    return model
      ? new DataStore(model, {
          fields,
          filter: {
            _domain: domain,
            _domainContext: {
              ...action.context,
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
        })
      : null;
  }, [view, isSameModelTree, action]);

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

      if ((Formatters as any)[type]) {
        attrs.formatter = (Formatters as any)[type];
      }

      return { ...column, ...attrs };
    });
  }, [view]);

  const onSearch = useCallback(
    async (options: Partial<SearchOptions> = {}) => {
      if (dataStore) {
        const rootNode = view.nodes?.[0];
        return dataStore.search({
          ...(rootNode && getSearchOptions(rootNode)),
          ...options,
        });
      }
    },
    [dataStore, view.nodes, getSearchOptions]
  );

  const onSearchNode = useCallback(
    async (treeNode: TreeRecord) => {
      const node = getNodeOfTreeRecord(view, treeNode, true);

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
          _domainContext: {
            ...action.context,
            ...(countOn && { _countOn: countOn }),
            _parentId: treeNode.id,
          },
        },
      });

      return toTreeData(
        node,
        (await ds.search(getSearchOptions(node))).records
      );
    },
    [action, view, isSameModelTree, toTreeData, getSearchOptions]
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
            { ...data, ...result }
          ),
        };
      }
      return record;
    },
    [view, isSameModelTree]
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
      [rootNode, toTreeData]
    )
  );

  const setPopupHandlers = useSetAtom(usePopupHandlerAtom());
  const setDashletHandlers = useSetAtom(useDashletHandlerAtom());

  useEffect(() => {
    if (popup && dataStore) {
      setPopupHandlers({
        dataStore: dataStore,
        onSearch: onSearch as (
          options?: SearchOptions
        ) => Promise<SearchResult>,
      });
    }
  }, [onSearch, popup, dataStore, setPopupHandlers]);

  useEffect(() => {
    if (dashlet && dataStore) {
      setDashletHandlers({
        dataStore,
        view,
        onRefresh: () => onSearch({}),
      });
    }
  }, [dashlet, view, dataStore, onSearch, setDashletHandlers]);

  const showToolbar = popupOptions?.showToolbar !== false;

  const { offset = 0, limit = 40, totalCount = 0 } = dataStore?.page || {};
  const canPrev = offset > 0;
  const canNext = offset + limit < totalCount;

  const nodeRenderer = useMemo(
    () => (props: TreeNodeProps) =>
      (
        <TreeNodeComponent
          {...props}
          view={view}
          actionExecutor={actionExecutor}
        />
      ),
    [view, actionExecutor]
  );

  return (
    <Box d="flex" flexDirection="column" overflow="auto" w={100}>
      {showToolbar && (
        <ViewToolBar
          meta={meta}
          actions={[]}
          pagination={{
            canPrev,
            canNext,
            onPrev: () => onSearch({ offset: offset - limit }),
            onNext: () => onSearch({ offset: offset + limit }),
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
                onClick: () => onSearch({}),
              },
            ],
          }}
        >
          <Box fontWeight="bold">{view.title}</Box>
        </ViewToolBar>
      )}
      <TreeProvider>
        <TreeComponent
          columns={columns}
          records={records}
          sortable
          nodeRenderer={nodeRenderer}
          onSort={handleSort}
          onLoad={onSearchNode}
          onNodeMove={handleNodeMove}
        />
      </TreeProvider>
    </Box>
  );
}
