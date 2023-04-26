import { useAtom, useAtomValue } from "jotai";
import { MouseEvent, useCallback } from "react";

import { Select } from "@axelor/ui";

import { usePerms } from "@/hooks/use-perms";
import { useCompletion, useEditor, useSelector } from "@/hooks/use-relation";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { toKebabCase } from "@/utils/names";

import { useAtomCallback } from "jotai/utils";
import { FieldContainer, FieldProps } from "../../builder";
import { ViewerInput, ViewerLink } from "../string";

export function ManyToOne(props: FieldProps<DataRecord>) {
  const { schema, formAtom, valueAtom, widgetAtom, readonly } = props;
  const {
    uid,
    domain,
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
    attrs: { title },
  } = useAtomValue(widgetAtom);

  const isSuggestBox = toKebabCase(widget) === "suggest-box";
  const showSelector = useSelector();
  const showEditor = useEditor();

  const search = useCompletion({
    target,
    targetName,
    targetSearch,
  });

  const handleChange = (value: any) => {
    setValue(value);
  };

  const canView = value && hasButton("view");
  const canEdit = value && hasButton("edit") && schema.canEdit === true;
  const canNew = hasButton("new") && schema.canNew === true;
  const canSelect = hasButton("select");

  const handleEdit = useCallback(
    async (readonly = false, record?: DataContext) => {
      showEditor({
        title: title ?? "",
        model: target,
        viewName: formView,
        record: record ?? value,
        readonly,
        onSelect: (record) => {
          setValue(record);
        },
      });
    },
    [setValue, showEditor, target, title, formView, value]
  );

  const handleView = useCallback(
    (e: MouseEvent<HTMLButtonElement>) => {
      e.preventDefault();
      return handleEdit(true);
    },
    [handleEdit]
  );

  const handleSelect = useCallback(() => {
    showSelector({
      title: i18n.get("Select {0}", title ?? ""),
      model: target,
      viewName: gridView,
      multiple: false,
      onSelect: (records) => {
        setValue(records[0]);
      },
    });
  }, [setValue, showSelector, target, title, gridView]);

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
        <Select
          onChange={handleChange}
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
