import { useCallback, useId, useMemo } from "react";

import { Select, SelectValue } from "@/components/select";
import { useAsync } from "@/hooks/use-async";
import { request } from "@/services/client/client";

import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import { ViewerInput } from "../string/viewer";

type ThemeOption = {
  name: string;
  title: string;
};

export function ThemeSelect(props: FieldProps<string>) {
  const { data: themes = [] } = useAsync(async () => {
    const res = await request({
      url: "ws/app/themes",
    });
    if (res.ok) {
      return await res.json() as ThemeOption[];
    }
    return Promise.reject();
  }, []);

  const { schema, readonly, valueAtom } = props;
  const { placeholder } = schema;

  const id = useId();
  const { value, setValue } = useInput(valueAtom, { defaultValue: "", schema });

  const text = useMemo(
    () => themes.find((x) => x.name === value)?.title ?? "",
    [themes, value],
  );
  const selected = useMemo(
    () => themes.find((x) => x.name === value) ?? null,
    [themes, value],
  );

  const handleChange = useCallback(
    (option: SelectValue<ThemeOption, false>) => {
      setValue(option?.name ?? null, true);
    },
    [setValue],
  );

  return (
    <FieldControl {...props} inputId={id}>
      {readonly && <ViewerInput id={id} name={schema.name} value={text} />}
      {readonly || (
        <Select
          id={id}
          autoComplete={false}
          placeholder={placeholder}
          onChange={handleChange}
          value={selected}
          options={themes}
          optionKey={(x) => x.name}
          optionLabel={(x) => x.title}
          optionEqual={(x, y) => x.name === y.name}
        />
      )}
    </FieldControl>
  );
}
