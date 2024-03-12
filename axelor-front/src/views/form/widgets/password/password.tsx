import { useState } from "react";

import { Box, Button } from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";
import { i18n } from "@/services/client/i18n";

import { String } from "../string";
import { FieldProps } from "../../builder";

import styles from "./password.module.scss";

export function Password(props: FieldProps<string>) {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <String
      {...props}
      inputProps={{
        type: showPassword ? "text" : "password",
        autoComplete: "new-password",
      }}
      inputEndAdornment={
        <Button
          as="span"
          onClick={() => setShowPassword((value) => !value)}
          className={styles.eyeIcon}
          title={
            showPassword ? i18n.get("Hide password") : i18n.get("Show password")
          }
        >
          <BootstrapIcon icon={showPassword ? "eye-slash" : "eye"} />
        </Button>
      }
    />
  );
}
