import { FormEvent, KeyboardEvent, useCallback, useState } from "react";

import { Button, Input } from "@axelor/ui";

import { alerts } from "@/components/alerts";
import { useDataStore } from "@/hooks/use-data-store";
import { DataStore } from "@/services/client/data-store";
import { i18n } from "@/services/client/i18n";
import { getDefaultMaxPerPage } from "@/utils/app-settings.ts";

import styles from "./page-text.module.scss";

export function PageText({ dataStore }: { dataStore: DataStore }) {
  const page = useDataStore(dataStore, (state) => state.page);
  const maxLimit = getDefaultMaxPerPage();
  const { offset = 0, totalCount = 0 } = page;
  const [showEditor, setShowEditor] = useState(false);
  const limit = page.limit ?? maxLimit;
  const [userPageSize, setUserPageSize] = useState(limit);

  const onChange = useCallback<React.ChangeEventHandler<HTMLInputElement>>(
    (e) => setUserPageSize(+e.target.value),
    [],
  );

  const onApply = useCallback(
    (e: FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      let size = userPageSize;
      if (
        getDefaultMaxPerPage() > 0 &&
        (userPageSize == 0 || userPageSize > getDefaultMaxPerPage())
      ) {
        size = getDefaultMaxPerPage();
        setUserPageSize(getDefaultMaxPerPage());
        alerts.warn({
          message: i18n.get("Page size limited to {0} records", size),
        });
      }
      dataStore.search({ limit: size });
      setShowEditor(false);
    },
    [dataStore, userPageSize],
  );

  const handleKeyDown = useCallback(
    (e: KeyboardEvent<HTMLFormElement>) => {
      if (e.key === "Escape") {
        e.preventDefault();
        setUserPageSize(limit);
        setShowEditor(false);
      }
    },
    [limit],
  );

  const onShow = useCallback(() => setShowEditor(true), []);

  const to = limit > 0 ? Math.min(offset + limit, totalCount) : totalCount;
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
          value={userPageSize}
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
