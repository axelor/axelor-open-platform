import { useCallback, useEffect, useMemo, useState } from "react";
import { useAtomValue } from "jotai";

import { Box, Button } from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";
import { i18n } from "@/services/client/i18n";

import { String } from "../string";
import { FieldControl, FieldProps } from "../../builder";

import styles from "./url.module.scss";

export function Url(props: FieldProps<string>) {
  const { readonly, valueAtom } = props;
  const value = useAtomValue(valueAtom);
  const [url, setUrl] = useState(value);

  const isValidUrl = useMemo(() => {
    try {
      new URL(url ?? "");
      return true;
    } catch (err) {
      return false;
    }
  }, [url]);

  const onChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setUrl(e.target.value);
  }, []);

  const handleOpenUrl = useCallback(() => {
    window.open(url as string, "_blank");
  }, [url]);

  useEffect(() => {
    setUrl(value);
  }, [value]);

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
  return (
    <String
      {...props}
      inputProps={{ type: "url" }}
      onChange={onChange}
      inputEndAdornment={
        <Button
          onClick={handleOpenUrl}
          className={styles.inputIcon}
          disabled={!isValidUrl}
          title={i18n.get("Open URL")}
        >
          <BootstrapIcon icon="globe" />
        </Button>
      }
    />
  );
}
