import { useAtom, useAtomValue } from "jotai";
import { useCallback } from "react";

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

import {
  FieldControl,
  FieldProps,
  usePermission,
  usePrepareContext,
} from "../../builder";
import { SelectionTag } from "../selection";

import styles from "./tag-select.module.scss";

export function TagSelect(props: FieldProps<DataRecord[]>) {
  const { schema, formAtom, valueAtom, widgetAtom, readonly, invalid } = props;
  const {
    target,
    targetName,
    targetSearch,
    canSuggest = true,
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
    (value: SelectValue<DataRecord, true>) => {
      if (Array.isArray(value)) {
        const items = value.map(({ version: _, ...rest }) => rest);
        setValue(items.length === 0 ? null : items, true);
      } else {
        setValue(value, true);
      }
    },
    [setValue],
  );

  const handleSelect = useCallback(
    (record: DataRecord) => {
      const all = Array.isArray(value) ? value : [];
      const next = all.find((x) => x.id === record.id) ? all : [...all, record];
      handleChange(next);
    },
    [handleChange, value],
  );

  const canNew = hasButton("new");
  const canView = readonly && hasButton("view");
  const canEdit = !readonly && hasButton("edit") && attrs.canEdit !== false;
  const canSelect = hasButton("select");
  const canRemove = !readonly && attrs.canRemove !== false;

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

  const renderValue = useCallback(
    ({ option }: SelectOptionProps<DataRecord>) => {
      return (
        <Tag
          record={option}
          targetName={targetName}
          onClick={canView || canEdit ? handleEdit : undefined}
          onRemove={canRemove ? handleRemove : undefined}
        />
      );
    },
    [canEdit, canRemove, canView, handleEdit, handleRemove, targetName],
  );

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
      />
    </FieldControl>
  );
}

type TagProps = {
  record: DataRecord;
  targetName: string;
  onRemove?: (record: DataRecord) => void;
  onClick?: (record: DataRecord) => void;
};

function Tag(props: TagProps) {
  const { record, targetName, onClick, onRemove } = props;

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
            {record[targetName]}
          </span>
        ) : (
          <span>{record[targetName]}</span>
        )
      }
      color="primary"
      onRemove={canRemove ? handleRemove : undefined}
    />
  );
}
