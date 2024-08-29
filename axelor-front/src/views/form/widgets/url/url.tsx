import { useAtomValue } from "jotai";
import { useCallback, useEffect, useMemo, useState } from "react";

import { TextLink } from "@/components/text-link";
import { i18n } from "@/services/client/i18n";
import { Button } from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";

import { FieldControl, FieldProps } from "../../builder";
import { String } from "../string";
import { isValidUrl } from "./utils";

import styles from "./url.module.scss";

export function Url(props: FieldProps<string>) {
  const { readonly, valueAtom } = props;
  const value = useAtomValue(valueAtom);
  const [inputValue, setInputValue] = useState(value);

  const validUrl = useMemo(() => isValidUrl(inputValue), [inputValue]);

  const onChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
  }, []);

  const handleOpenUrl = useCallback(() => {
    window.open(inputValue as string, "_blank", "noopener,noreferrer");
  }, [inputValue]);

  useEffect(() => {
    setInputValue(value);
  }, [value]);

  if (readonly) {
    return (
      <FieldControl {...props}>
        {value && (
          <TextLink href={validUrl ? value : undefined} className={styles.link}>
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
          disabled={!validUrl}
          title={i18n.get("Open URL")}
        >
          <BootstrapIcon icon="globe" />
        </Button>
      }
    />
  );
}
