import { useAtom, useAtomValue } from "jotai";
import { useAtomCallback } from "jotai/utils";
import { MouseEvent, useCallback, useRef, useState } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import {
  useBeforeSelect,
  useCompletion,
  useEditor,
  useEditorInTab,
  useSelector,
} from "@/hooks/use-relation";
import { DataSource } from "@/services/client/data";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { toKebabCase } from "@/utils/names";

import { usePermission, usePrepareContext } from "../../builder/form";
import { FieldControl } from "../../builder/form-field";
import { FieldProps } from "../../builder/types";
import { ViewerInput, ViewerLink } from "../string/viewer";
import {
  CreatableSelect,
  CreatableSelectProps,
} from "../tag-select/creatable-select";

export function ManyToOne(props: FieldProps<DataRecord>) {
  const { schema, formAtom, valueAtom, widgetAtom, readonly, invalid } = props;
  const {
    target,
    targetName,
    targetSearch,
    widget,
    placeholder,
    orderBy: sortBy,
    formView,
    gridView,
    limit,
    searchLimit,
  } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const { hasButton } = usePermission(schema, widgetAtom);
  const [hasMore, setHasMore] = useState(false);

  const { attrs } = useAtomValue(widgetAtom);
  const { title, focus, domain } = attrs;

  const isSuggestBox = toKebabCase(widget) === "suggest-box";

  const getContext = usePrepareContext(formAtom);
  const showSelector = useSelector();
  const showEditor = useEditor();
  const showEditorInTab = useEditorInTab(schema);

  const search = useCompletion({
    sortBy,
    limit,
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
  const canEdit = value && hasButton("edit") && attrs.canEdit;
  const canNew = hasButton("new") && attrs.canNew;
  const canSelect = hasButton("select");
  const isRefLink = schema.widget === "ref-link";

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
      const $record = record ?? value;
      if (showEditorInTab && ($record?.id ?? 0) > 0) {
        return showEditorInTab($record!, readonly);
      }
      showEditor({
        title: title ?? "",
        model: target,
        viewName: formView,
        record: $record,
        readonly,
        context: {
          _parent: getContext(),
        },
        onSelect: handleChange,
      });
    },
    [
      title,
      target,
      formView,
      value,
      getContext,
      showEditor,
      handleChange,
      showEditorInTab,
    ]
  );

  const handleView = useCallback(
    async (e: MouseEvent<HTMLButtonElement>) => {
      e.preventDefault();
      if (isRefLink && showEditorInTab && value?.id) {
        return showEditorInTab(value, true);
      }
      return handleEdit(true);
    },
    [isRefLink, value, handleEdit, showEditorInTab]
  );

  const handleCreate = useCallback(
    (record?: DataContext, readonly?: boolean) => {
      return handleEdit(readonly ?? false, record);
    },
    [handleEdit]
  );

  const [beforeSelect, beforeSelectProps] = useBeforeSelect(schema);

  const handleSelect = useCallback(async () => {
    const _domain = (await beforeSelect(true)) ?? domain;
    const _domainContext = _domain ? getContext() : {};
    showSelector({
      model: target,
      viewName: gridView,
      orderBy: sortBy,
      multiple: false,
      domain: _domain,
      context: _domainContext,
      limit: searchLimit,
      onSelect: async (records) => {
        const value = await ensureRelated(records[0]);
        handleChange(value);
      },
    });
  }, [
    beforeSelect,
    domain,
    getContext,
    showSelector,
    target,
    gridView,
    sortBy,
    searchLimit,
    ensureRelated,
    handleChange,
  ]);

  const handleCompletion = useCallback(
    async (value: string) => {
      if (!canSelect) return [];
      const _domain = (await beforeSelect()) ?? domain;
      const _domainContext = _domain ? getContext() : {};
      const options = {
        _domain,
        _domainContext,
      };
      const { records, page } = await search(value, options);
      setHasMore((page.totalCount ?? 0) > records.length);
      return records;
    },
    [canSelect, beforeSelect, domain, getContext, search]
  );

  const valueRef = useRef<DataRecord>();

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

  const getOptionLabel = useCallback(
    (option: any) => {
      const trKey = `$t:${targetName}`;
      return option[trKey] ?? option[targetName];
    },
    [targetName]
  );

  useAsyncEffect(ensureRelatedValues, [ensureRelatedValues]);

  return (
    <FieldControl {...props}>
      {readonly ? (
        value && hasButton("view") ? (
          <ViewerLink onClick={handleView}>{getOptionLabel(value)}</ViewerLink>
        ) : (
          <ViewerInput value={value?.[targetName] || ""} />
        )
      ) : (
        <CreatableSelect
          {...(focus && { key: "focused" })}
          autoFocus={focus}
          schema={schema}
          onChange={handleChange}
          invalid={invalid}
          value={value ?? null}
          placeholder={placeholder}
          icons={
            isSuggestBox
              ? [
                  {
                    hidden: !canEdit || !canView,
                    icon: "edit",
                    onClick: () => handleEdit(),
                  },
                  { icon: "arrow_drop_down" },
                ]
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
          optionLabel={getOptionLabel}
          optionValue={"id"}
          {...(canSelect && {
            canCreate: canNew,
            canSearch: hasMore,
            onCreate: handleCreate as CreatableSelectProps["onCreate"],
            onSearch: handleSelect,
          })}
          {...beforeSelectProps}
        />
      )}
    </FieldControl>
  );
}
