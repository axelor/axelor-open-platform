import {
  SyntheticEvent,
  useCallback,
  useReducer,
  useState,
  useRef,
  useMemo,
  Fragment,
} from "react";
import { Box, Input, Link } from "@axelor/ui";
import { GridRow, GridColumn } from "@axelor/ui/src/grid";
import { useAtom } from "jotai";
import { uniq } from "lodash";
import { DndProvider } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import clsx from "clsx";

import { GridView } from "@/services/client/meta.types";
import { PageText } from "@/components/page-text";
import { useViewTab } from "@/view-containers/views/scope";
import { usePerms } from "@/hooks/use-perms";
import { SearchOptions } from "@/services/client/data";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import { AdvanceSearch } from "@/view-containers/advance-search";
import { i18n } from "@/services/client/i18n";
import { ViewProps } from "../types";
import { TreeRecord } from "./types";
import { dialogs } from "@/components/dialogs";
import { useSession } from "@/hooks/use-session";
import { useDataStore } from "@/hooks/use-data-store";
import { useGridState } from "../grid/builder/utils";
import { useEditor } from "@/hooks/use-relation";
import { useRoute } from "@/hooks/use-route";
import { Grid as GridComponent } from "../grid/builder";
import { DmsTree } from "./dms-tree";
import { DmsOverlay } from "./dms-overlay";
import { downloadAsBatch, toStrongText } from "./utils";
import styles from "./dms.module.scss";

const ROOT: TreeRecord = { id: null, fileName: i18n.get("Home") };

const promptInput = async (title: string, inputValue: string = "") => {
  const confirmed = await dialogs.confirm({
    title,
    content: (
      <Box d="flex" w={100}>
        <Input
          type="text"
          autoFocus={true}
          defaultValue={inputValue}
          onChange={(e) => {
            inputValue = e.target.value;
          }}
        />
      </Box>
    ),
    yesTitle: i18n.get("Create"),
  });
  return confirmed && inputValue;
};

export function Dms(props: ViewProps<GridView>) {
  const { meta, dataStore, searchAtom, domains } = props;
  const { view, fields } = meta;
  const { action, popupOptions } = useViewTab();
  const { data: session } = useSession();
  const { hasButton } = usePerms(meta.view, meta.perms);
  const showEditor = useEditor();

  const [advanceSearch, setAdvancedSearch] = useAtom(searchAtom!);
  const [state, setState] = useGridState();
  const { navigate } = useRoute();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // tree state
  const [treeRecords, setTreeRecords] = useState<TreeRecord[]>([ROOT]);
  const [expanded, setExpanded] = useState<TreeRecord["id"][]>([ROOT.id]);
  const [selected, setSelected] = useState<TreeRecord["id"]>(ROOT.id);
  const [showTree, toggleTree] = useReducer((show) => !show, true);

  const { orderBy, rows, selectedRows } = state;
  const uploadSize = session?.api?.upload?.maxSize ?? 0;

  const getSelectedNode = useCallback(
    () => treeRecords.find((r) => r.id === selected),
    [treeRecords, selected]
  );

  const getSelectedDocument = useCallback(() => {
    const selected = selectedRows?.[0];
    return rows[selected ?? -1]?.record;
  }, [rows, selectedRows]);

  const getSelectedDocuments = useCallback(() => {
    const records = selectedRows?.map?.((ind) => rows[ind]?.record);
    return records?.length ? records : null;
  }, [rows, selectedRows]);

  const getSelected = useCallback(() => {
    return getSelectedDocument() || getSelectedNode();
  }, [getSelectedDocument, getSelectedNode]);

  const onSearch = useCallback(
    (options: Partial<SearchOptions> = {}) => {
      const { query = {} } = advanceSearch;
      const { archived: _archived } = query;

      const sortBy = orderBy?.map(
        (column) => `${column.order === "desc" ? "-" : ""}${column.name}`
      );

      return dataStore
        .search({
          sortBy,
          filter: {
            ...query,
            _archived,
            _domain: `${action.domain ? `${action.domain} AND ` : ""}${
              selected ? `self.parent.id = ${selected}` : "self.parent is null"
            }`,
          },
          ...options,
          ...(options.fields && {
            fields: uniq([
              ...options.fields,
              "isDirectory",
              "parent.id",
              "relatedModel",
              "relatedId",
              "metaFile.id",
            ]),
          }),
        })
        .then((result) => {
          const { records } = result;
          const dirs = records.filter((r) => r.isDirectory);
          const dirIds = dirs.map((r) => r.id);

          setTreeRecords((records) => [
            ...records.filter((r) => !dirIds.includes(r.id)),
            ...dirs,
          ]);

          return result;
        });
    },
    [action.domain, dataStore, selected, orderBy, advanceSearch]
  );

  const onNew = useCallback(
    async (title: string, inputValue?: string, data?: TreeRecord) => {
      const node = getSelectedNode();

      const exists = dataStore.records.filter(
        (rec) =>
          rec.fileName === inputValue &&
          (!data?.isDirectory || rec.isDirectory === true)
      );

      if (exists.length > 0) {
        inputValue = `${inputValue} (${exists.length + 1})`;
      }
      let input = await promptInput(title, inputValue);

      if (input) {
        const record = await dataStore.save({
          fileName: input,
          ...data,
          ...(node?.id && {
            parent: {
              id: node.id,
            },
          }),
        });
        record && onSearch({});
      }
    },
    [dataStore, getSelectedNode, onSearch]
  );

  const onFolderNew = useCallback(async () => {
    return onNew(i18n.get("Create folder"), i18n.get("New Folder"), {
      isDirectory: true,
    });
  }, [onNew]);

  const onDocumentNew = useCallback(async () => {
    return onNew(i18n.get("Create document"), i18n.get("New Document"), {
      isDirectory: false,
      contentType: "html",
    });
  }, [onNew]);

  const onSpreadsheetNew = useCallback(async () => {
    return onNew(i18n.get("Create spreadsheet"), i18n.get("New Spreadsheet"), {
      isDirectory: false,
      contentType: "spreadsheet",
    });
  }, [onNew]);

  const onDocumentRename = useCallback(async () => {
    const doc = getSelectedDocument();
    const node = getSelectedNode();
    const record = doc || node;

    if (!record) return;

    let input = await promptInput(i18n.get("Information"), record.fileName);

    if (input) {
      const updated = await dataStore.save({
        id: record.id,
        version: record.version,
        fileName: input,
      });
      if (updated) {
        if (doc) {
          onSearch({});
        } else {
          setTreeRecords((records) =>
            records.map((r) =>
              r.id === updated.id ? { ...r, fileName: updated.fileName } : r
            )
          );
        }
      }
    }
  }, [getSelectedNode, getSelectedDocument, onSearch, dataStore]);

  const onDocumentDownload = useCallback(async () => {
    const record = getSelected();
    record && downloadAsBatch(record);
  }, [getSelected]);

  const onDocumentPermissions = useCallback(() => {
    const doc = getSelectedDocument();
    if (doc) {
      // TODO: permissions dialog
      showEditor({
        title: i18n.get("Permissions {0}", doc.fileName),
        model: "com.axelor.dms.db.DMSFile",
        viewName: "dms-file-permission-form",
        record: doc,
        readonly: false,
      });
    }
  }, [showEditor, getSelectedDocument]);

  const onDocumentDelete = useCallback(async () => {
    const docs = getSelectedDocuments();
    const node = getSelectedNode();

    if (!docs && !node) return;

    const records = docs || [node];
    const [record] = records;

    const confirmed = await dialogs.confirm({
      content:
        records.length > 1 ? (
          i18n.get(
            "Are you sure you want to delete the {0} selected documents?",
            records.length
          )
        ) : (
          <Box
            dangerouslySetInnerHTML={{
              __html: i18n.get(
                "Are you sure you want to delete {0}?",
                toStrongText(record.fileName)
              ),
            }}
          />
        ),
    });

    if (confirmed) {
      const result = await dataStore.delete(records);
      if (result) {
        const dirIds = records.filter((r) => r.isDirectory).map((r) => r.id);

        // remove directories from tree
        dirIds.length > 0 &&
          setTreeRecords((records) =>
            records.filter((r) => !dirIds.includes(r.id))
          );

        // set to root when selected node is deleted
        setSelected((selected) =>
          dirIds.includes(selected) ? ROOT.id : selected
        );
      }
    }
  }, [getSelectedDocuments, getSelectedNode, dataStore]);

  const handleUpload = useCallback(
    (files: FileList | null) => {
      for (let i = 0; i < (files?.length ?? 0); i++) {
        const file = files?.[i];
        if (file && uploadSize > 0 && file.size > 1048576 * uploadSize) {
          return dialogs.info({
            content: i18n.get(
              "You are not allowed to upload a file bigger than {0} MB.",
              uploadSize
            ),
          });
        }
      }
      // TODO file upload in chunk
    },
    [uploadSize]
  );

  const handleNodeSelect = useCallback((record: TreeRecord) => {
    setSelected(record.id);
  }, []);

  const handleNodeExpand = useCallback((record: TreeRecord) => {
    setExpanded((list) =>
      list.includes(record.id)
        ? list.filter((id) => id !== record.id)
        : [...list, record.id]
    );
  }, []);

  const handleDocumentOpen = useCallback(
    (e: SyntheticEvent, row: GridRow) => {
      if (row?.record?.isDirectory) {
        handleNodeSelect(row.record);
      } else if (row?.record?.id) {
        navigate(`/ds/dms.file/edit/${row.record.id}`);
      }
    },
    [handleNodeSelect, navigate]
  );

  const handleGridCellClick = useCallback(
    (
      e: React.SyntheticEvent,
      col: GridColumn,
      colIndex: number,
      row: GridRow,
      rowIndex: number
    ) => {
      const cellValue = row?.record[col?.name];
      if (cellValue === "fa fa-folder") {
        handleDocumentOpen(e, row);
      } else if (cellValue === "fa fa-download") {
        row?.record && downloadAsBatch(row.record);
      } else if (cellValue === "fa fa-info-circle") {
        // TODO open details view of record
      }
    },
    [handleDocumentOpen]
  );

  const showToolbar = popupOptions?.showToolbar !== false;

  const { offset = 0, limit = 40, totalCount = 0 } = dataStore.page;
  const canPrev = offset > 0;
  const canNext = offset + limit < totalCount;
  const records = useDataStore(dataStore, (ds) => ds.records);
  const breadcrumbs = useMemo(() => {
    function collect(parentId: TreeRecord["id"]): TreeRecord[] {
      const item = treeRecords.find((r) => r.id === parentId);
      return item ? collect(item["parent.id"]).concat([item]) : [];
    }
    return collect(selected);
  }, [treeRecords, selected]);

  return (
    <DndProvider backend={HTML5Backend}>
      <DmsOverlay className={styles.container} onUpload={handleUpload}>
        {showToolbar && (
          <ViewToolBar
            meta={meta}
            actions={[
              {
                key: "toggle",
                text: i18n.get("Toggle"),
                iconOnly: true,
                iconProps: {
                  icon: "menu",
                },
                onClick: toggleTree,
              },
              {
                key: "new",
                text: i18n.get("New"),
                hidden: !hasButton("new"),
                iconOnly: false,
                onClick: onFolderNew,
                items: [
                  {
                    key: "folder",
                    text: i18n.get("Folder"),
                    onClick: onFolderNew,
                  },
                  { key: "d1", divider: true },
                  {
                    key: "document",
                    text: i18n.get("Document"),
                    onClick: onDocumentNew,
                  },
                  {
                    key: "spreadsheet",
                    text: i18n.get("Spreadsheet"),
                    onClick: onSpreadsheetNew,
                  },
                  { key: "d2", divider: true },
                  {
                    key: "file_upload",
                    text: i18n.get("File upload"),
                    onClick: () => fileInputRef.current?.click?.(),
                  },
                ],
              },
              {
                key: "more",
                text: i18n.get("More"),
                iconProps: {
                  icon: "more_vert",
                },
                disabled: selected === ROOT.id && !selectedRows?.length,
                items: [
                  {
                    key: "rename",
                    text: i18n.get("Rename..."),
                    onClick: onDocumentRename,
                  },
                  {
                    key: "permissions",
                    text: i18n.get("Permissions..."),
                    onClick: onDocumentPermissions,
                    hidden: !selectedRows?.length,
                  },
                  { key: "d1", divider: true },
                  {
                    key: "download",
                    text: i18n.get("Download"),
                    onClick: onDocumentDownload,
                  },
                  { key: "d2", divider: true },
                  {
                    key: "delete",
                    text: i18n.get("Delete..."),
                    onClick: onDocumentDelete,
                  },
                ],
              },
            ]}
            pagination={{
              canPrev,
              canNext,
              onPrev: () => onSearch({ offset: offset - limit }),
              onNext: () => onSearch({ offset: offset + limit }),
              text: () => <PageText dataStore={dataStore} />,
            }}
          >
            <Box d="flex">
              <Box
                d="flex"
                flex={1}
                alignItems="center"
                className={styles.breadcrumbs}
              >
                <Breadcrumbs
                  data={breadcrumbs}
                  selected={selected}
                  onSelect={handleNodeSelect}
                />
              </Box>
              <AdvanceSearch
                dataStore={dataStore}
                items={view.items}
                fields={fields}
                domains={domains}
                value={advanceSearch}
                setValue={setAdvancedSearch as any}
              />
            </Box>
          </ViewToolBar>
        )}
        <Box className={styles.content}>
          <Input
            type="file"
            multiple={true}
            ref={fileInputRef}
            d="none"
            onChange={(e) => handleUpload(e.target.files)}
          />
          <Box
            className={clsx(styles.tree, {
              [styles.hide]: !showTree,
            })}
          >
            <DmsTree
              data={treeRecords}
              expanded={expanded}
              selected={selected}
              onSelect={handleNodeSelect}
              onExpand={handleNodeExpand}
            />
          </Box>
          <Box className={styles.grid} borderStart>
            <GridComponent
              records={records}
              view={view}
              fields={fields}
              state={state}
              setState={setState}
              sortType={"live"}
              onSearch={onSearch}
              onCellClick={handleGridCellClick}
              onRowDoubleClick={handleDocumentOpen}
            />
          </Box>
        </Box>
      </DmsOverlay>
    </DndProvider>
  );
}

function Breadcrumbs({
  data,
  selected,
  onSelect,
}: {
  data: TreeRecord[];
  selected?: TreeRecord["id"];
  onSelect?: (data: TreeRecord) => void;
}) {
  return (
    <Box as="ul">
      {data.map((item, ind) => {
        function render() {
          return item.id === ROOT.id ? (
            <MaterialIcon icon="home" fontSize={20} fill />
          ) : (
            item.fileName
          );
        }
        return (
          <Box key={ind} as="li">
            {selected === item.id && item.id ? (
              <Box as="span">{render()}</Box>
            ) : (
              <Link onClick={() => onSelect?.(item)}>{render()}</Link>
            )}
            {ind < data.length - 1 && (
              <Box as="span" color="muted">
                <MaterialIcon icon="chevron_right" />
              </Box>
            )}
          </Box>
        );
      })}
    </Box>
  );
}
