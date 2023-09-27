import { useAtom, useAtomValue } from "jotai";
import getObjValue from "lodash/get";
import isEqual from "lodash/isEqual";
import { useCallback, useMemo, useRef } from "react";

import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { Select, SelectIcon, SelectValue } from "@/components/select";
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

import { useViewMeta } from "@/view-containers/views/scope";
import { usePermission, usePrepareContext } from "../../builder/form";
import { FieldControl } from "../../builder/form-field";
import { useFormRefresh } from "../../builder/scope";
import { FieldProps } from "../../builder/types";
import { ViewerInput, ViewerLink } from "../string/viewer";
import { useOptionLabel } from "./utils";

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

  const { attrs } = useAtomValue(widgetAtom);
  const { title, focus, required, domain } = attrs;

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
    (value: SelectValue<DataRecord, false>) => {
      if (value && value.id && value.id > 0) {
        const { version, ...rec } = value;
        setValue(rec, true);
      } else {
        setValue(value, true);
      }
    },
    [setValue],
  );

  const canView = value && hasButton("view");
  const canEdit = value && hasButton("edit") && attrs.canEdit;
  const canNew = hasButton("new") && attrs.canNew;
  const canSelect = hasButton("select");
  const isRefLink = schema.widget === "ref-link";

  const { findItems: findFormItems } = useViewMeta();

  const ensureRelated = useCallback(
    async (value: DataRecord, refetch?: boolean) => {
      if (value && value.id && value.id > 0) {
        const name = schema.name;
        const prefix = name + ".";
        const items = findFormItems();
        const related = items
          .flatMap((item) => [item.name, item.depends?.split(",")])
          .flat()
          .filter(Boolean)
          .filter((name) => name.startsWith(prefix))
          .map((name) => name.substring(prefix.length));

        const names = [targetName, ...related];
        const missing = refetch
          ? names
          : names.filter((x) => getObjValue(value, x) === undefined);
        if (missing.length > 0) {
          try {
            const ds = new DataSource(target);
            const rec = await ds.read(value.id, { fields: missing }, true);
            return { ...value, ...rec, version: undefined };
          } catch (er) {
            return { ...value, [targetName]: value.id };
          }
        }
      }
      return value;
    },
    [findFormItems, schema.name, target, targetName],
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
    (inputValue: string) => {
      const record: DataRecord = {
        [targetName]: inputValue,
      };
      return handleEdit(false, record);
    },
    [handleEdit, targetName],
  );

  const [beforeSelect, beforeSelectProps] = useBeforeSelect(schema);

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

  const valueRef = useRef<DataRecord>();

  const ensureRelatedValues = useCallback(
    async (signal?: AbortSignal, refetch?: boolean) => {
      if (valueRef.current === value && !refetch) return;
      if (value) {
        const newValue = await ensureRelated(value, refetch);
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
    if (canEdit || canView) result.push(view);
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

  return (
    <FieldControl {...props}>
      {readonly &&
        (value && hasButton("view") ? (
          <ViewerLink onClick={handleView}>{getOptionLabel(value)}</ViewerLink>
        ) : (
          <ViewerInput value={value?.[targetName] || ""} />
        ))}
      {readonly || (
        <Select
          autoFocus={focus}
          required={required}
          invalid={invalid}
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
          onShowCreate={canNew ? showCreate : undefined}
          onShowSelect={canSelect && !isSuggestBox ? showSelect : undefined}
          icons={icons}
          clearIcon={false}
          toggleIcon={isSuggestBox ? undefined : false}
        />
      )}
    </FieldControl>
  );
}
