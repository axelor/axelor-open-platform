import { DataRecord } from "@/services/client/data.types";
import { PrimitiveAtom, SetStateAction, atom, useAtomValue } from "jotai";
import {
  ScopeProvider,
  createScope,
  molecule,
  useMolecule,
} from "bunshi/react";
import { atomFamily, useAtomCallback } from "jotai/utils";
import {
  Dispatch,
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import isEqual from "lodash/isEqual";
import uniq from "lodash/uniq";

import { JsonField, Property, Schema } from "@/services/client/meta.types";
import { parseOrderBy } from "./utils";
import { Attrs, FormAtom } from "@/views/form/builder";
import { useFormScope } from "@/views/form/builder/scope";
import { fallbackFormAtom } from "@/views/form/builder/atoms";
import { GridState } from "@axelor/ui/grid";

export type GridHandler = {
  readonly?: boolean;
  newIcon?: boolean;
  editIcon?: boolean;
  deleteIcon?: boolean;
  type?: "grid" | "panel-related";
};

export const GridContext = createContext<GridHandler>({});

export function useGridContext() {
  return useContext(GridContext);
}

export type GridExpandableEvents = Record<string, Record<number, boolean>>;
export type GridExpandableHandler = {
  selectAtom: PrimitiveAtom<Record<string, any>>;
  eventsAtom: PrimitiveAtom<GridExpandableEvents>;
  level?: number;
};

export const GridExpandableContext = createContext<GridExpandableHandler>({
  level: 0,
  eventsAtom: atom({}),
  selectAtom: atom({}),
});

export function useGridExpandableContext() {
  return useContext(GridExpandableContext);
}

export function useGridColumnNames({
  view,
  fields,
}: {
  view: Schema;
  fields?: Record<string, Property>;
}) {
  return useMemo(
    () =>
      uniq(
        [...view.items!, ...(parseOrderBy(view.orderBy) ?? [])].reduce(
          (names, item) => {
            const field = fields?.[item.name!];
            if ((item as JsonField).jsonField) {
              return [...names, (item as JsonField).jsonField as string];
            } else if (field) {
              return [
                ...names,
                field.name,
                ...(field.type?.endsWith("TO_ONE") &&
                (item as Schema).target &&
                (item as Schema).targetName &&
                (item as Schema).targetName !== field.targetName
                  ? [`${field.name}.${(item as Schema).targetName}`]
                  : []),
              ];
            }
            return names;
          },
          [] as string[],
        ),
      ),
    [fields, view.items, view.orderBy],
  );
}

export interface ItemState {
  record: DataRecord;
  model: string;
  expanded?: boolean;
  parentItemAtom?: ItemAtom;
  parentCollectionAtom?: CollectionAtom;
  rootCollectionAtom?: CollectionAtom;
}

export interface NewItemState {
  data?: DataRecord;
  refId?: null | number;
}

export type ItemAtom = PrimitiveAtom<ItemState>;

export interface CollectionState {
  expand: PrimitiveAtom<boolean>;
  items: PrimitiveAtom<ItemState[]>;
  newItem: PrimitiveAtom<NewItemState>; // add new row for top level tree-grid
  getItem: (id: number, model: string) => ItemAtom;
  formAtom: FormAtom;
  columnAttrs?: Record<string, Partial<Attrs>>;
  setColumnAttrs?: Dispatch<SetStateAction<Record<string, Partial<Attrs>>>>;
  enabled?: boolean;
  waitForActions?: () => Promise<void>;
}

export interface CollectionEditableState {
  commit?: () => Promise<any> | void;
  setCommit?: (callback: (() => Promise<any> | void) | null) => void;
}

export type CollectionAtom = PrimitiveAtom<CollectionState>;

const FALLBACK_STATE: CollectionState = {
  expand: atom(
    () => false,
    () => {},
  ),
  newItem: atom<NewItemState>({ refId: null }),
  formAtom: fallbackFormAtom,
  items: atom<ItemState[]>([]),
  getItem: (id: number, model: string) =>
    atom<ItemState>({ record: { id }, model }),
};

const CollectionScope = createScope<CollectionState>(FALLBACK_STATE);
const CollectionEditableScope = createScope<CollectionEditableState>({});

const collectionMolecule = molecule((getMol, getScope) => {
  const initialState = getScope(CollectionScope);
  return atom(initialState);
});

const collectionEditableMolecule = molecule((getMol, getScope) => {
  const initialState = getScope(CollectionEditableScope);
  return atom(initialState);
});

export function useCollectionTree() {
  const collectionAtom = useMolecule(collectionMolecule);
  return useAtomValue(collectionAtom);
}

export function useCollectionTreeEditable() {
  const collectionAtom = useMolecule(collectionEditableMolecule);
  return useAtomValue(collectionAtom);
}

export function useIsRootCollectionTree() {
  const collectionAtom = useMolecule(collectionMolecule);
  const collectionState = useAtomValue(collectionAtom);
  return collectionState === FALLBACK_STATE;
}

const ADJUST_COLUMN_WIDTH = "ADJUST_COLUMN_WIDTH";
const ADJUST_COLUMN_PADDING = "ADJUST_COLUMN_PADDING";

export function useSetRootCollectionTreeColumnAttrs(
  state: GridState,
  options: {
    defaultAttrs?: Record<string, Partial<Attrs>>;
    padding?: number;
  } = {},
) {
  const { defaultAttrs, padding = 0 } = options;
  const { setColumnAttrs } = useCollectionTree();

  useEffect(() => {
    let hasSetAdjustColumnWidth = false;
    const _columnAttrs = (state.columns ?? []).reduce(
      (colsAttrs, col) => {
        colsAttrs = {
          ...colsAttrs,
          [col.name]: {
            ...colsAttrs[col.name],
            visible: col.visible,
          } as any,
        };

        if (col.width) {
          colsAttrs[col.name] = {
            ...colsAttrs[col.name],
            computed: true,
            width: col.width,
          } as any;
        }

        if (
          !hasSetAdjustColumnWidth &&
          !col.action &&
          col.visible !== false &&
          (col.width ?? 0) > 50
        ) {
          hasSetAdjustColumnWidth = true;
          colsAttrs[col.name] = {
            ...colsAttrs[col.name],
            [ADJUST_COLUMN_PADDING]: padding,
            [ADJUST_COLUMN_WIDTH]: true,
          } as any;
        }

        return colsAttrs;
      },
      defaultAttrs ?? ({} as Record<string, Partial<Attrs>>),
    );
    setColumnAttrs?.((_attrs) =>
      isEqual(_attrs, _columnAttrs) ? _attrs : _columnAttrs,
    );
  }, [state.columns, setColumnAttrs, padding, defaultAttrs]);
}

export function useCollectionTreeColumnAttrs(
  { enabled, padding: nodePadding } = { enabled: true, padding: 0 },
) {
  const { columnAttrs } = useCollectionTree();
  const { level: expandLevel = 0 } = useGridExpandableContext();

  return useMemo(() => {
    return enabled
      ? Object.keys(columnAttrs ?? {}).reduce((obj, key) => {
          const {
            [ADJUST_COLUMN_WIDTH]: $adjustColumnWidth,
            [ADJUST_COLUMN_PADDING]: $adjustColumnPadding = 0,
            ...colAttrs
          } = columnAttrs?.[key] as any;
          return {
            ...obj,
            [key]: {
              ...colAttrs,
              ...($adjustColumnWidth && {
                width:
                  colAttrs.width -
                  ($adjustColumnPadding + nodePadding * expandLevel),
                computed: true,
              }),
            },
          };
        }, {})
      : {};
  }, [enabled, nodePadding, columnAttrs, expandLevel]);
}

export function CollectionTree(props: {
  children: JSX.Element;
  enabled?: boolean;
  waitForActions?: () => Promise<void>;
}) {
  const isRootTree = useIsRootCollectionTree();
  const { formAtom } = useFormScope();
  if (isRootTree) {
    return <CollectionRoot {...props} formAtom={formAtom} />;
  }
  return props.children;
}

function CollectionRoot({
  children,
  enabled,
  formAtom,
  waitForActions,
}: {
  children: JSX.Element;
  formAtom: FormAtom;
  enabled?: boolean;
  waitForActions?: () => Promise<void>;
}) {
  const [columnAttrs, setColumnAttrs] = useState<
    Record<string, Partial<Attrs>>
  >({});
  const newItemAtom = useMemo(() => atom<NewItemState>({ refId: null }), []);
  const commitAtom = useMemo(() => atom<any>(null), []);
  const expandAtom = useMemo(() => atom(false), []);
  const itemsAtom = useMemo(() => {
    return atom<ItemState[]>([]);
  }, []);

  const itemsFamily = useMemo(() => {
    const findItem = (items: ItemState[], id: number, model: string) =>
      items.find(
        (item) =>
          item.model === model &&
          (item.record?.cid === id || item.record?.id === id),
      );
    return atomFamily(({ id, model }: { id: number; model: string }) =>
      atom(
        (get) => {
          const items = get(itemsAtom);
          return findItem(items, id, model);
        },
        (get, set, update: SetStateAction<ItemState>) => {
          const items = get(itemsAtom);
          const item = findItem(items, id, model);
          const state =
            typeof update === "function"
              ? update(item ?? { record: { id }, model })
              : update;

          if (item) {
            set(
              itemsAtom,
              items.map((_item) => (_item === item ? state : _item)),
            );
          } else {
            set(itemsAtom, [...items, state]);
          }
        },
      ),
    );
  }, [itemsAtom]);

  const getItem = useCallback(
    (id: number, model: string) => itemsFamily({ id, model }) as ItemAtom,
    [itemsFamily],
  );

  const setCommit = useAtomCallback(
    useCallback(
      (get, set, cb: (() => Promise<any> | void) | null) => {
        set(commitAtom, () => cb);
      },
      [commitAtom],
    ),
  );

  const commit = useAtomCallback(
    useCallback(
      async (get, set) => {
        const fn = get(commitAtom);
        await fn?.();
        set(commitAtom, null);
      },
      [commitAtom],
    ),
  );

  return (
    <ScopeProvider
      scope={CollectionScope}
      value={{
        expand: expandAtom,
        items: itemsAtom,
        newItem: newItemAtom,
        formAtom,
        getItem,
        enabled,
        waitForActions,
        columnAttrs,
        setColumnAttrs,
      }}
    >
      <ScopeProvider
        scope={CollectionEditableScope}
        value={{ commit, setCommit }}
      >
        {children}
      </ScopeProvider>
    </ScopeProvider>
  );
}
