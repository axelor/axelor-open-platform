import { useCallback } from "react";
import { useSession } from "@/hooks/use-session";
import { i18n } from "@/services/client/i18n";
import { SavedFilter, SearchFilter, View } from "@/services/client/meta.types";
import { MetaData, removeFilter, saveFilter } from "@/services/client/meta";
import { DataStore } from "@/services/client/data-store";
import { download } from "@/utils/download";
import { useViewAction, useViewFilters } from "../views/scope";
import { dialogs } from "@/components/dialogs";
import AdvanceSearchComponent from "./advance-search";

export interface AdvancedSearchProps {
  dataStore: DataStore;
  items?: View["items"];
  value?: any;
  setValue?: () => any;
  fields?: MetaData["fields"];
  domains?: SearchFilter[];
  onSave?: any;
  onDelete?: any;
}

export default function AdvancedSearch({
  dataStore,
  ...props
}: AdvancedSearchProps) {
  const { data: sessionInfo } = useSession();
  const [filters, setFilters] = useViewFilters();
  const { name, params } = useViewAction();

  const filterView = (params || {})["search-filters"] || `act:${name}`;
  const user = sessionInfo?.user!;
  const advanceSearchConfig = sessionInfo?.view?.advanceSearch;

  const handleSave = useCallback(
    async (filter: SavedFilter) => {
      const newFilter = await saveFilter({ ...filter, filterView });
      const newFilters = [...(filters || [])];
      const ind = newFilters.findIndex((f) => f.id === filter.id);
      if (ind > -1) {
        newFilters[ind] = newFilter;
      } else {
        newFilters.push(newFilter);
      }
      setFilters(newFilters);
    },
    [filterView, filters, setFilters]
  );

  const handleDelete = useCallback(
    async (filter: SavedFilter) => {
      const confirmed = await dialogs.confirm({
        title: i18n.get("Question"),
        content: i18n.get(`Would you like to remove the filter?`),
      });
      if (!confirmed) return;

      const res = await removeFilter(filter);
      res && setFilters(filters!.filter((f) => f.id !== filter.id));
      return Boolean(res);
    },
    [filters, setFilters]
  );

  const handleExport = useCallback(() => {
    dataStore.export({}).then(({ fileName }) => {
      download(
        `ws/rest/${dataStore.model}/export/${fileName}?fileName=${fileName}`,
        fileName
      );
    });
  }, [dataStore]);

  return (
    <AdvanceSearchComponent
      canShare={advanceSearchConfig?.share !== false}
      canExportFull={advanceSearchConfig?.exportFull !== false}
      userId={user.id}
      userGroup={user.group}
      translate={i18n.get}
      filters={filters}
      onSave={handleSave}
      onDelete={handleDelete}
      onExport={handleExport}
      {...(props as any)}
    />
  );
}
