import { useAtomValue } from "jotai";
import { useCallback, useEffect, useMemo, useState } from "react";

import { TextLink } from "@/components/text-link";
import { i18n } from "@/services/client/i18n";
import { Button } from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";

import { FieldControl, FieldProps } from "../../builder";
import { String } from "../string";

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
    window.open(url as string, "_blank", "noopener,noreferrer");
  }, [url]);

  useEffect(() => {
    setUrl(value);
  }, [value]);

  if (readonly) {
    return (
      <FieldControl {...props}>
        {value && (
          <TextLink href={value} className={styles.link}>
            {value}
          </TextLink>
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
          disabled={!isValidUrl}
          title={i18n.get("Open URL")}
        >
          <BootstrapIcon icon="globe" />
        </Button>
      }
    />
  );
}
