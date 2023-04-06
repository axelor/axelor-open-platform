import { useAtom } from "jotai";
import { MouseEvent, useCallback } from "react";

import { Box, Select } from "@axelor/ui";

import { useCompletion, useEditor, useSelector } from "@/hooks/use-relation";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";

import { FieldContainer, FieldProps } from "../../builder";

export function ManyToOne(props: FieldProps<DataRecord>) {
  const { schema, valueAtom, readonly } = props;
  const { uid, title, target, targetName, targetSearch } = schema;

  const [value, setValue] = useAtom(valueAtom);

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

  const handleEdit = useCallback(async () => {
    showEditor({
      title: title ?? "",
      model: target,
      record: value,
      onSelect: (record) => {
        setValue(record);
      },
    });
  }, [setValue, showEditor, target, title, value]);

  const handleView = useCallback(
    (e: MouseEvent<HTMLAnchorElement>) => {
      e.preventDefault();
      return handleEdit();
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
      {title && <label htmlFor={uid}>{title}</label>}
      {readonly ? (
        value && (
          <Box as="a" href="#" onClick={handleView}>
            {value[targetName]}
          </Box>
        )
      ) : (
        <Select
          onChange={handleChange}
          value={value}
          icons={[
            {
              icon: "edit",
              onClick: handleEdit,
            },
            {
              icon: "search",
              onClick: handleSelect,
            },
          ]}
          fetchOptions={handleCompletion}
          optionLabel={targetName}
          optionValue={"id"}
        />
      )}
    </FieldContainer>
  );
}
