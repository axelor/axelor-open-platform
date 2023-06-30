import {
  SyntheticEvent,
  useCallback,
  useReducer,
  useState,
  useRef,
  useMemo,
  useEffect,
} from "react";
import { Box, Input, Link } from "@axelor/ui";
import { GridRow, GridColumn } from "@axelor/ui/grid";
import { useSetAtom } from "jotai";
import { useAtomCallback } from "jotai/utils";
import { uniq } from "lodash";
import { DndProvider } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
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
import { DataRecord } from "@/services/client/data.types";
import { dialogs } from "@/components/dialogs";
import { useTabs } from "@/hooks/use-tabs";
import { useSession } from "@/hooks/use-session";
import { useDataStore } from "@/hooks/use-data-store";
import { useGridState } from "../grid/builder/utils";
import { useEditor } from "@/hooks/use-relation";
import { useRoute } from "@/hooks/use-route";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { Grid as GridComponent } from "../grid/builder";
import { Uploader } from "./builder/scope";
import {
  TreeRecord,
  DmsOverlay,
  DmsUpload,
  DmsTree,
  DmsDetails,
} from "./builder";
import {
  CONTENT_TYPE,
  downloadAsBatch,
  toStrongText,
  prepareCustomView,
} from "./builder/utils";
import styles from "./dms.module.scss";
import { useResponsive } from "@/hooks/use-responsive";

const ROOT: TreeRecord = { id: null, fileName: i18n.get("Home") };
const UNDEFINED_ID = -1;

const promptInput = async (
  title: string,
  inputValue: string = "",
  yesTitle?: string
) => {
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
    yesTitle: yesTitle ?? i18n.get("Create"),
  });
  return confirmed && inputValue;
};

export function Dms(props: ViewProps<GridView>) {
  const { meta, dataStore, searchAtom } = props;
  const { view, fields } = meta;
  const { action, popup, popupOptions } = useViewTab();
  const { data: session } = useSession();
  const { hasButton } = usePerms(meta.view, meta.perms);
  const { open: openTab } = useTabs();
  const { navigate } = useRoute();
  const showEditor = useEditor();
  const setPopupHandlers = useSetAtom(usePopupHandlerAtom());

  const popupRecord = action.params?.["_popup-record"];

  const [state, setState] = useGridState();
  const [root, setRoot] = useState<TreeRecord>(popupRecord ?? ROOT);
  const [detailsId, setDetailsId] = useState<TreeRecord["id"]>(null);
  const [showDetails, setDetailsPopup] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // tree state
  const [treeRecords, setTreeRecords] = useState<TreeRecord[]>([root]);
  const [expanded, setExpanded] = useState<TreeRecord["id"][]>([root.id]);
  const [selected, setSelected] = useState<TreeRecord["id"]>(root.id);
  const [showTree, setShowTree] = useState<boolean | undefined>(undefined);

  const { orderBy, rows, selectedRows } = state;
  const uploadSize = session?.api?.upload?.maxSize ?? 0;
  const uploader = useMemo(() => new Uploader(), []);
  const { relatedId, relatedModel } = popupRecord || {};

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

  const openDMSFile = useCallback(
    (record: TreeRecord) => {
      if (popup) return;
      if (
        [CONTENT_TYPE.SPREADSHEET, CONTENT_TYPE.HTML].includes(
          record.contentType
        )
      ) {
        // open HTML/spreadsheet view
        openTab(prepareCustomView(view, record));
      } else {
        navigate(`/ds/dms.file/edit/${record.id}`);
      }
    },
    [navigate, view, popup, openTab]
  );

  const setRootIfNeeded = useCallback(
    ({ parent }: TreeRecord) => {
      if (parent && root.id === UNDEFINED_ID) {
        setRoot((record) => ({
          ...record,
          ...parent,
        }));
        setTreeRecords((records) =>
          records.map((rec) =>
            rec.id === root.id ? { ...rec, ...parent } : rec
          )
        );
        setSelected((id) => (id === root.id ? parent.id : id));
        setExpanded((ids) =>
          ids.map((id) => (id === root.id ? parent.id : id))
        );
        return true;
      }
    },
    [root]
  );

  const onSearch = useAtomCallback(
    useCallback(
      (get, set, options: Partial<SearchOptions> = {}) => {
        const { query = {} } = searchAtom ? get(searchAtom) : {};

        const sortBy = orderBy?.map(
          (column) => `${column.order === "desc" ? "-" : ""}${column.name}`
        );

        return dataStore
          .search({
            sortBy,
            filter: {
              ...query,
              _domain: `${action.domain ? `${action.domain} AND ` : ""}${
                selected
                  ? `self.parent.id = ${selected}`
                  : "self.parent is null"
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
      [searchAtom, action.domain, dataStore, selected, orderBy]
    )
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
          relatedId,
          relatedModel,
          fileName: input,
          ...data,
          ...(node?.id &&
            node.id > 0 && {
              parent: {
                id: node.id,
              },
            }),
        });
        if (record) {
          const rootChanged = setRootIfNeeded(record);
          !rootChanged && onSearch({});
        }
        return record;
      }
    },
    [
      relatedId,
      relatedModel,
      dataStore,
      setRootIfNeeded,
      getSelectedNode,
      onSearch,
    ]
  );

  const onFolderNew = useCallback(async () => {
    return onNew(i18n.get("Create folder"), i18n.get("New Folder"), {
      isDirectory: true,
    });
  }, [onNew]);

  const onDocumentNew = useCallback(async () => {
    const record = await onNew(
      i18n.get("Create document"),
      i18n.get("New Document"),
      {
        isDirectory: false,
        contentType: CONTENT_TYPE.HTML,
      }
    );
    record && openDMSFile(record);
  }, [onNew, openDMSFile]);

  const onSpreadsheetNew = useCallback(async () => {
    const record = await onNew(
      i18n.get("Create spreadsheet"),
      i18n.get("New Spreadsheet"),
      {
        isDirectory: false,
        contentType: CONTENT_TYPE.SPREADSHEET,
      }
    );
    record && openDMSFile(record);
  }, [onNew, openDMSFile]);

  const onDocumentRename = useCallback(async () => {
    const doc = getSelectedDocument();
    const node = getSelectedNode();
    const record = doc || node;

    if (!record) return;

    let input = await promptInput(
      i18n.get("Information"),
      record.fileName,
      i18n.get("Save")
    );

    if (input) {
      const updated = await dataStore.save({
        id: record.id,
        version: record.version,
        fileName: input,
      });
      if (updated && !doc) {
        setTreeRecords((records) =>
          records.map((r) =>
            r.id === updated.id ? { ...r, fileName: updated.fileName } : r
          )
        );
      }
    }
  }, [getSelectedNode, getSelectedDocument, dataStore]);

  const onDocumentSave = useCallback(
    (record: TreeRecord) => dataStore.save(record),
    [dataStore]
  );

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
        model: view.model!,
        viewName: "dms-file-permission-form",
        record: doc,
        readonly: false,
        onSelect: () => {},
      });
    }
  }, [view, showEditor, getSelectedDocument]);

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
          dirIds.includes(selected) ? root.id : selected
        );
      }
    }
  }, [getSelectedDocuments, getSelectedNode, root, dataStore]);

  const handleUpload = useCallback(
    async (files: FileList | null) => {
      if (!files) return;

      for (let i = 0; i < files.length; i++) {
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

      for (let i = 0; i < files.length; i++) {
        uploader.queue({
          file: files[i],
        });
      }

      await uploader.process();
    },
    [uploadSize, uploader]
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
        openDMSFile(row?.record);
      }
    },
    [handleNodeSelect, openDMSFile]
  );

  const handleDetailsPopupClose = useCallback(() => {
    setDetailsPopup(false);
  }, []);

  const handleGridCellClick = useCallback(
    (
      e: React.SyntheticEvent,
      col: GridColumn,
      colIndex: number,
      row: GridRow,
      rowIndex: number
    ) => {
      const record = row?.record;
      const cellValue = record?.[col?.name];
      if (cellValue === "fa fa-folder") {
        handleDocumentOpen(e, row);
      } else if (cellValue === "fa fa-download") {
        row?.record && downloadAsBatch(row.record);
      } else if (cellValue === "fa fa-info-circle") {
        setDetailsPopup(true);
        setDetailsId(record.id);
      } else if (cellValue?.includes("fa")) {
        handleDocumentOpen(e, row);
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
  const detailRecord = useMemo(
    () => (detailsId ? records.find((r) => r.id === detailsId) : null),
    [records, detailsId]
  );

  useEffect(() => {
    const parent = getSelectedNode();
    uploader.setSaveHandler(async (data: DataRecord) => {
      const record = await dataStore.save({
        relatedId,
        relatedModel,
        ...data,
        ...(parent?.id &&
          parent.id > 0 && {
            parent: {
              id: parent.id,
            },
          }),
      });
      record && setRootIfNeeded(record);
      return record;
    });
  }, [
    relatedId,
    relatedModel,
    uploader,
    dataStore,
    getSelectedNode,
    setRootIfNeeded,
  ]);

  useEffect(() => {
    !showDetails && setDetailsId(null);
  }, [showDetails]);

  useEffect(() => {
    if (popup) {
      setPopupHandlers({
        data: {
          selected: getSelectedDocuments(),
        },
      });
    }
  }, [popup, getSelectedDocuments, setPopupHandlers]);

  const size = useResponsive();

  const canShowTree = showTree ?? (size.xs || size.sm ? false : true);

  return (
    <DndProvider backend={HTML5Backend}>
      <DmsOverlay
        className={clsx(styles.container, {
          [styles.popup]: popup,
        })}
        onUpload={handleUpload}
      >
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
                onClick: () => setShowTree((show) => !show),
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
                disabled: selected === root.id && !selectedRows?.length,
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
                  root={root}
                  data={breadcrumbs}
                  selected={selected}
                  onSelect={handleNodeSelect}
                />
              </Box>
              {searchAtom && (
                <AdvanceSearch
                  stateAtom={searchAtom}
                  dataStore={dataStore}
                  items={view.items}
                  customSearch={view.customSearch}
                  freeSearch={view.freeSearch}
                  onSearch={onSearch}
                />
              )}
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
              [styles.hide]: !canShowTree,
            })}
          >
            <DmsTree
              root={root}
              data={treeRecords}
              expanded={expanded}
              selected={selected}
              onSelect={handleNodeSelect}
              onExpand={handleNodeExpand}
            />
          </Box>
          <Box className={styles.grid}>
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
            <DmsDetails
              open={showDetails}
              data={detailRecord}
              onView={openDMSFile}
              onSave={onDocumentSave}
              onClose={handleDetailsPopupClose}
            />
          </Box>
        </Box>
        <DmsUpload uploader={uploader} />
      </DmsOverlay>
    </DndProvider>
  );
}

function Breadcrumbs({
  root,
  data,
  selected,
  onSelect,
}: {
  root: TreeRecord;
  data: TreeRecord[];
  selected?: TreeRecord["id"];
  onSelect?: (data: TreeRecord) => void;
}) {
  return (
    <Box as="ul">
      {data.map((item, ind) => {
        function render() {
          return item.id === root.id ? (
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
              <Box as="span" color="secondary">
                <MaterialIcon icon="chevron_right" />
              </Box>
            )}
          </Box>
        );
      })}
    </Box>
  );
}
