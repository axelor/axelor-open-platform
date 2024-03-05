import { useCallback, useEffect, useMemo, useState } from "react";
import { useAtomValue } from "jotai";

import { Box, Button } from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";
import { i18n } from "@/services/client/i18n";

import { FieldControl, FieldProps } from "../../builder";
import { String } from "../string";

import styles from "./email.module.scss";

export function Email(props: FieldProps<string>) {
  const { readonly, valueAtom } = props;
  const value = useAtomValue(valueAtom);
  const [email, setEmail] = useState(value);

  const isValidEmail = useMemo(() => {
    return !!email?.match(/^[\w-.]+@([\w-]+\.)+[\w-]{2,}$/g);
  }, [email]);

  const onChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setEmail(e.target.value);
  }, []);

  const handleOpenEmailLink = useCallback(() => {
    window.location.href = `mailto:${email}`;
  }, [email]);

  useEffect(() => {
    setEmail(value);
  }, [value]);

  if (readonly) {
    return (
      <FieldControl {...props}>
        {value && (
          <Box
            as="a"
            target="_blank"
            href={`mailto:${value}`}
            className={styles.link}
          >
            {value}
          </Box>
        )}
      </FieldControl>
    );
  }
  return (
    <String
      {...props}
      inputProps={{ type: "email" }}
      onChange={onChange}
      inputEndAdornment={
        <Button
          onClick={handleOpenEmailLink}
          disabled={!isValidEmail}
          className={styles.inputIcon}
          title={i18n.get("Send email")}
        >
          <BootstrapIcon icon={"envelope"} />
        </Button>
      }
    />
  );
}
