import { useAsync } from "@/hooks/use-async";
import { DataStore } from "@/hooks/use-data-store";
import { Tab } from "@/hooks/use-tabs";
import { ViewData } from "@/services/client/meta";

export function Grid(props: {
  tab: Tab;
  schema: ViewData<any>;
  dataStore: DataStore;
}) {
  const { search } = props.dataStore;
  const data = useAsync(async () => {
    return await search({});
  }, []);

  if (data.state === "loading") {
    return <div>Loading</div>;
  }

  return <div>Grid</div>;
}
