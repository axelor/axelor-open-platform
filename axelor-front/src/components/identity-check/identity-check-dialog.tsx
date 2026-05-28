import { useCallback, useEffect, useRef, useState } from "react";

import {
  AdornedInput,
  Alert,
  Box,
  Button,
  Divider,
  Input,
  InputLabel,
  type TForeground,
} from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { useDialogContext } from "@/components/dialogs";
import { LoadingButton } from "@/components/loading-button";
import { i18n } from "@/services/client/i18n";
import {
  IdentityCheckInfo,
  getIdentityCheckInfo,
  verifyIdentity,
} from "@/services/client/identity";
import { MFAMethod, sendEmailVerificationCode } from "@/services/client/mfa";
import { session } from "@/services/client/session";

import styles from "./identity-check.module.scss";

const MFA_OPTIONS: { key: MFAMethod; title: string; color?: TForeground }[] = [
  { key: "TOTP", title: i18n.get("Authenticator app") },
  { key: "EMAIL", title: i18n.get("Email verification") },
  { key: "RECOVERY", title: i18n.get("Recovery code"), color: "danger" },
];

function getMfaDescription(method: MFAMethod) {
  switch (method) {
    case "TOTP":
      return i18n.get("Enter the code from your authentication app.");
    case "EMAIL":
      return i18n.get("Enter the code from verification email.");
    case "RECOVERY":
      return i18n.get("Enter one of your recovery codes.");
  }
  return "";
}

function getMfaPlaceholder(method: MFAMethod) {
  switch (method) {
    case "TOTP":
      return i18n.get("Authenticator code");
    case "EMAIL":
      return i18n.get("Email code");
    case "RECOVERY":
      return i18n.get("Recovery code");
  }
  return "";
}

export function IdentityCheckDialog() {
  const { close } = useDialogContext();

  const [info, setInfo] = useState<IdentityCheckInfo | null>(null);
  const [loading, setLoading] = useState(true);

  // Password state
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  // MFA state
  const [mfaCode, setMfaCode] = useState("");
  const [mfaMethod, setMfaMethod] = useState<MFAMethod>("RECOVERY");
  const [showOptions, setShowOptions] = useState(false);
  const [retryCount, setRetryCount] = useState(0);

  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const passwordRef = useRef<HTMLInputElement>(null);
  const mfaRef = useRef<HTMLInputElement>(null);

  const username = session.info?.user?.login ?? "";

  useEffect(() => {
    let cancelled = false;
    getIdentityCheckInfo()
      .then((result) => {
        if (!cancelled) {
          setInfo(result);
          if (result.mfaMethods?.length) {
            setMfaMethod(result.mfaMethods[0]);
          }
          setLoading(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError(
            i18n.get("Sorry, something went wrong. Please try again later."),
          );
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!loading && info?.requiresPassword) {
      passwordRef.current?.focus();
    } else if (!loading && info?.requiresMfa) {
      mfaRef.current?.focus();
    }
  }, [loading, info, mfaMethod]);

  // Email retry countdown
  useEffect(() => {
    if (mfaMethod === "EMAIL" && retryCount > 0) {
      const timer = setTimeout(() => {
        setRetryCount((c) => c - 1);
      }, 1000);
      return () => clearTimeout(timer);
    }
  }, [mfaMethod, retryCount]);

  const availableMethods: MFAMethod[] = [
    ...(info?.mfaMethods ?? []),
    "RECOVERY",
  ];

  const handleSendEmail = useCallback(async () => {
    if (retryCount > 0) return;
    if (!username) {
      setError(i18n.get("Unable to send verification email."));
      return;
    }

    try {
      const { emailRetryAfter } = await sendEmailVerificationCode(username);
      if (emailRetryAfter) {
        const diff = Math.max(
          0,
          Math.ceil((new Date(emailRetryAfter).getTime() - Date.now()) / 1000),
        );
        setRetryCount(diff);
      }
      setError("");
    } catch (err: any) {
      if (typeof err?.message === "string") {
        setError(err.message);
      }
    }
  }, [retryCount, username]);

  const handleMethodChange = useCallback(
    (method: MFAMethod) => {
      setMfaMethod(method);
      setShowOptions(false);
      setMfaCode("");
      setError("");

      if (method === "EMAIL") {
        handleSendEmail();
      }
    },
    [handleSendEmail],
  );

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setError("");
      setIsSubmitting(true);

      try {
        await verifyIdentity({
          ...(info?.requiresPassword ? { password } : {}),
          ...(info?.requiresMfa ? { mfaCode, mfaMethod } : {}),
        });
        close(true);
      } catch (err) {
        setError(
          typeof err === "string"
            ? err
            : i18n.get("Sorry, something went wrong. Please try again later."),
        );
      } finally {
        setIsSubmitting(false);
      }
    },
    [info, password, mfaCode, mfaMethod, close],
  );

  if (loading) {
    return (
      <Box d="flex" justifyContent="center" p={3}>
        <Box>{i18n.get("Loading…")}</Box>
      </Box>
    );
  }

  const authOptions = MFA_OPTIONS.filter(
    (opt) => opt.key !== mfaMethod && availableMethods.includes(opt.key),
  );

  return (
    <Box as="form" onSubmit={handleSubmit} className={styles.container}>
      {info?.requiresPassword && (
        <>
          <Box mb={3} className={styles.description}>
            {i18n.get("Enter your current password to continue.")}
          </Box>
          <Box className={styles.field}>
            <AdornedInput
              ref={passwordRef}
              id="identity-password"
              name="password"
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              spellCheck="false"
              autoComplete="current-password"
              aria-required="true"
              placeholder={i18n.get("Current password")}
              endAdornment={
                <Button
                  as="span"
                  onClick={() => setShowPassword((v) => !v)}
                  title={
                    showPassword
                      ? i18n.get("Hide password")
                      : i18n.get("Show password")
                  }
                  aria-label={
                    showPassword
                      ? i18n.get("Hide password")
                      : i18n.get("Show password")
                  }
                >
                  <BootstrapIcon
                    icon={showPassword ? "eye-slash" : "eye"}
                    aria-hidden="true"
                  />
                </Button>
              }
            />
          </Box>
        </>
      )}

      {info?.requiresMfa && (
        <>
          <Box mb={3} className={styles.description}>
            {getMfaDescription(mfaMethod)}
          </Box>
          <Box className={styles.field}>
            <Input
              ref={mfaRef}
              id="identity-mfa"
              name="mfaCode"
              value={mfaCode}
              onChange={(e) => setMfaCode(e.target.value)}
              autoCapitalize="none"
              autoComplete="one-time-code"
              autoCorrect="off"
              spellCheck="false"
              placeholder={getMfaPlaceholder(mfaMethod)}
              aria-required="true"
            />
          </Box>

          {mfaMethod === "EMAIL" && (
            <Box mt={2}>
              <LoadingButton
                w={100}
                variant="secondary"
                d="flex"
                disabled={retryCount > 0}
                justifyContent="center"
                onClick={handleSendEmail}
              >
                {retryCount > 0
                  ? i18n.get("Resend email in ({0})s", retryCount)
                  : i18n.get("Resend email")}
              </LoadingButton>
            </Box>
          )}

          {availableMethods.length > 1 && (
            <Box mt={3}>
              <Button
                w={100}
                variant="light"
                d="flex"
                justifyContent="center"
                onClick={() => setShowOptions((v) => !v)}
                aria-expanded={showOptions}
              >
                {i18n.get("Other options")}
                <MaterialIcon
                  icon={showOptions ? "arrow_drop_up" : "arrow_drop_down"}
                  aria-hidden="true"
                />
              </Button>
            </Box>
          )}

          {showOptions && (
            <>
              <Divider />
              {authOptions.map((option) => (
                <Box key={option.key} mt={2}>
                  <Button
                    w={100}
                    variant="light"
                    color={option.color}
                    onClick={() => handleMethodChange(option.key)}
                  >
                    {option.title}
                  </Button>
                </Box>
              ))}
            </>
          )}
        </>
      )}

      {error && (
        <Alert mt={2} mb={1} p={2} variant="danger" role="alert">
          {error}
        </Alert>
      )}

      <Box d="flex" g={2} justifyContent="end" mt={3}>
        <Button variant="secondary" type="button" onClick={() => close(false)}>
          {i18n.get("Cancel")}
        </Button>
        <LoadingButton
          type="submit"
          variant="primary"
          loading={isSubmitting}
          disabled={info?.requiresPassword ? !password : !mfaCode}
          aria-label={i18n.get("Verify")}
        >
          {i18n.get("Verify")}
        </LoadingButton>
      </Box>
    </Box>
  );
}
