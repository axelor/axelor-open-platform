import { useCallback, useState } from "react";

import { Button, Input } from "@axelor/ui";

import { useDataStore } from "@/hooks/use-data-store";
import { DataStore } from "@/services/client/data-store";
import { i18n } from "@/services/client/i18n";
import { session } from "@/services/client/session";

import styles from "./page-text.module.scss";

export function PageText({ dataStore }: { dataStore: DataStore }) {
  const page = useDataStore(dataStore, (state) => state.page);
  const maxLimit = session.info?.api?.pagination?.maxPerPage ?? 40;
  const { offset = 0, totalCount = 0 } = page;
  const [showEditor, setShowEditor] = useState(false);
  const [limit, setLimit] = useState(page.limit ?? maxLimit);

  const onChange = useCallback<React.ChangeEventHandler<HTMLInputElement>>(
    (e) => setLimit(+e.target.value),
    []
  );

  const onApply = useCallback(() => {
    dataStore.search({ limit });
    setShowEditor(false);
  }, [dataStore, limit]);

  const onShow = useCallback(() => setShowEditor(true), []);

  const to = Math.min(offset + limit, totalCount);
  const text = i18n.get("{0} to {1} of {2}", offset + 1, to, totalCount);

  if (showEditor) {
    return (
      <div className={styles.editor}>
        <Input
          type="number"
          min={0}
          max={maxLimit}
          value={limit}
          onChange={onChange}
        />
        <Button variant="secondary" onClick={onApply}>
          {i18n.get("Apply")}
        </Button>
      </div>
    );
  }
  return (
    <div className={styles.text} onClick={onShow}>
      {text}
    </div>
  );
}
