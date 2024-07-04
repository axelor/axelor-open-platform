import { useAtomValue } from "jotai";
import { useCallback, useEffect, useMemo, useState } from "react";

import { TextLink } from "@/components/text-link";
import { i18n } from "@/services/client/i18n";
import { Button } from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";

import { FieldControl, FieldProps } from "../../builder";
import { String } from "../string";
import { Validators } from "@/utils/validate";
import { Field } from "@/services/client/meta.types";

import styles from "./email.module.scss";

export function Email(props: FieldProps<string>) {
  const { schema, readonly, valueAtom } = props;
  const value = useAtomValue(valueAtom);
  const [email, setEmail] = useState(value);

  const onChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setEmail(e.target.value);
  }, []);

  const handleOpenEmailLink = useCallback(() => {
    window.location.href = `mailto:${email}`;
  }, [email]);

  // to live validate email on input change
  // instead of on input blur through invalid prop
  const canOpenEmail = useMemo(
    () =>
      email &&
      !Validators.string(email, {
        props: schema as Field,
        context: { [schema.name]: email },
      }),
    [email, schema],
  );

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
          disabled={!canOpenEmail}
          title={i18n.get("Write an email")}
        >
          <BootstrapIcon icon={"envelope"} />
        </Button>
      }
    />
  );
}
