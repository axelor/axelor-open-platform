import { Box, Divider, Input, InputLabel, clsx } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { useAtom } from "jotai";
import { useCallback, useEffect, useMemo, useState } from "react";

import { dialogs } from "@/components/dialogs";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useSession } from "@/hooks/use-session";
import { DeleteOption } from "@/services/client/data";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { focusAtom } from "@/utils/atoms";
import { FieldProps } from "../../builder";
import { useFormScope } from "../../builder/scope";
import styles from "./translatable.module.scss";

const ds = new DataStore("com.axelor.meta.db.MetaTranslation", {
  sortBy: ["id"],
});

function Translations({
  value: originalValue,
  onUpdate,
}: {
  value: string;
  onUpdate: (value: string, translations: DataRecord[]) => void;
}) {
  const [value, setValue] = useState(originalValue);
  const [translations, setTranslations] = useState<DataRecord[]>([]);

  const handleValueChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    setValue(value);
    setTranslations((values) =>
      values.map((v) => ({ ...v, key: `value:${value}` })),
    );
  };

  function handleAdd() {
    setTranslations((values) => [...values, {}]);
  }

  function handleChange(key: string, value: any, ind: number) {
    setTranslations((values) =>
      values.map((v, i) => (ind === i ? { ...v, [key]: value } : v)),
    );
  }

  function handleRemove(ind: number) {
    setTranslations((values) => values.filter((v, i) => i !== ind));
  }

  useAsyncEffect(async () => {
    const key = `value:${originalValue}`;
    const { records = [] } = await ds.search({
      filter: {
        _domain: "self.key = :key",
        _domainContext: {
          key,
        },
      },
    });
    setTranslations(records.map((record) => ({ ...record, $data: record })));
  }, [originalValue]);

  useEffect(() => {
    onUpdate(value, translations);
  }, [value, translations, onUpdate]);

  return (
    <Box d="flex" flex={1} flexDirection="column">
      <Box flex={1}>
        <InputLabel>{i18n.get("Value")}</InputLabel>
        <Input
          data-input
          type="text"
          value={value}
          onChange={handleValueChange}
        />
      </Box>
      <Box my={2} flex={1}>
        <Divider />
      </Box>
      <Box className={styles.list} flex={1}>
        <Box gap={5}>
          <InputLabel>{i18n.get("Translation")}</InputLabel>
          <InputLabel>{i18n.get("Language")}</InputLabel>
        </Box>
        {translations.map((value, ind) => {
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
  value: originalValue,
  onValueChange,
  onUpdate,
}: {
  value: string;
  onValueChange: (val: string, fireOnChange?: boolean) => void;
  onUpdate: (val: string) => void;
}) {
  const { actionHandler, actionExecutor } = useFormScope();
  const lang = useSession().data?.user?.lang;

  return useCallback(async () => {
    let value = originalValue;
    let translations: DataRecord[] = [];

    await dialogs.modal({
      open: true,
      title: i18n.get("Translation"),
      content: (
        <Box d="flex" p={3} flex={1}>
          <Translations
            value={originalValue}
            onUpdate={(_value, _translations) => {
              value = _value;
              translations = _translations;
            }}
          />
        </Box>
      ),
      onClose: async (isOk) => {
        if (!isOk) return;

        if (onValueChange && value !== originalValue) {
          onValueChange(value, true);
          await actionExecutor.waitFor();
          await actionExecutor.wait();
          await actionHandler.save();
        }

        const key = `value:${value}`;
        const removed = translations
          .filter((v) => v.$removed)
          .map(({ id, version }) => ({ id, version }) as DeleteOption);

        removed.length && (await ds.delete(removed));

        const updated = translations
          .filter(
            ({ language, key, message, $data }) =>
              language &&
              message &&
              ($data?.language !== language ||
                $data.key !== key ||
                $data?.message !== message),
          )
          .map(({ id, version, message, language }) => ({
            id,
            version,
            message,
            language,
            key,
          }));

        updated.length && (await ds.save(updated));

        const translation = translations.find(
          (v) => v.language && v.message && v.language === lang,
        );

        if (translation) {
          onUpdate(translation.$removed ? "" : translation.message);
        }
      },
    });
  }, [
    actionExecutor,
    actionHandler,
    lang,
    onUpdate,
    originalValue,
    onValueChange,
  ]);
}

export function Translatable({
  value,
  onValueChange,
  position = "bottom",
  className,
  onUpdate,
}: {
  value: string;
  onValueChange: (val: string, fireOnChange?: boolean) => void;
  position?: "top" | "bottom";
  className?: string;
  onUpdate: (val: string) => void;
}) {
  const showModal = useTranslateModal({ value, onValueChange, onUpdate });

  return (
    <span
      onClick={showModal}
      className={clsx(styles.container, styles[position], className)}
    >
      <MaterialIcon icon="flag" fill />
    </span>
  );
}
