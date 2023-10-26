import { PrimitiveAtom, useAtom, useAtomValue } from "jotai";
import { focusAtom } from "jotai-optics";
import { useCallback, useEffect, useMemo, useRef } from "react";
import isEqual from "lodash/isEqual";
import getObjValue from "lodash/get";

import { Select, SelectOptionProps, SelectValue } from "@/components/select";
import {
  useBeforeSelect,
  useCompletion,
  useCreateOnTheFly,
  useEditor,
  useEditorInTab,
  useSelector,
} from "@/hooks/use-relation";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { DataSource } from "@/services/client/data";
import { useAsyncEffect } from "@/hooks/use-async-effect";

import { toKebabCase } from "@/utils/names";
import {
  FieldControl,
  FieldProps,
  usePermission,
  usePrepareContext,
} from "../../builder";
import { removeVersion } from "../../builder/utils";
import { SelectionTag } from "../selection";

import styles from "./tag-select.module.scss";

export function TagSelect(props: FieldProps<DataRecord[]>) {
  const { schema, formAtom, valueAtom, widgetAtom, readonly, invalid } = props;
  const {
    target,
    targetName,
    targetSearch: _targetSearch,
    colorField,
    canSuggest = true,
    placeholder,
    orderBy: sortBy,
    formView,
    gridView,
    limit,
    searchLimit,
  } = schema;

  const isManyToMany =
    toKebabCase(schema.serverType || schema.widget) === "many-to-many";

  const [value, setValue] = useAtom(valueAtom);
  const { hasButton } = usePermission(schema, widgetAtom);

  const valueRef = useRef<DataRecord[]>();
  const { attrs } = useAtomValue(widgetAtom);
  const { title, focus, required, domain } = attrs;

  const getContext = usePrepareContext(formAtom);
  const showSelector = useSelector();
  const showEditor = useEditor();
  const showEditorInTab = useEditorInTab(schema);
  const showCreator = useCreateOnTheFly(schema);
  const targetSearch = useMemo<string[]>(
    () => [...(_targetSearch || [])].concat(colorField ? [colorField] : []),
    [_targetSearch, colorField],
  );

  const search = useCompletion({
    sortBy,
    limit,
    target,
    targetName,
    targetSearch,
  });

  const [originalField, setOriginalField] = useAtom(
    useMemo(
      () =>
        focusAtom(formAtom, (o) =>
          o.prop("original").optional().prop(schema.name),
        ),
      [formAtom, schema.name],
    ) as PrimitiveAtom<DataRecord[] | undefined>,
  );

  const originalFieldRef = useRef<DataRecord[]>();

  // This widget manages saving independently from main record
  useEffect(() => {
    if (originalField === originalFieldRef.current) return;
    setOriginalField((values) => {
      if (Array.isArray(values)) {
        values = values.map(removeVersion);
      }
      originalFieldRef.current = values;
      return values;
    });
  }, [originalField, setOriginalField]);

  const handleChange = useCallback(
    (changedValue: SelectValue<DataRecord, true>) => {
      if (Array.isArray(changedValue)) {
        const items = changedValue.map(removeVersion);
        const prev = value ?? [];
        const markDirty =
          !isManyToMany ||
          prev.length !== items.length ||
          items.some((item, index) => item.id !== prev[index].id);
        setValue(items.length === 0 ? null : items, true, markDirty);
      } else {
        setValue(changedValue, true);
      }
    },
    [isManyToMany, setValue, value],
  );

  const handleSelect = useCallback(
    (record: DataRecord) => {
      const next = Array.isArray(value) ? [...value] : [];
      const index = next.findIndex((x) => x.id === record.id);
      if (index >= 0) {
        const found = next[index];
        next[index] = { ...found, ...record };
      } else {
        next.push(record);
      }
      handleChange(next);
    },
    [handleChange, value],
  );

  const canNew = hasButton("new");
  const canView = readonly && hasButton("view");
  const canEdit = !readonly && hasButton("edit") && attrs.canEdit !== false;
  const canSelect = hasButton("select");
  const canRemove = !readonly && attrs.canRemove !== false;

  const ensureRelated = useCallback(
    async (value: DataRecord[]) => {
      const names = [targetName, colorField].filter((s) => Boolean(s));
      const ids = value
        .filter((v) => names.some((name) => getObjValue(v, name) === undefined))
        .map((v) => v.id);

      if (ids.length > 0) {
        let records: DataRecord[] = [];
        try {
          const ds = new DataSource(target);
          records = await ds
            .search({
              fields: [targetName],
              filter: {
                _domain: "self.id in (:_ids)",
                _domainContext: {
                  _ids: ids as number[],
                },
              },
            })
            .then((res) => res.records);
        } catch (er) {
          records = ids.map((id) => ({ id, [targetName]: id }));
          //
        }
        const newValue = value.map((v) => {
          const rec = records.find((r) => r.id === v.id);
          return rec ? rec : v;
        });
        return newValue;
      }
      return value;
    },
    [target, targetName, colorField],
  );

  const ensureRelatedValues = useCallback(
    async (signal?: AbortSignal) => {
      if (valueRef.current === value) return;
      if (value) {
        const newValue = await ensureRelated(value);
        if (!isEqual(newValue, value)) {
          valueRef.current = newValue;
          if (signal?.aborted) return;
          setValue(newValue, false, false);
        } else {
          valueRef.current = value;
        }
      }
    },
    [ensureRelated, setValue, value],
  );

  const handleEdit = useCallback(
    async (record?: DataContext) => {
      if (showEditorInTab && (record?.id ?? 0) > 0) {
        return showEditorInTab(record!, readonly);
      }
      showEditor({
        title: title ?? "",
        model: target,
        viewName: formView,
        record,
        readonly,
        context: {
          _parent: getContext(),
        },
        onSelect: handleSelect,
      });
    },
    [
      showEditorInTab,
      showEditor,
      title,
      target,
      formView,
      readonly,
      getContext,
      handleSelect,
    ],
  );

  const handleRemove = useCallback(
    (record: DataRecord) => {
      if (Array.isArray(value)) {
        handleChange(value.filter((x) => x.id !== record.id));
      }
    },
    [handleChange, value],
  );

  const [beforeSelect, beforeSelectProps] = useBeforeSelect(schema);

  const showSelect = useCallback(async () => {
    const _domain = (await beforeSelect(true)) ?? domain;
    const _domainContext = _domain ? getContext() : {};
    showSelector({
      model: target,
      viewName: gridView,
      orderBy: sortBy,
      multiple: true,
      domain: _domain,
      context: _domainContext,
      limit: searchLimit,
      onSelect: async (records = []) => {
        const all = Array.isArray(value) ? value : [];
        const add = records.filter((x) => !all.some((a) => a.id === x.id));
        handleChange([...all, ...add]);
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
    value,
    handleChange,
  ]);

  const showCreate = useCallback(
    (input: string, popup = true) =>
      showCreator({
        input,
        popup,
        onEdit: handleEdit,
        onSelect: handleSelect,
      }),
    [handleEdit, handleSelect, showCreator],
  );

  const showCreateAndSelect = useCallback(
    (input: string) => showCreate(input, false),
    [showCreate],
  );

  const fetchOptions = useCallback(
    async (text: string) => {
      const _domain = (await beforeSelect()) ?? domain;
      const _domainContext = _domain ? getContext() : {};
      const options = {
        _domain,
        _domainContext,
      };
      const { records } = await search(text, options);
      return records;
    },
    [beforeSelect, domain, getContext, search],
  );

  const handleOpen = useCallback(async () => {
    beforeSelectProps?.onMenuOpen?.();
  }, [beforeSelectProps]);

  const handleClose = useCallback(() => {
    beforeSelectProps?.onMenuClose?.();
  }, [beforeSelectProps]);

  const getOptionKey = useCallback((option: DataRecord) => option.id!, []);
  const getOptionLabel = useCallback(
    (option: DataRecord) => {
      const trKey = `$t:${targetName}`;
      return option[trKey] ?? option[targetName];
    },
    [targetName],
  );
  const getOptionEqual = useCallback(
    (a: DataRecord, b: DataRecord) => a.id === b.id,
    [],
  );

  const getOptionMatch = useCallback(() => true, []);

  const renderOption = useCallback(
    ({ option }: SelectOptionProps<DataRecord>) => {
      return (
        <Tag
          record={option}
          colorField={colorField}
          optionLabel={getOptionLabel}
        />
      );
    },
    [colorField, getOptionLabel],
  );

  const renderValue = useCallback(
    ({ option }: SelectOptionProps<DataRecord>) => {
      return (
        <Tag
          record={option}
          colorField={colorField}
          optionLabel={getOptionLabel}
          onClick={canView || canEdit ? handleEdit : undefined}
          onRemove={canRemove ? handleRemove : undefined}
        />
      );
    },
    [
      canEdit,
      canRemove,
      canView,
      colorField,
      getOptionLabel,
      handleEdit,
      handleRemove,
    ],
  );

  useAsyncEffect(ensureRelatedValues, [ensureRelatedValues]);

  return (
    <FieldControl {...props}>
      <Select
        className={styles.select}
        autoFocus={focus}
        multiple={true}
        readOnly={readonly}
        required={required}
        invalid={invalid}
        canSelect={canSelect}
        autoComplete={canSuggest}
        fetchOptions={fetchOptions}
        options={[] as DataRecord[]}
        optionKey={getOptionKey}
        optionLabel={getOptionLabel}
        optionEqual={getOptionEqual}
        optionMatch={getOptionMatch}
        value={value}
        placeholder={placeholder}
        onChange={handleChange}
        onOpen={handleOpen}
        onClose={handleClose}
        canCreateOnTheFly={canNew && schema.create}
        onShowCreate={canNew ? showCreate : undefined}
        onShowCreateAndSelect={
          canNew && schema.create ? showCreateAndSelect : undefined
        }
        onShowSelect={canSelect ? showSelect : undefined}
        clearIcon={false}
        renderValue={renderValue}
        renderOption={renderOption}
      />
    </FieldControl>
  );
}

type TagProps = {
  record: DataRecord;
  colorField?: string;
  optionLabel: (record: DataRecord) => string;
  onRemove?: (record: DataRecord) => void;
  onClick?: (record: DataRecord) => void;
};

function Tag(props: TagProps) {
  const { record, colorField, optionLabel, onClick, onRemove } = props;

  const handleClick = useCallback(
    (event: React.MouseEvent<HTMLButtonElement>) => {
      event.preventDefault();
      onClick?.(record);
    },
    [onClick, record],
  );

  const handleRemove = useCallback(() => {
    onRemove?.(record);
  }, [onRemove, record]);

  const canOpen = Boolean(onClick);
  const canRemove = Boolean(onRemove);

  return (
    <SelectionTag
      title={
        canOpen ? (
          <span className={styles.tagLink} onClick={handleClick}>
            {optionLabel(record)}
          </span>
        ) : (
          <span>{optionLabel(record)}</span>
        )
      }
      color={(colorField ? record[colorField] : "") || "primary"}
      onRemove={canRemove ? handleRemove : undefined}
    />
  );
}
