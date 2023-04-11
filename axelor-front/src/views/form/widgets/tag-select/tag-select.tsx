import { useAtom } from "jotai";
import { MouseEvent, useCallback, useMemo } from "react";
import { Box, Select } from "@axelor/ui";

import { useCompletion, useEditor } from "@/hooks/use-relation";
import { DataRecord } from "@/services/client/data.types";

import { FieldContainer, FieldProps } from "../../builder";
import { Chip } from "../selection";

export function TagSelect(props: FieldProps<DataRecord[]>) {
  const { schema, valueAtom, readonly } = props;
  const { uid, title, target, targetName, targetSearch } = schema;

  const [value, setValue] = useAtom(valueAtom);

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
    async (value: DataRecord, readonly = false) => {
      showEditor({
        title: title ?? "",
        model: target,
        record: value,
        readonly,
      });
    },
    [showEditor, target, title]
  );

  const handleView = useCallback(
    (e: MouseEvent<HTMLAnchorElement>, value: DataRecord) => {
      e.preventDefault();
      return handleEdit(value, true);
    },
    [handleEdit]
  );

  const handleCompletion = useCallback(
    async (value: string) => {
      const res = await search(value);
      const { records } = res;
      return records;
    },
    [search]
  );

  const components = useMemo(
    () => ({
      MultiValue: (props: any) => {
        const { data, removeProps } = props;
        return (
          <Box me={1}>
            <Chip
              color={"indigo"}
              title={data?.[targetName]}
              onRemove={removeProps.onClick}
            />
          </Box>
        );
      },
    }),
    [targetName]
  );

  return (
    <FieldContainer>
      {title && <label htmlFor={uid}>{title}</label>}
      {readonly ? (
        <Box d="flex">
          {(value || []).map((val) => (
            <Box
              me={1}
              key={val?.id}
              as="a"
              href="#"
              onClick={(e) => handleView(e, val)}
            >
              <Chip title={val[targetName]} color={"indigo"} />
            </Box>
          ))}
        </Box>
      ) : (
        <Select
          isMulti
          onChange={handleChange}
          value={value}
          fetchOptions={handleCompletion}
          optionLabel={targetName}
          optionValue={"id"}
          components={components}
        />
      )}
    </FieldContainer>
  );
}
