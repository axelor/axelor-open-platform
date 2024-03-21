import { useAtomValue } from "jotai";
import { useCallback, useEffect, useMemo, useState } from "react";

import { TextLink } from "@/components/text-link";
import { i18n } from "@/services/client/i18n";
import { Button } from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";

import { FieldControl, FieldProps } from "../../builder";
import { String } from "../string";

import styles from "./email.module.scss";

export function Email(props: FieldProps<string>) {
  const { readonly, valueAtom, schema } = props;
  const { pattern } = schema;
  const value = useAtomValue(valueAtom);
  const [email, setEmail] = useState(value);

  const regexp = useMemo(() => new RegExp(pattern ?? "", "i"), [pattern]);

  const isValidEmail = useMemo(() => {
    return email && regexp.test(email);
  }, [email, regexp]);

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
          <TextLink href={`mailto:${value}`} className={styles.link}>
            {value}
          </TextLink>
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
          title={i18n.get("Write an email")}
        >
          <BootstrapIcon icon={"envelope"} />
        </Button>
      }
    />
  );
}
