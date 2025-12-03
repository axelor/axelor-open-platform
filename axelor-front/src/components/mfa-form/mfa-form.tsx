import { useCallback, useEffect, useRef, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Divider,
  Input,
  InputLabel,
  TVariant,
  type TForeground,
} from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { LoadingButton } from "@/components/loading-button";
import { AppSignInLogo } from "@/components/app-logo";
import { useAppSettings } from "@/hooks/use-app-settings";
import { i18n } from "@/services/client/i18n";
import { useSession } from "@/hooks/use-session";
import { moment } from "@/services/client/l10n";
import { SessionInfo } from "@/services/client/session";
import {
  MFAMethod,
  mfaSession,
  sendEmailVerificationCode,
} from "@/services/client/mfa";
import { CLIENT_NAME_PARAM } from "@/routes/login";
import styles from "./mfa-form.module.scss";

function getTimeoutOfEmailRetryByUser(username: string) {
  const retryAfter = mfaSession.EmailRetryAfter.get(username);
  if (retryAfter) {
    return Math.max(0, moment(retryAfter).diff(moment(), "second"));
  }
  return 0;
}

export function MFAForm({
  state,
  shadow,
  onSuccess,
  onBackToLogin,
}: {
  state?: {
    methods?: MFAMethod[];
    username?: string;
    emailRetryAfter?: string;
    tenant?: string;
  };
  shadow?: boolean;
  onSuccess?: (info: SessionInfo) => void;
  onBackToLogin?: () => void;
}) {
  const session = useSession();
  const { copyright } = useAppSettings();
  const { methods = [], username = "", emailRetryAfter, tenant } = state ?? {};

  const availableMethods: MFAMethod[] = [...methods, "RECOVERY"];

  const [mfaCode, setMFACode] = useState("");
  const [mfaMethod, setMFAMethod] = useState<MFAMethod>(
    mfaSession.MFAMethod.get(username) ?? availableMethods?.[0],
  );
  const [showOptions, setShowOptions] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showResend, setShowResend] = useState(mfaMethod !== "EMAIL");
  const [retryCount, setRetryCount] = useState(0);
  const [alert, setAlert] = useState<{
    variant: TVariant;
    message: string;
  } | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);

  const showAlert = useCallback((variant: TVariant, message: string) => {
    setAlert({ variant, message });
  }, []);

  const handleSendEmail = useCallback(async () => {
    if (getTimeoutOfEmailRetryByUser(username) > 0) {
      return;
    }

    try {
      const { message, emailRetryAfter } =
        await sendEmailVerificationCode(username);

      mfaSession.EmailRetryAfter.set(username, emailRetryAfter);

      const diff = getTimeoutOfEmailRetryByUser(username);
      setRetryCount(diff);

      showAlert("success", message);
    } catch (err: any) {
      // request is forbidden then
      // redirect to login page
      if (err === 403) {
        return onBackToLogin?.();
      }
      if (typeof err?.message === "string") {
        showAlert("danger", err.message);
        if (err?.emailRetryAfter) {
          mfaSession.EmailRetryAfter.set(usernameKey, err?.emailRetryAfter);
          const diff = getTimeoutOfEmailRetryByUser(usernameKey);
          setRetryCount(diff);
        } else {
          setRetryCount(0);
        }
      }
    }
  }, [username, onBackToLogin, showAlert]);

  const handleMFAMethodChange = useCallback(
    (method: MFAMethod) => {
      setMFAMethod(method);
      mfaSession.MFAMethod.set(username, method);

      // reset states
      setShowOptions(false);
      setMFACode("");
      setAlert(null);

      // auto send email for first time only
      // skip for subsequent attempts
      if (method === "EMAIL" && !mfaSession.EmailRetryAfter.get(username)) {
        handleSendEmail();
      }
    },
    [username, handleSendEmail],
  );

  const handleSubmit: (event: React.SyntheticEvent) => Promise<void> =
    useCallback(
      async (event) => {
        event.preventDefault();

        const errorText = i18n.get("Invalid verification code");
        const serverErrorText = i18n.get(
          "Sorry, something went wrong. Please try again later.",
        );

        setIsSubmitting(true);

        try {
          const params = new URLSearchParams({
            [CLIENT_NAME_PARAM]: "MfaClient",
          });
          const info = await session.login(
            { username, mfaCode, mfaMethod },
            { params, tenant },
          );
          const { user } = info;

          if (user) {
            onSuccess?.(info);
          } else {
            showAlert("danger", errorText);
          }
        } catch (err: any) {
          if (err === 403) {
            return onBackToLogin?.();
          }
          showAlert("danger", err === 500 ? serverErrorText : errorText);
        } finally {
          setIsSubmitting(false);
        }
      },
      [
        session,
        username,
        mfaCode,
        mfaMethod,
        tenant,
        showAlert,
        onBackToLogin,
        onSuccess,
      ],
    );

  useEffect(() => {
    // on MFA Change, re-focus input
    inputRef?.current?.focus();

    if (mfaMethod === "EMAIL") {
      const count = getTimeoutOfEmailRetryByUser(username);
      setRetryCount(count);
    }
    setShowResend(true);
  }, [mfaMethod, username]);

  useEffect(() => {
    if (mfaMethod === "EMAIL" && retryCount > 0) {
      const timer = setTimeout(() => {
        setRetryCount((count) => count - 1);
      }, 1000);
      return () => {
        clearTimeout(timer);
      };
    }
  }, [mfaMethod, retryCount]);

  useEffect(() => {
    // if default method is email then
    // no need to execute initial email attempt
    if (username && emailRetryAfter) {
      const current = mfaSession.EmailRetryAfter.get(username);

      // Only set if storage is empty or the route value is newer than stored value
      // This prevents stale route state (on F5 refresh) from overwriting new resend timers
      if (!current || moment(emailRetryAfter).isAfter(moment(current))) {
        mfaSession.EmailRetryAfter.set(username, emailRetryAfter);
        const diff = getTimeoutOfEmailRetryByUser(username);
        setRetryCount(diff);
      }
    }
  }, [username, emailRetryAfter]);

  const authOptions = (
    [
      { key: "TOTP", title: i18n.get("Authenticator app") },
      { key: "EMAIL", title: i18n.get("Email verification") },
      {
        key: "RECOVERY",
        title: i18n.get("Recovery code"),
        color: "danger",
      },
    ] as { key: MFAMethod; title: string; color?: TForeground }[]
  ).filter(
    (opt) => opt.key !== mfaMethod && availableMethods.includes(opt.key),
  );

  function getTitle() {
    return i18n.get("Two-factor authentication");
  }

  function getDescription() {
    switch (mfaMethod) {
      case "TOTP":
        return i18n.get("Enter the code from your authentication app.");
      case "EMAIL":
        return i18n.get("Enter the code from verification email.");
      case "RECOVERY":
        return i18n.get("Enter one of your recovery codes.");
    }
    return "";
  }

  function getPlaceholder() {
    switch (mfaMethod) {
      case "TOTP":
        return i18n.get("Authenticator code");
      case "EMAIL":
        return i18n.get("Email code");
      case "RECOVERY":
        return i18n.get("Recovery code");
    }
    return "";
  }

  return (
    <Box className={styles.container}>
      <Box
        className={styles.paper}
        shadow={shadow ? "2xl" : false}
        d="flex"
        flexDirection="column"
        alignItems="center"
        p={3}
        mb={3}
      >
        <AppSignInLogo className={styles.logo} />
        <Box as="form" w={100} onSubmit={handleSubmit} mt={3}>
          <Box>
            <Box mt={2} mb={3} as="h3" textAlign={"center"}>
              {getTitle()}
            </Box>
            <Box as="p" textAlign={"center"}>
              {getDescription()}
            </Box>
          </Box>

          <Box>
            <InputLabel htmlFor="mfaCode">
              {i18n.get("Verification code")}
            </InputLabel>
            <Input
              ref={inputRef}
              id="mfaCode"
              name="mfaCode"
              autoFocus
              value={mfaCode}
              onChange={(e) => setMFACode(e.target.value)}
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck="false"
              placeholder={getPlaceholder()}
            />
          </Box>

          {alert && (
            <Alert mt={3} mb={1} p={2} variant={alert.variant}>
              {alert.message}
            </Alert>
          )}

          <LoadingButton
            type={"submit"}
            variant={"primary"}
            loading={isSubmitting}
            disabled={!mfaCode}
            d="flex"
            justifyContent="center"
            mt={2}
            w={100}
            gap={4}
          >
            {i18n.get("Verify")}
          </LoadingButton>

          {mfaMethod === "EMAIL" && (
            <Box my={2}>
              <LoadingButton
                w={100}
                variant="secondary"
                d="flex"
                disabled={!showResend || retryCount > 0}
                justifyContent={"center"}
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
                justifyContent={"center"}
                onClick={() => setShowOptions(!showOptions)}
              >
                {i18n.get("Other options")}
                <MaterialIcon
                  icon={showOptions ? "arrow_drop_up" : "arrow_drop_down"}
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
                    onClick={() =>
                      handleMFAMethodChange(option.key as MFAMethod)
                    }
                  >
                    {option.title}
                  </Button>
                </Box>
              ))}
            </>
          )}
        </Box>
      </Box>
      <Box mt={3} as="p" textAlign="center">
        {copyright}
      </Box>
    </Box>
  );
}
