import {
  FormEventHandler,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { Navigate, useLocation } from "react-router-dom";

import { Alert, Box, Button, Input, InputLabel } from "@axelor/ui";

import { useRoute } from "@/hooks/use-route";
import { useSession } from "@/hooks/use-session";
import { i18n } from "@/services/client/i18n";
import { CLIENT_NAME_PARAM, FORM_CLIENT_NAME } from "../login";

import logo from "@/assets/axelor.svg";
import styles from "./change-password.module.scss";

export function ChangePassword() {
  const session = useSession();
  const appInfo = session.data;
  const defaultClient = appInfo?.authentication?.defaultClient;

  const { navigate } = useRoute();
  const location = useLocation();
  const { route, ...locationState } = location.state ?? {};
  const { username, password, error, passwordPattern, passwordPatternTitle } =
    route ?? {};

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState(error);

  const { logo: appLogo = logo, name: appName = "Axelor" } =
    appInfo?.application || {};
  const appLegal = appInfo?.application.copyright?.replace("&copy;", "©");
  const defaultLegal = `© 2005–${new Date().getFullYear()} Axelor. ${i18n.get(
    "All Rights Reserved",
  )}.`;

  const copyright = appLegal || defaultLegal;

  const handleSubmit: FormEventHandler<HTMLFormElement> = useCallback(
    async (event) => {
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
          { username, password, newPassword },
          params,
        );

        const { user, route } = info;

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
                route: { ...state, username, password, newPassword },
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
      }

      setErrorMessage(
        i18n.get("Sorry, something went wrong. Please try again later."),
      );
    },
    [
      defaultClient,
      session,
      username,
      password,
      newPassword,
      navigate,
      locationState,
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
        if (value === password) {
          validity = i18n.get("New password must be different.");
        } else if (!passwordPatternExp.test(value)) {
          validity = passwordPatternTitle;
        }
      }
      return validity;
    },
    [password, passwordPatternExp, passwordPatternTitle],
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

  const newPasswordInputRef = useRef<HTMLInputElement>(null);
  const confirmPasswordInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    newPasswordInputRef.current?.setCustomValidity(newPasswordValidity);
  }, [newPasswordValidity]);

  useEffect(() => {
    confirmPasswordInputRef.current?.setCustomValidity(confirmPasswordValidity);
  }, [confirmPasswordValidity]);

  if (!username) {
    return <Navigate to={"/"} />;
  }

  return (
    <Box as="main" mt={5} ms="auto" me="auto" className={styles.main}>
      <Box className={styles.container}>
        <Box
          className={styles.paper}
          shadow={"2xl"}
          d="flex"
          flexDirection="column"
          alignItems="center"
          p={3}
        >
          <img
            className={styles.logo}
            src={appLogo}
            alt={appName}
            onError={(e) => {
              e.currentTarget.src = logo;
            }}
          />
          <Box as="legend">{i18n.get("Change your password")}</Box>

          <Box
            as="form"
            w={100}
            onSubmit={handleSubmit}
            onInput={() => setErrorMessage("")}
          >
            <Box className={styles.inputContainer}>
              <InputLabel htmlFor="newPassword">
                {i18n.get("New password")}
              </InputLabel>
              <Input
                ref={newPasswordInputRef}
                id="newPassword"
                name="newPassword"
                type="password"
                autoFocus
                mb={3}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
              {newPasswordValidity && (
                <Box className={styles.validity}>{newPasswordValidity}</Box>
              )}
            </Box>

            <Box className={styles.inputContainer}>
              <InputLabel htmlFor="confirmPassword">
                {i18n.get("Confirm new password")}
              </InputLabel>
              <Input
                ref={confirmPasswordInputRef}
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                mb={3}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
              {confirmPasswordValidity && (
                <Box className={styles.validity}>{confirmPasswordValidity}</Box>
              )}
            </Box>

            {errorMessage && (
              <Alert
                mt={3}
                mb={1}
                p={2}
                variant="danger"
                className={styles.error}
              >
                {errorMessage}
              </Alert>
            )}

            <Button
              type="submit"
              variant="primary"
              mt={3}
              w={100}
              disabled={Boolean(!newPassword || !confirmPassword)}
            >
              {i18n.get("Change password")}
            </Button>
          </Box>
        </Box>
        <Box as="p" textAlign="center">
          {copyright}
        </Box>
      </Box>
    </Box>
  );
}
