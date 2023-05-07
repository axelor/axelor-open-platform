import { useAtomValue } from "jotai";

import { Box } from "@axelor/ui";

import { String } from "../string";

import { FieldControl, FieldProps } from "../../builder";

export function Url(props: FieldProps<string>) {
  const { readonly, valueAtom } = props;
  const value = useAtomValue(valueAtom);
  if (readonly) {
    return (
      <FieldControl {...props}>
        {value && (
          <Box as="a" target="_blank" href={value}>
            {value}
          </Box>
        )}
      </FieldControl>
    );
  }
  return <String {...props} inputProps={{ type: "url" }} />;
}
