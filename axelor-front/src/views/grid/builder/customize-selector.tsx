import { useCallback, useEffect, useMemo, useState } from "react";
import { atom } from "jotai";
import { useAtomCallback } from "jotai/utils";

import { Grid, GridProvider } from "@axelor/ui/grid";
import { Box } from "@axelor/ui";

import { GridView } from "@/services/client/meta.types";
import { DataRecord } from "@/services/client/data.types";
import { DataStore } from "@/services/client/data-store";
import { Loader } from "@/components/loader/loader";
import { useAsync } from "@/hooks/use-async";
import { i18n } from "@/services/client/i18n";
import { MetaData } from "@/services/client/meta";
import { nextId } from "@/views/form/builder/utils";
import { toTitleCase } from "@/utils/names";
import { unaccent } from "@/utils/sanitize";

import { useGridState } from "./utils";
import { SearchColumn } from "../renderers/search";

const dataStore = new DataStore("com.axelor.meta.db.MetaField");

export function CustomizeSelectorDialog({
  view,
  jsonFields,
  onSelectionChange,
  excludeFields,
}: {
  view: GridView;
  jsonFields?: MetaData["jsonFields"];
  onSelectionChange: (records: DataRecord[]) => void;
  excludeFields?: string[];
}) {
  const [state, setState] = useGridState({
    orderBy: [{ name: "label", order: "asc" }],
  });
  const [search, setSearch] = useState<Record<string, string> | null>(null);

  const searchAtom = useMemo(() => atom<Record<string, string>>({}), []);

  const onGridColumnSearch = useAtomCallback(
    useCallback(
      (get) => {
        const keyValues = get(searchAtom);
        const hasValues = Object.values(keyValues).some(Boolean);
        setSearch(hasValues ? keyValues : null);
      },
      [searchAtom],
    ),
  );

  const searchColumnRenderer = useMemo(() => {
    return (props: any) => (
      <SearchColumn
        {...props}
        dataAtom={searchAtom}
        onSearch={onGridColumnSearch}
      />
    );
  }, [searchAtom, onGridColumnSearch]);

  const { data: records, state: dataState } = useAsync<
    DataRecord[]
  >(async () => {
    let { records: _records } = await dataStore.search({
      filter: {
        _domain:
          "self.metaModel.fullName = :_metaModelName AND self.name NOT IN :_excludedFieldNames",
        _domainContext: {
          _model: dataStore.model,
          _metaModelName: view.model,
          _excludedFieldNames: ["id", "version"],
        },
      },
      limit: -1,
      fields: ["label", "name"],
    });

    _records = _records.map((rec) => ({
      ...rec,
      type: "field",
      label: i18n.get(rec.label || toTitleCase(rec.name ?? "")),
    }));

    // Custom fields
    const customFields = Object.values(jsonFields ?? []).flatMap((fields) =>
      Object.values(fields)
        .filter((item) => !item.forceHidden)
        .map((item) => ({
          id: nextId(),
          name: `${item.jsonField}.${item.name}`,
          type: "field",
          label: item.title || item.autoTitle,
        })),
    ) as DataRecord[];

    // Extra fields from view
    const extraFields = view.items
      ?.filter((item) => item.name?.includes(".") || item.type !== "field")
      .map((item) => ({
        id: nextId(),
        name: item.name,
        type: item.type === "button" ? "button" : "field",
        label: item.title || item.autoTitle,
      })) as DataRecord[];

    return (
      [..._records, ...customFields, ...extraFields]
        // exclude fields already in the grid
        .filter((value) => (excludeFields || []).indexOf(value.name) < 0)
        // remove duplicate
        .reduce((acc, cur) => {
          if (!acc.find((item: DataRecord) => item.name === cur.name)) {
            acc.push(cur);
          }
          return acc;
        }, [] as DataRecord[])
    );
  }, [view, jsonFields, excludeFields]);

  const gridColumns = useMemo(
    () => [
      {
        type: "field",
        name: "label",
        title: i18n.get("Title"),
      },
      {
        type: "field",
        name: "name",
        title: i18n.get("Name"),
      },
    ],
    [],
  );

  const gridRecords = useMemo(
    () =>
      search && records
        ? records.filter((rec) =>
            Object.keys(search)
              .filter((k) => search[k])
              .every((k) =>
                unaccent((rec[k] ?? "").toLowerCase()).includes(
                  unaccent(search[k].toLowerCase()),
                ),
              ),
          )
        : records,
    [search, records],
  );

  useEffect(() => {
    const selectedRecords =
      state.selectedRows?.map((index) => state.rows[index].record) ?? [];
    onSelectionChange(selectedRecords);
  }, [onSelectionChange, state.selectedRows, state.rows]);

  return (
    <Box d="flex" flex={1} style={{ minHeight: "30vh" }}>
      {dataState === "loading" && <Loader />}
      {dataState == "hasData" && (
        <GridProvider>
          <Grid
            allowSelection
            allowCheckboxSelection
            allowCellSelection
            allowSorting
            sortType="state"
            selectionType="multiple"
            records={gridRecords as DataRecord[]}
            columns={gridColumns}
            state={state}
            setState={setState}
            allowSearch={true}
            searchRowRenderer={Box}
            searchColumnRenderer={searchColumnRenderer}
          />
        </GridProvider>
      )}
    </Box>
  );
}
