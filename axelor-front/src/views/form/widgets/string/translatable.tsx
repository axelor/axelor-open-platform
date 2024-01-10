import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { Box, Divider, Input, InputLabel, clsx } from "@axelor/ui";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useAtom } from "jotai";

import { focusAtom } from "@/utils/atoms";
import { dialogs } from "@/components/dialogs";
import { i18n } from "@/services/client/i18n";
import { DataStore } from "@/services/client/data-store";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { DataRecord } from "@/services/client/data.types";
import { DeleteOption } from "@/services/client/data";
import { useSession } from "@/hooks/use-session";
import { FieldProps } from "../../builder";
import styles from "./translatable.module.scss";

const ds = new DataStore("com.axelor.meta.db.MetaTranslation", {
  sortBy: ["id"],
});

function Translations({
  value,
  onUpdate,
}: {
  value: string;
  onUpdate: (values: DataRecord[]) => void;
}) {
  const [values, setValues] = useState<DataRecord[]>([]);

  function handleAdd() {
    setValues((values) => [...values, {}]);
  }

  function handleChange(key: string, value: any, ind: number) {
    setValues((values) =>
      values.map((v, i) => (ind === i ? { ...v, [key]: value } : v)),
    );
  }

  function handleRemove(ind: number) {
    setValues((values) => values.filter((v, i) => i !== ind));
  }

  useAsyncEffect(async () => {
    const key = `value:${value}`;
    const { records = [] } = await ds.search({
      filter: {
        _domain: "self.key = :key",
        _domainContext: {
          key,
        },
      },
    });
    setValues(records.map((record) => ({ ...record, $data: record })));
  }, [value]);

  useEffect(() => {
    onUpdate(values);
  }, [values, onUpdate]);

  return (
    <Box d="flex" flex={1} flexDirection="column">
      <Box flex={1}>
        <InputLabel>{i18n.get("Value")}</InputLabel>
        <Input data-input type="text" readOnly defaultValue={value} />
      </Box>
      <Box my={2} flex={1}>
        <Divider />
      </Box>
      <Box className={styles.list} flex={1}>
        <Box gap={5}>
          <InputLabel>{i18n.get("Translation")}</InputLabel>
          <InputLabel>{i18n.get("Language")}</InputLabel>
        </Box>
        {values.map((value, ind) => {
          if (value.$removed) return null;
          return (
            <Box key={ind} gap={5} my={2}>
              <Box>
                <Input
                  data-input
                  id={`${ind}-msg`}
                  type="text"
                  required
                  value={value.message || ""}
                  invalid={!value.message}
                  onChange={(e) => handleChange("message", e.target.value, ind)}
                />
              </Box>
              <Box d="flex" alignItems="center" gap={4}>
                <Input
                  data-input
                  id={`${ind}-lang`}
                  type="text"
                  required
                  value={value.language || ""}
                  invalid={!value.language}
                  onChange={(e) =>
                    handleChange("language", e.target.value, ind)
                  }
                />
                <MaterialIcon
                  icon="close"
                  className={styles.icon}
                  onClick={() =>
                    value.id
                      ? handleChange("$removed", true, ind)
                      : handleRemove(ind)
                  }
                />
              </Box>
            </Box>
          );
        })}
      </Box>
      <Box>
        <MaterialIcon icon="add" className={styles.icon} onClick={handleAdd} />
      </Box>
    </Box>
  );
}

export function useTranslationValue({
  schema: { name },
  formAtom,
}: FieldProps<string>) {
  const trKey = `$t:${name}`;
  return useAtom(
    useMemo(
      () =>
        focusAtom(
          formAtom,
          ({ record }) => record[trKey],
          ({ record, ...rest }, value) => ({
            ...rest,
            record: { ...record, [trKey]: value },
          }),
        ),
      [formAtom, trKey],
    ),
  );
}

export function useTranslateModal({
  value,
  onUpdate,
}: {
  value: string;
  onUpdate: (val: string) => void;
}) {
  const lang = useSession().data?.user?.lang;

  return useCallback(async () => {
    let values: DataRecord[] = [];

    await dialogs.modal({
      open: true,
      title: i18n.get("Translations"),
      content: (
        <Box d="flex" p={3} flex={1}>
          <Translations
            value={value}
            onUpdate={(_values) => {
              values = _values;
            }}
          />
        </Box>
      ),
      onClose: async (isOk) => {
        const key = `value:${value}`;
        if (isOk) {
          const removed = values
            .filter((v) => v.$removed)
            .map(({ id, version }) => ({ id, version }) as DeleteOption);

          removed.length && (await ds.delete(removed));

          const updated = values
            .filter(
              ({ language, message, $data }) =>
                language &&
                message &&
                ($data?.language !== language || $data?.message !== message),
            )
            .map(({ id, version, message, language }) => ({
              id,
              version,
              message,
              language,
              key,
            }));

          updated.length && (await ds.save(updated));

          const value = values.find(
            (v) => v.language && v.message && v.language === lang,
          );

          if (value) {
            onUpdate(value.$removed ? "" : value.message);
          }
        }
      },
    });
  }, [lang, value, onUpdate]);
}

export function Translatable({
  value,
  position = "bottom",
  className,
  onUpdate,
}: {
  value: string;
  position?: "top" | "bottom";
  className?: string;
  onUpdate: (val: string) => void;
}) {
  const showModal = useTranslateModal({
    value,
    onUpdate,
  });

  return (
    <span
      onClick={showModal}
      className={clsx(styles.container, styles[position], className)}
    >
      <MaterialIcon icon="flag" fill />
    </span>
  );
}
