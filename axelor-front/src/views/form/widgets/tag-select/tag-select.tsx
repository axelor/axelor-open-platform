import { useAtom, useAtomValue } from "jotai";
import { useAtomCallback } from "jotai/utils";
import { MouseEvent, useCallback, useMemo, useState } from "react";
import { Box, SelectProps } from "@axelor/ui";

import {
  EditorOptions,
  useBeforeSelect,
  useCompletion,
  useEditor,
  useEditorInTab,
  useSelector,
} from "@/hooks/use-relation";
import { DataRecord } from "@/services/client/data.types";

import {
  FieldControl,
  FieldProps,
  usePermission,
  usePrepareContext,
} from "../../builder";
import { Chip } from "../selection";
import { CreatableSelect, CreatableSelectProps } from "./creatable-select";

export function TagSelectComponent({
  onView,
  ...props
}: CreatableSelectProps & {
  onView?: (e: any, value: DataRecord, readonly?: boolean) => void;
}) {
  const { targetName } = props.schema;

  const getOptionLabel = useCallback(
    (option: any) => {
      const trKey = `$t:${targetName}`;
      return option[trKey] ?? option[targetName];
    },
    [targetName]
  );

  const components = useMemo(
    () => ({
      MultiValue: (props: any) => {
        const { data, removeProps } = props;
        return (
          <Box me={1} onMouseDown={(e) => onView?.(e, data, false)}>
            <Chip
              color={"indigo"}
              title={getOptionLabel(data)}
              onRemove={removeProps.onClick}
            />
          </Box>
        );
      },
    }),
    [getOptionLabel, onView]
  );

  return (
    <CreatableSelect
      isMulti
      optionLabel={getOptionLabel}
      optionValue={"id"}
      components={components}
      {...props}
      icons={[{ icon: "arrow_drop_down" }]}
    />
  );
}

export function TagSelect(
  props: FieldProps<DataRecord[]> & {
    selectProps?: Partial<SelectProps>;
  }
) {
  const { schema, valueAtom, formAtom, widgetAtom, readonly, selectProps } =
    props;
  const {
    target,
    targetName,
    targetSearch,
    placeholder,
    gridView,
    formView,
    orderBy: sortBy,
    limit,
    searchLimit,
  } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const [hasMore, setHasMore] = useState(false);

  const [value, setValue] = useAtom(valueAtom);
  const showEditor = useEditor();
  const showSelector = useSelector();
  const showEditorInTab = useEditorInTab(schema);
  const getContext = usePrepareContext(formAtom);
  const { hasButton } = usePermission(schema, widgetAtom);

  const { title, focus, domain } = attrs;
  const canNew = hasButton("new");

  const search = useCompletion({
    sortBy,
    limit,
    target,
    targetName,
    targetSearch,
  });

  const handleSelect = useAtomCallback(
    useCallback(
      (get, set, records: DataRecord[]) => {
        const values = get(valueAtom) || [];
        const ids = values.map((x) => x.id);
        const newValues = records.filter(({ id }) => !ids.includes(id));

        set(valueAtom, [
          ...values.map?.((v) => {
            const record = records.find(
              (record) => v.id === record.id && (record?.id ?? 0) > 0
            );
            return record ? { ...v, ...record, version: undefined } : v;
          }),
          ...newValues.map((value) => ({ ...value, version: undefined })),
        ]);
      },
      [valueAtom]
    )
  );

  const handleEdit = useCallback(
    async (
      value: DataRecord,
      readonly = false,
      onSelect?: EditorOptions["onSelect"]
    ) => {
      if (showEditorInTab && (value?.id ?? 0) > 0) {
        return showEditorInTab(value!, readonly);
      }
      return showEditor({
        title: title ?? "",
        model: target,
        viewName: formView,
        record: value,
        readonly,
        onSelect: onSelect ?? ((record: DataRecord) => handleSelect([record])),
      });
    },
    [formView, target, title, showEditor, showEditorInTab, handleSelect]
  );

  const handleView = useCallback(
    (e: MouseEvent<HTMLAnchorElement>, value: DataRecord, readonly = true) => {
      e.preventDefault();
      return handleEdit(value, readonly);
    },
    [handleEdit]
  );

  const [beforeSelect, beforeSelectProps] = useBeforeSelect(schema);

  const handleCompletion = useCallback(
    async (value: string) => {
      const _domain = (await beforeSelect(true)) ?? domain;
      const _domainContext = _domain ? getContext() : {};
      const options = {
        _domain,
        _domainContext,
      };
      const { records, page } = await search(value, options);
      setHasMore((page.totalCount ?? 0) > records.length);
      return records;
    },
    [search, domain, beforeSelect, getContext]
  );

  const handleSearch = useCallback(async () => {
    const _domain = (await beforeSelect(true)) ?? domain;
    const _domainContext = _domain ? getContext() : {};
    showSelector({
      model: target,
      viewName: gridView,
      domain: _domain,
      context: _domainContext,
      limit: searchLimit,
      onSelect: handleSelect,
    });
  }, [
    beforeSelect,
    domain,
    getContext,
    showSelector,
    target,
    gridView,
    searchLimit,
    handleSelect,
  ]);

  const handleChange = useCallback(
    (value: any[]) =>
      setValue(
        value?.map?.((v) => ({
          id: v.id,
          [targetName]: v[targetName],
        })),
        true
      ),
    [setValue, targetName]
  );

  const getOptionLabel = useCallback(
    (option: any) => {
      const trKey = `$t:${targetName}`;
      return option[trKey] ?? option[targetName];
    },
    [targetName]
  );

  return (
    <FieldControl {...props}>
      {readonly ? (
        <Box d="flex" flexWrap="wrap">
          {(value || []).map((val) => (
            <Box
              m={1}
              key={val?.id}
              as="a"
              href="#"
              onClick={(e: any) => handleView(e, val)}
            >
              <Chip title={getOptionLabel(val)} color={"indigo"} />
            </Box>
          ))}
        </Box>
      ) : (
        <TagSelectComponent
          {...(focus && { key: "focused" })}
          autoFocus={focus}
          schema={schema}
          placeholder={placeholder}
          value={value}
          canCreate={canNew}
          fetchOptions={handleCompletion}
          {...beforeSelectProps}
          {...selectProps}
          canSearch={hasMore}
          onSearch={handleSearch}
          onView={handleView}
          onCreate={handleEdit as CreatableSelectProps["onCreate"]}
          onChange={handleChange}
          optionLabel={getOptionLabel}
        />
      )}
    </FieldControl>
  );
}
