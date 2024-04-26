import { DataRecord } from "@/services/client/data.types";
import { PrimitiveAtom, SetStateAction, atom, useAtomValue } from "jotai";
import {
  ScopeProvider,
  createScope,
  molecule,
  useMolecule,
} from "bunshi/react";
import { atomFamily, useAtomCallback } from "jotai/utils";
import { createContext, useCallback, useContext, useMemo } from "react";
import uniq from "lodash/uniq";

import { JsonField, Property, Schema } from "@/services/client/meta.types";
import { parseOrderBy } from "./utils";

export type GridHandler = {
  readonly?: boolean;
};

export const GridContext = createContext<GridHandler>({});

export function useGridContext() {
  return useContext(GridContext);
}

export type GridExpandableHandler = {
  selectAtom: PrimitiveAtom<Record<string, any>>;
  level?: number;
};

export const GridExpandableContext = createContext<GridExpandableHandler>({
  level: 0,
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

export type ItemAtom = PrimitiveAtom<ItemState>;

export interface CollectionState {
  expand: PrimitiveAtom<boolean>;
  items: PrimitiveAtom<ItemState[]>;
  getItem: (id: number, model: string) => ItemAtom;
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

export function CollectionTree(props: {
  children: JSX.Element;
  enabled?: boolean;
  waitForActions?: () => Promise<void>;
}) {
  const isRootTree = useIsRootCollectionTree();
  if (isRootTree) {
    return <CollectionRoot {...props} />;
  }
  return props.children;
}

function CollectionRoot({
  children,
  enabled,
  waitForActions,
}: {
  children: JSX.Element;
  enabled?: boolean;
  waitForActions?: () => Promise<void>;
}) {
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
        getItem,
        enabled,
        waitForActions,
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
