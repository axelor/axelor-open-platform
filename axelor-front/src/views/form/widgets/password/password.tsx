import { useAtomValue } from "jotai";
import { useCallback, useState } from "react";

import { Button } from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";

import { i18n } from "@/services/client/i18n";
import { FieldProps } from "../../builder";
import { String } from "../string";

export function Password(props: Readonly<FieldProps<string>>) {
  const { readonly, valueAtom } = props;
  const value = useAtomValue(valueAtom);

  const [showPassword, setShowPassword] = useState(false);

  const toggleShowPassword = useCallback(() => {
    setShowPassword((show) => !show);
  }, []);

  return (
    <String
      {...props}
      inputProps={{
        type: showPassword ? "text" : "password",
        autoComplete: "new-password",
      }}
      inputEndAdornment={
        !readonly || value ? (
          <ShowPasswordButton
            showPassword={showPassword}
            toggleShowPassword={toggleShowPassword}
          />
        ) : undefined
      }
    />
  );
}

/**
 * Show button for toggling the visibility of a password input value
 */
function ShowPasswordButton({
  showPassword,
  toggleShowPassword,
}: Readonly<{ showPassword: boolean; toggleShowPassword: () => void }>) {
  return (
    <Button
      as="span"
      onClick={toggleShowPassword}
      title={showPassword ? i18n.get("Hide") : i18n.get("Show")}
    >
      <BootstrapIcon icon={showPassword ? "eye-slash" : "eye"} />
    </Button>
  );
}
