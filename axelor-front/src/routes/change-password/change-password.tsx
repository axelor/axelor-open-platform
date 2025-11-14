import {
  FormEventHandler,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
} from "react";
import { Navigate, useLocation } from "react-router";

import { AdornedInput, Alert, Box, Button, InputLabel } from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";

import { AppSignInLogo } from "@/components/app-logo/app-logo";
import { LoadingButton } from "@/components/loading-button";
import { useAppSettings } from "@/hooks/use-app-settings";
import { useRoute } from "@/hooks/use-route";
import { useSession } from "@/hooks/use-session";
import { i18n } from "@/services/client/i18n";
import { CLIENT_NAME_PARAM, FORM_CLIENT_NAME } from "../login";

import styles from "./change-password.module.scss";

export function ChangePassword({
  onSubmit,
  error: propsErrorMessage,
  passwordPattern: propsPasswordPattern,
  passwordPatternTitle: propsPasswordPatternTitle,
}: {
  onSubmit?: FormEventHandler<HTMLFormElement>;
  error?: string;
  passwordPattern?: string;
  passwordPatternTitle?: string;
}) {
  const session = useSession();
  const { copyright } = useAppSettings();
  const appInfo = session.data;
  const defaultClient = appInfo?.authentication?.defaultClient;

  const { navigate } = useRoute();
  const location = useLocation();
  const { route, ...locationState } = location.state ?? {};
  const {
    username,
    error,
    passwordPattern = propsPasswordPattern,
    passwordPatternTitle = propsPasswordPatternTitle,
  } = route ?? {};

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [_errorMessage, setErrorMessage] = useState(error);
  const errorMessage = propsErrorMessage || _errorMessage;
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const requireCurrentPassword = Boolean(username);

  const handleSubmit: FormEventHandler<HTMLFormElement> = useCallback(
    async (event) => {
      setIsSubmitting(true);

      if (onSubmit) {
        await onSubmit(event);
        setIsSubmitting(false);
        return;
      }

      event.preventDefault();

      // Need to specify client if default client is not form client.
      const params =
        defaultClient && defaultClient !== FORM_CLIENT_NAME
          ? new URLSearchParams({
              [CLIENT_NAME_PARAM]: FORM_CLIENT_NAME,
            })
          : undefined;

      try {
        const info = await session.login(
          { username, password: currentPassword, newPassword },
          { params },
        );

        const { user, route } = info;
        setIsSubmitting(false);

        if (route) {
          const { path, state } = route;
          if (path === "/change-password") {
            const { error } = state;
            if (error) {
              setErrorMessage(String(error));
              return;
            }
          } else {
            navigate(path, {
              state: {
                ...locationState,
                route: { ...state, username },
              },
            });
            return;
          }
        } else if (user) {
          let { from } = locationState || { from: { pathname: "/" } };
          if (!from || from === "/login") from = "/";
          navigate(from);
          return;
        }
      } catch (e) {
        if (e === 401) {
          setErrorMessage(i18n.get("Wrong current username or password"));
          return;
        }
      } finally {
        setIsSubmitting(false);
      }

      setErrorMessage(
        i18n.get("Sorry, something went wrong. Please try again later."),
      );
    },
    [
      defaultClient,
      session,
      username,
      currentPassword,
      newPassword,
      navigate,
      locationState,
      onSubmit,
    ],
  );

  const passwordPatternExp = useMemo(
    () => new RegExp(passwordPattern),
    [passwordPattern],
  );

  const getNewPasswordValidity = useCallback(
    (value: string) => {
      let validity = "";
      if (value) {
        if (requireCurrentPassword && value === currentPassword) {
          validity = i18n.get("New password must be different.");
        } else if (!passwordPatternExp.test(value)) {
          validity = passwordPatternTitle;
        }
      }
      return validity;
    },
    [
      currentPassword,
      passwordPatternExp,
      passwordPatternTitle,
      requireCurrentPassword,
    ],
  );

  const newPasswordValidity = useMemo(() => {
    return getNewPasswordValidity(newPassword);
  }, [getNewPasswordValidity, newPassword]);

  const getConfirmPasswordValidity = useCallback(
    (value: string) => {
      return value && value !== newPassword
        ? i18n.get("Passwords do not match.")
        : "";
    },
    [newPassword],
  );

  const confirmPasswordValidity = useMemo(() => {
    return getConfirmPasswordValidity(confirmPassword);
  }, [getConfirmPasswordValidity, confirmPassword]);

  const currentPasswordInputRef = useRef<HTMLInputElement>(null);
  const newPasswordInputRef = useRef<HTMLInputElement>(null);
  const confirmPasswordInputRef = useRef<HTMLInputElement>(null);

  const errorId = useId();
  const newPasswordHelpId = useId();
  const confirmPasswordHelpId = useId();

  useEffect(() => {
    newPasswordInputRef.current?.setCustomValidity(newPasswordValidity);
  }, [newPasswordValidity]);

  useEffect(() => {
    confirmPasswordInputRef.current?.setCustomValidity(confirmPasswordValidity);
  }, [confirmPasswordValidity]);

  if (!username && !onSubmit) {
    return <Navigate to={"/"} />;
  }

  return (
    <Box
      as="main"
      mt={5}
      ms="auto"
      me="auto"
      className={styles.main}
      data-testid="change-password-page"
    >
      <Box className={styles.container}>
        <Box
          className={styles.paper}
          shadow={"2xl"}
          d="flex"
          flexDirection="column"
          alignItems="center"
          p={3}
        >
          <AppSignInLogo className={styles.logo} data-testid="logo" />
          <Box as="legend" style={{ textWrap: "balance" }} data-testid="title">
            {i18n.get("Change your password")}
          </Box>
          <Box
            as="form"
            w={100}
            onSubmit={handleSubmit}
            onInput={() => setErrorMessage("")}
            data-testid="form"
          >
            {requireCurrentPassword && (
              <Box
                className={styles.inputContainer}
                data-testid="field-current-password"
              >
                <InputLabel htmlFor="password" data-testid="label">
                  {i18n.get("Current password")}
                </InputLabel>
                <AdornedInput
                  ref={currentPasswordInputRef}
                  id="password"
                  name="password"
                  type={showCurrentPassword ? "text" : "password"}
                  autoFocus
                  mb={3}
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  spellCheck="false"
                  aria-required="true"
                  aria-describedby={errorMessage ? errorId : undefined}
                  data-testid="input"
                  endAdornment={
                    <Button
                      as="span"
                      onClick={() => setShowCurrentPassword((value) => !value)}
                      title={
                        showCurrentPassword
                          ? i18n.get("Hide password")
                          : i18n.get("Show password")
                      }
                      aria-label={
                        showCurrentPassword
                          ? i18n.get("Hide password")
                          : i18n.get("Show password")
                      }
                      data-testid="btn-toggle-current-password"
                    >
                      <BootstrapIcon
                        icon={showCurrentPassword ? "eye-slash" : "eye"}
                        aria-hidden="true"
                      />
                    </Button>
                  }
                />
              </Box>
            )}
            <Box
              className={styles.inputContainer}
              data-testid="field-new-password"
            >
              <InputLabel htmlFor="newPassword" data-testid="label">
                {i18n.get("New password")}
              </InputLabel>
              <AdornedInput
                ref={newPasswordInputRef}
                id="newPassword"
                name="newPassword"
                type={showPassword ? "text" : "password"}
                autoFocus={!requireCurrentPassword}
                mb={3}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                spellCheck="false"
                aria-required="true"
                aria-invalid={!!newPasswordValidity}
                aria-describedby={
                  newPasswordValidity
                    ? newPasswordHelpId
                    : errorMessage
                      ? errorId
                      : undefined
                }
                data-testid="input"
                endAdornment={
                  <Button
                    as="span"
                    onClick={() => setShowPassword((value) => !value)}
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
                    data-testid="btn-toggle-password"
                  >
                    <BootstrapIcon
                      icon={showPassword ? "eye-slash" : "eye"}
                      aria-hidden="true"
                    />
                  </Button>
                }
              />
              {newPasswordValidity && (
                <Box
                  className={styles.validity}
                  id={newPasswordHelpId}
                  role="alert"
                  data-testid="help"
                >
                  {newPasswordValidity}
                </Box>
              )}
            </Box>

            <Box
              className={styles.inputContainer}
              data-testid="field-confirm-password"
            >
              <InputLabel htmlFor="confirmPassword" data-testid="label">
                {i18n.get("Confirm new password")}
              </InputLabel>
              <AdornedInput
                ref={confirmPasswordInputRef}
                id="confirmPassword"
                name="confirmPassword"
                type={showConfirmPassword ? "text" : "password"}
                mb={3}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                spellCheck="false"
                aria-required="true"
                aria-invalid={!!confirmPasswordValidity}
                aria-describedby={
                  confirmPasswordValidity
                    ? confirmPasswordHelpId
                    : errorMessage
                      ? errorId
                      : undefined
                }
                data-testid="input"
                endAdornment={
                  <Button
                    as="span"
                    onClick={() => setShowConfirmPassword((value) => !value)}
                    title={
                      showConfirmPassword
                        ? i18n.get("Hide password")
                        : i18n.get("Show password")
                    }
                    aria-label={
                      showConfirmPassword
                        ? i18n.get("Hide password")
                        : i18n.get("Show password")
                    }
                    data-testid="btn-toggle-confirm-password"
                  >
                    <BootstrapIcon
                      icon={showConfirmPassword ? "eye-slash" : "eye"}
                      aria-hidden="true"
                    />
                  </Button>
                }
              />

              {confirmPasswordValidity && (
                <Box
                  className={styles.validity}
                  id={confirmPasswordHelpId}
                  role="alert"
                  data-testid="help"
                >
                  {confirmPasswordValidity}
                </Box>
              )}
            </Box>

            {errorMessage && (
              <Alert
                mb={1}
                p={2}
                variant="danger"
                className={styles.error}
                id={errorId}
                role="alert"
                data-testid="error"
              >
                {errorMessage}
              </Alert>
            )}

            <LoadingButton
              type="submit"
              variant="primary"
              d="flex"
              justifyContent="center"
              gap={4}
              mt={3}
              w={100}
              loading={isSubmitting}
              disabled={
                (requireCurrentPassword && !currentPassword) ||
                !newPassword ||
                !confirmPassword
              }
              data-testid="btn-change-password"
              aria-label={i18n.get("Change password")}
            >
              {i18n.get("Change password")}
            </LoadingButton>
          </Box>
        </Box>
        <Box as="p" textAlign="center">
          {copyright}
        </Box>
      </Box>
    </Box>
  );
}
