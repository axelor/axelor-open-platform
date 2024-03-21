import { useState } from "react";

import { Button } from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";

import { i18n } from "@/services/client/i18n";
import { FieldProps } from "../../builder";
import { String } from "../string";

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
