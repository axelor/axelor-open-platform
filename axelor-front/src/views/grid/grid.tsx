import { useAsync } from "@/hooks/use-async";
import { GridView } from "@/services/client/meta.types";
import { useEffect } from "react";
import { ViewProps } from "../types";

export function Grid(props: ViewProps<GridView>) {
  const { dataStore } = props;

  useAsync(() => dataStore.search({}), [dataStore]);

  useEffect(() => {
    return dataStore.subscribe(() => {
      console.log(dataStore.records);
    });
  }, [dataStore]);

  return <div>Grid</div>;
}
