import { useAtom, useAtomValue } from "jotai";
import { MouseEvent, useCallback } from "react";

import { Box, Select } from "@axelor/ui";

import { useCompletion, useEditor, useSelector } from "@/hooks/use-relation";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";

import { toKebabCase } from "@/utils/names";
import { FieldContainer, FieldProps } from "../../builder";

export function ManyToOne(props: FieldProps<DataRecord>) {
  const { schema, valueAtom, widgetAtom, readonly } = props;
  const {
    uid,
    target,
    targetName,
    targetSearch,
    widget,
    placeholder,
    showTitle = true,
  } = schema;

  const [value, setValue] = useAtom(valueAtom);
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

  const handleEdit = useCallback(
    async (readonly = false) => {
      showEditor({
        title: title ?? "",
        model: target,
        record: value,
        readonly,
        onSelect: (record) => {
          setValue(record);
        },
      });
    },
    [setValue, showEditor, target, title, value]
  );

  const handleView = useCallback(
    (e: MouseEvent<HTMLAnchorElement>) => {
      e.preventDefault();
      return handleEdit(true);
    },
    [handleEdit]
  );

  const handleSelect = useCallback(() => {
    showSelector({
      title: i18n.get("Select {0}", title ?? ""),
      model: target,
      multiple: false,
      onSelect: (records) => {
        setValue(records[0]);
      },
    });
  }, [setValue, showSelector, target, title]);

  const handleCompletion = useCallback(
    async (value: string) => {
      const res = await search(value);
      const { records } = res;
      return records;
    },
    [search]
  );

  return (
    <FieldContainer>
      {showTitle && <label htmlFor={uid}>{title}</label>}
      {readonly ? (
        value && (
          <Box as="a" href="#" onClick={handleView}>
            {value[targetName]}
          </Box>
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
                    icon: "edit",
                    onClick: () => handleEdit(),
                  },
                  {
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
