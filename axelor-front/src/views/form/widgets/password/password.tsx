import { useAtomValue } from "jotai";
import { useCallback, useEffect, useRef, useState } from "react";

import { Box, Button, Popper } from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";

import { alerts } from "@/components/alerts";
import { i18n } from "@/services/client/i18n";
import { FieldProps } from "../../builder";
import { String } from "../string";

import styles from "./password.module.scss";

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
        <PasswordEndAdornment
          readonly={readonly}
          value={value}
          showPassword={showPassword}
          toggleShowPassword={toggleShowPassword}
        />
      }
    />
  );
}

/**
 * End adornment for password input
 */
function PasswordEndAdornment({
  readonly,
  value,
  showPassword,
  toggleShowPassword,
}: Readonly<{
  readonly?: boolean;
  value?: string | null;
  showPassword: boolean;
  toggleShowPassword: () => void;
}>) {
  const hasValue = value?.length;

  // Show button: always visible if not readonly, or if readonly and has value
  // Copy button: only visible if readonly and has value
  return (
    <>
      {(!readonly || hasValue) && (
        <ShowPasswordButton
          showPassword={showPassword}
          toggleShowPassword={toggleShowPassword}
        />
      )}
      {readonly && hasValue && <CopyPasswordButton text={value} />}
    </>
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

/**
 * Copy button that copies a given text to the clipboard
 *
 * When the text is copied, a confirmation popper appears for a short duration.
 * Displays an error alert if clipboard action fails
 * Renders nothing if the clipboard API is not available.
 */
function CopyPasswordButton({ text }: Readonly<{ text: string }>) {
  const ref = useRef<HTMLElement>(null);
  const [copied, setCopied] = useState(false);

  const handleCopyPassword = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
    } catch (error) {
      alerts.error({
        message: i18n.get("Failed to copy"),
      });
      console.error(error);
    }
  }, [text]);

  useEffect(() => {
    if (copied) {
      const timeoutId = window.setTimeout(() => setCopied(false), 2000);
      return () => window.clearTimeout(timeoutId);
    }
  }, [copied]);

  return (
    navigator.clipboard && (
      <>
        <Button
          as="span"
          onClick={handleCopyPassword}
          title={i18n.get("Copy to clipboard")}
          ref={ref}
        >
          <BootstrapIcon icon={copied ? "check-lg" : "copy"} />
        </Button>
        <Popper
          placement="bottom"
          open={copied}
          target={ref.current}
          offset={[0, 4]}
          shadow
          arrow
          rounded
          role={"status"}
        >
          <Box className={styles.copyToClipboard}>{i18n.get("Copied")}</Box>
        </Popper>
      </>
    )
  );
}
