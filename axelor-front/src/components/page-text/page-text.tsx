import { FormEvent, KeyboardEvent, useCallback, useState } from "react";

import { Button, Input } from "@axelor/ui";

import { useDataStore } from "@/hooks/use-data-store";
import { DataStore } from "@/services/client/data-store";
import { i18n } from "@/services/client/i18n";
import { session } from "@/services/client/session";

import styles from "./page-text.module.scss";

export function PageText({ dataStore }: { dataStore: DataStore }) {
  const page = useDataStore(dataStore, (state) => state.page);
  const maxLimit = session.info?.api?.pagination?.maxPerPage ?? 500;
  const { offset = 0, totalCount = 0 } = page;
  const [showEditor, setShowEditor] = useState(false);
  const initialLimit = page.limit ?? maxLimit;
  const [limit, setLimit] = useState(initialLimit);

  const onChange = useCallback<React.ChangeEventHandler<HTMLInputElement>>(
    (e) => setLimit(+e.target.value),
    []
  );

  const onApply = useCallback(
    (e: FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      dataStore.search({ limit });
      setShowEditor(false);
    },
    [dataStore, limit]
  );

  const handleKeyDown = useCallback(
    (e: KeyboardEvent<HTMLFormElement>) => {
      if (e.key === "Escape") {
        e.preventDefault();
        setLimit(initialLimit);
        setShowEditor(false);
      }
    },
    [initialLimit]
  );

  const onShow = useCallback(() => setShowEditor(true), []);

  const to = Math.min(offset + limit, totalCount);
  const start = to === 0 ? 0 : offset + 1;
  const text = i18n.get("{0} to {1} of {2}", start, to, totalCount);

  if (showEditor) {
    return (
      <form
        className={styles.editor}
        onSubmit={onApply}
        onKeyDown={handleKeyDown}
      >
        <Input
          name="limit"
          type="number"
          min={0}
          max={maxLimit}
          value={limit}
          onChange={onChange}
          onFocus={(e) => e.target.select()}
          autoFocus
          style={{ width: "5rem" }}
        />
        <Button variant="secondary" type="submit">
          {i18n.get("Apply")}
        </Button>
      </form>
    );
  }
  return (
    <div className={styles.text} onClick={onShow}>
      {text}
    </div>
  );
}
