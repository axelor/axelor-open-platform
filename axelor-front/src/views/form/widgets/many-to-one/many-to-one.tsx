import { useAtom, useAtomValue } from "jotai";
import getObjValue from "lodash/get";
import setObjValue from "lodash/set";
import isEqual from "lodash/isEqual";
import { useCallback, useMemo, useRef, useState } from "react";
import { useAtomCallback } from "jotai/utils";

import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { Select, SelectIcon, SelectValue } from "@/components/select";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { usePermitted } from "@/hooks/use-permitted";
import {
  useBeforeSelect,
  useCompletion,
  useCreateOnTheFly,
  useEditor,
  useEditorInTab,
  useSelector,
} from "@/hooks/use-relation";
import { DataSource } from "@/services/client/data";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { toKebabCase } from "@/utils/names";
import { useViewMeta } from "@/view-containers/views/scope";
import { usePermission, usePrepareContext } from "../../builder/form";
import { FieldControl } from "../../builder/form-field";
import { useFormRefresh } from "../../builder/scope";
import { FieldProps } from "../../builder/types";
import { removeVersion } from "../../builder/utils";
import { ViewerInput, ViewerLink } from "../string/viewer";
import { useOptionLabel } from "./utils";

export function ManyToOne(props: FieldProps<DataRecord>) {
  const {
    schema,
    formAtom,
    valueAtom,
    widgetAtom,
    readonly: _readonly,
    invalid,
  } = props;
  const {
    target,
    targetName,
    targetSearch,
    canSuggest = true,
    widget,
    placeholder,
    orderBy: sortBy,
    formView,
    gridView,
    limit,
    searchLimit,
    perms,
  } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const [hasSearchMore, setSearchMore] = useState(false);
  const { hasButton } = usePermission(schema, widgetAtom, perms);
  const { attrs } = useAtomValue(widgetAtom);
  const { title, focus, required, domain, hidden } = attrs;

  const isSuggestBox = toKebabCase(widget) === "suggest-box";

  const getContext = usePrepareContext(formAtom);
  const showSelector = useSelector();
  const showEditor = useEditor();
  const showEditorInTab = useEditorInTab(schema);
  const showCreator = useCreateOnTheFly(schema);

  const search = useCompletion({
    sortBy,
    limit,
    target,
    targetName,
    targetSearch,
  });

  const handleChange = useCallback(
    (changedValue: SelectValue<DataRecord, false>) => {
      if (changedValue && changedValue.id && changedValue.id > 0) {
        const rec = removeVersion(changedValue);
        setValue(rec, true, rec.id !== value?.id);
      } else {
        setValue(changedValue, true);
      }
    },
    [setValue, value],
  );

  const dataSource = useMemo(() => new DataSource(target), [target]);

  const canRead = perms?.read !== false;
  const canView = value && hasButton("view");
  const canEdit = value && hasButton("edit") && attrs.canEdit;
  const canNew = hasButton("new") && attrs.canNew;
  const canSelect = hasButton("select");
  const isRefLink = schema.widget === "ref-link";
  const readonly = _readonly || !canRead;

  const { findItems: findFormItems } = useViewMeta();

  const ensureRelated = useCallback(
    async (value: DataRecord, refetch?: boolean) => {
      if (value && value.id && value.id > 0 && canRead) {
        const name = schema.name;
        const prefix = name + ".";
        const items = findFormItems();
        const related = items
          .flatMap((item) => [item.name, item.depends?.split(",")])
          .flat()
          .filter(Boolean)
          .filter((name) => name.startsWith(prefix))
          .map((name) => name.substring(prefix.length));

        const names = [targetName, ...related].filter(Boolean);
        const missing = refetch
          ? names
          : names.filter((x) => getObjValue(value, x) === undefined);
        if (missing.length > 0) {
          try {
            const rec = await dataSource.read(
              value.id,
              { fields: missing },
              true,
            );
            return { ...value, ...rec, version: undefined };
          } catch (er) {
            return { ...value, [targetName]: value[targetName] ?? value.id };
          }
        }
      }
      return value;
    },
    [findFormItems, canRead, schema.name, targetName, dataSource],
  );

  const isPermitted = usePermitted(target, perms);

  const handleEdit = useCallback(
    async (readonly = false, record?: DataContext) => {
      const $record = record ?? value;
      if (!(await isPermitted($record, readonly))) {
        return;
      }
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
      value,
      isPermitted,
      showEditorInTab,
      showEditor,
      title,
      target,
      formView,
      getContext,
      handleChange,
    ],
  );

  const handleView = useCallback(
    async (e: React.MouseEvent<HTMLButtonElement>) => {
      e.preventDefault();
      if (isRefLink && showEditorInTab && value?.id) {
        return showEditorInTab(value, true);
      }
      return handleEdit(true);
    },
    [isRefLink, value, handleEdit, showEditorInTab],
  );

  const showCreate = useCallback(
    (input: string, popup = true) =>
      showCreator({
        input,
        popup,
        onEdit: (record) => handleEdit(false, record),
        onSelect: handleChange,
      }),
    [handleEdit, handleChange, showCreator],
  );

  const showCreateAndSelect = useCallback(
    (input: string) => showCreate(input, false),
    [showCreate],
  );

  const [beforeSelect, { onMenuOpen, onMenuClose }] = useBeforeSelect(schema);

  const showSelect = useCallback(async () => {
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

  const fetchOptions = useCallback(
    async (text: string) => {
      const _domain = (await beforeSelect()) ?? domain;
      const _domainContext = _domain ? getContext() : {};
      const options = {
        _domain,
        _domainContext,
      };
      const { records, page } = await search(text, options);
      setSearchMore((page.totalCount ?? 0) > records.length);
      return records;
    },
    [beforeSelect, domain, getContext, search],
  );

  const valueRef = useRef<DataRecord>();

  const ensureRelatedValues = useAtomCallback(
    useCallback(
      async (get, set, signal?: AbortSignal, refetch?: boolean) => {
        if (valueRef.current === value && !refetch) return;
        if (value) {
          const newValue = await ensureRelated(value, refetch);
          if (!isEqual(newValue, value)) {
            valueRef.current = newValue;
            if (signal?.aborted) return;
            // related field of ref-select?
            if (schema.related) {
              setValue(newValue, false, false);
              return;
            }
            const { name } = schema;
            const state = get(formAtom);
            const record = { ...state.record };
            setObjValue(record, name, newValue);
            // updated reference dotted fields in record
            Object.keys(record).forEach((fieldName) => {
              if (fieldName.includes(".") && fieldName.startsWith(name)) {
                record[fieldName] = getObjValue(record, fieldName.split("."));
              }
            });
            set(formAtom, {
              ...state,
              record,
            });
          } else {
            valueRef.current = value;
          }
        }
      },
      [schema, formAtom, ensureRelated, value],
    ),
  );

  const onRefSelectRefresh = useCallback(() => {
    if (["ref-select", "ref-link"].includes(toKebabCase(schema.widget))) {
      valueRef.current = undefined;
      ensureRelatedValues(undefined, true);
    }
  }, [schema, ensureRelatedValues]);

  const getOptionKey = useCallback((option: DataRecord) => option.id!, []);
  const getOptionLabel = useOptionLabel(schema);
  const getOptionEqual = useCallback(
    (a: DataRecord, b: DataRecord) => a.id === b.id,
    [],
  );

  const getOptionMatch = useCallback(() => true, []);

  const icons: SelectIcon[] = useMemo(() => {
    const edit: SelectIcon = {
      icon: <MaterialIcon icon="edit" />,
      onClick: () => handleEdit(),
    };
    const view: SelectIcon = {
      icon: <MaterialIcon icon="description" />,
      onClick: () => handleEdit(true),
    };
    const add: SelectIcon = {
      icon: <MaterialIcon icon="add" />,
      onClick: () => handleEdit(false, { id: null }),
    };
    const find: SelectIcon = {
      icon: <MaterialIcon icon="search" />,
      onClick: showSelect,
    };

    const result: SelectIcon[] = [];

    if (canEdit && canView) result.push(edit);
    if (isSuggestBox) return result;
    if (!canEdit && canView) result.push(view);
    if (canNew) result.push(add);
    if (canSelect) result.push(find);

    return result;
  }, [
    canEdit,
    canNew,
    canSelect,
    canView,
    handleEdit,
    showSelect,
    isSuggestBox,
  ]);

  useAsyncEffect(ensureRelatedValues, [ensureRelatedValues]);

  // register form:refresh
  useFormRefresh(onRefSelectRefresh);

  if (hidden) {
    return null;
  }

  return (
    <FieldControl {...props}>
      {readonly &&
        (value && hasButton("view") ? (
          <ViewerLink onClick={handleView}>{getOptionLabel(value)}</ViewerLink>
        ) : (
          <ViewerInput value={getOptionLabel(value)} />
        ))}
      {readonly || (
        <Select
          autoFocus={focus}
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
          onOpen={onMenuOpen}
          onClose={onMenuClose}
          canCreateOnTheFly={canNew && schema.create}
          canShowNoResultOption={true}
          onShowCreate={canNew ? showCreate : undefined}
          onShowSelect={
            canSelect && !isSuggestBox && hasSearchMore ? showSelect : undefined
          }
          onShowCreateAndSelect={
            canNew && schema.create ? showCreateAndSelect : undefined
          }
          icons={icons}
          clearIcon={false}
          toggleIcon={isSuggestBox ? undefined : false}
        />
      )}
    </FieldControl>
  );
}
