import { useAtom, useAtomValue } from "jotai";
import { MouseEvent, useCallback, useRef } from "react";

import { usePerms } from "@/hooks/use-perms";
import { useCompletion, useEditor, useSelector } from "@/hooks/use-relation";
import { DataSource } from "@/services/client/data";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { toKebabCase } from "@/utils/names";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useAtomCallback } from "jotai/utils";
import { FieldContainer, FieldProps } from "../../builder";
import { ViewerInput, ViewerLink } from "../string";
import {
  CreatableSelect,
  CreatableSelectProps,
} from "../tag-select/creatable-select";

export function ManyToOne(props: FieldProps<DataRecord>) {
  const { schema, formAtom, valueAtom, widgetAtom, readonly, invalid } = props;
  const {
    uid,
    target,
    targetName,
    targetSearch,
    widget,
    placeholder,
    formView,
    gridView,
    showTitle = true,
  } = schema;

  const [value, setValue] = useAtom(valueAtom);
  const { hasButton } = usePerms(schema);

  const {
    attrs: { title, focus, domain },
  } = useAtomValue(widgetAtom);

  const isSuggestBox = toKebabCase(widget) === "suggest-box";
  const showSelector = useSelector();
  const showEditor = useEditor();

  const search = useCompletion({
    target,
    targetName,
    targetSearch,
  });

  const handleChange = useCallback(
    (value: DataRecord | null) => {
      if (value && value.id && value.id > 0) {
        const { version, ...rec } = value;
        setValue(rec, true);
      } else {
        setValue(value, true);
      }
    },
    [setValue]
  );

  const canView = value && hasButton("view");
  const canEdit = value && hasButton("edit") && schema.canEdit === true;
  const canNew = hasButton("new") && schema.canNew === true;
  const canSelect = hasButton("select");

  const ensureRelated = useAtomCallback(
    useCallback(
      async (get, set, value: DataRecord) => {
        if (value && value.id && value.id > 0) {
          const name = schema.name;
          const prefix = name + ".";
          const { fields } = get(formAtom);
          const related = Object.keys(fields)
            .filter((x) => x.startsWith(prefix))
            .map((x) => x.substring(prefix.length));
          const names = [targetName, ...related];
          const missing = names.filter((x) => value[x] === undefined);
          if (missing.length > 0) {
            const ds = new DataSource(target);
            const rec = await ds.read(value.id, {
              fields: missing,
            });
            return { ...value, ...rec, version: undefined };
          }
        }
        return value;
      },
      [formAtom, schema.name, target, targetName]
    )
  );

  const handleEdit = useCallback(
    async (readonly = false, record?: DataContext) => {
      showEditor({
        title: title ?? "",
        model: target,
        viewName: formView,
        record: record ?? value,
        readonly,
        onSelect: handleChange,
      });
    },
    [showEditor, title, target, formView, value, handleChange]
  );

  const handleView = useCallback(
    (e: MouseEvent<HTMLButtonElement>) => {
      e.preventDefault();
      return handleEdit(true);
    },
    [handleEdit]
  );

  const handleCreate = useCallback(
    (record?: DataContext, readonly?: boolean) => {
      return handleEdit(readonly ?? false, record);
    },
    [handleEdit]
  );

  const handleSelect = useAtomCallback(
    useCallback(
      (get) => {
        showSelector({
          title: i18n.get("Select {0}", title ?? ""),
          model: target,
          viewName: gridView,
          multiple: false,
          domain: domain,
          context: get(formAtom).record,
          onSelect: async (records) => {
            handleChange(records[0]);
          },
        });
      },
      [showSelector, title, target, gridView, domain, formAtom, handleChange]
    )
  );

  const handleCompletion = useAtomCallback(
    useCallback(
      async (get, set, value: string) => {
        const res = await search(value, {
          _domain: domain,
          _domainContext: get(formAtom).record,
        });
        const { records } = res;
        return records;
      },
      [domain, formAtom, search]
    )
  );

  const valueRef = useRef(value);

  const ensureRelatedValues = useAtomCallback(
    useCallback(
      async (get, set, signal: AbortSignal) => {
        if (valueRef.current === value) return;
        if (value) {
          const newValue = await ensureRelated(value);
          if (newValue !== value) {
            valueRef.current = newValue;
            if (signal.aborted) return;
            setValue(newValue, false, false);
          } else {
            valueRef.current = value;
          }
        }
      },
      [ensureRelated, setValue, value]
    )
  );

  useAsyncEffect(ensureRelatedValues, [ensureRelatedValues]);

  return (
    <FieldContainer>
      {showTitle && <label htmlFor={uid}>{title}</label>}
      {readonly ? (
        value && hasButton("view") ? (
          <ViewerLink onClick={handleView}>{value[targetName]}</ViewerLink>
        ) : (
          <ViewerInput value={value?.[targetName] || ""} />
        )
      ) : (
        <CreatableSelect
          autoFocus={focus}
          schema={schema}
          canCreate={canNew}
          onCreate={handleCreate as CreatableSelectProps["onCreate"]}
          onChange={handleChange}
          invalid={invalid}
          value={value ?? null}
          placeholder={placeholder}
          icons={
            isSuggestBox
              ? [{ icon: "arrow_drop_down" }]
              : [
                  {
                    hidden: !canEdit || !canView,
                    icon: "edit",
                    onClick: () => handleEdit(),
                  },
                  {
                    hidden: canEdit || !canView,
                    icon: "description",
                    onClick: () => handleEdit(true),
                  },
                  {
                    hidden: !canNew,
                    icon: "add",
                    onClick: () => handleEdit(false, { id: null }),
                  },
                  {
                    hidden: !canSelect,
                    icon: "search",
                    onClick: handleSelect,
                  },
                ]
          }
          fetchOptions={handleCompletion}
          optionLabel={targetName}
          optionValue={"id"}
        />
      )}
    </FieldContainer>
  );
}
