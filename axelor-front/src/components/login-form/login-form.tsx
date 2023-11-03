import { FormEventHandler, useCallback, useState } from "react";
import { useLocation } from "react-router-dom";

import { Alert, Box, Button, Input, InputLabel } from "@axelor/ui";

import { useRoute } from "@/hooks/use-route";
import { useSession } from "@/hooks/use-session";
import { i18n } from "@/services/client/i18n";
import { SessionInfo } from "@/services/client/session";

import {
  CLIENT_NAME_PARAM,
  FORM_CLIENT_NAME,
  requestLogin,
} from "@/routes/login";

import logo from "@/assets/axelor.svg";
import styles from "./login-form.module.scss";

const YEAR = new Date().getFullYear();

export type LoginFormProps = {
  onSuccess?: (info: SessionInfo) => void;
  shadow?: boolean;
  error?: string;
  children?: React.ReactNode;
};

export function LoginForm({
  onSuccess,
  error,
  shadow,
  children,
}: LoginFormProps) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showError, setShowError] = useState(false);

  const location = useLocation();
  const locationState = location.state;
  const { navigate } = useRoute();

  const session = useSession();
  const appInfo = session.data;

  const defaultClient = appInfo?.authentication?.defaultClient;

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
        const info = await session.login({ username, password }, params);
        const { user, route } = info;

        if (route) {
          const { path, state } = route;
          navigate(path, {
            state: {
              ...locationState,
              route: { ...state, username, password },
            },
          });
        } else if (user) {
          onSuccess?.(info);
        } else {
          setShowError(true);
        }
      } catch (e) {
        setShowError(true);
      }
    },
    [
      defaultClient,
      session,
      username,
      password,
      navigate,
      locationState,
      onSuccess,
    ],
  );

  if (session.state === "loading" || session.state === "hasError") {
    return null;
  }

  const currentClient = session.data?.authentication?.currentClient;

  if (currentClient) {
    requestLogin(currentClient);
    return <Reconnecting />;
  }

  const { logo: appLogo = logo, name: appName = "Axelor" } =
    appInfo?.application || {};
  const appLegal = appInfo?.application.copyright?.replace("&copy;", "©");
  const defaultLegal = `© 2005–${YEAR} Axelor. ${i18n.get(
    "All Rights Reserved",
  )}.`;

  const copyright = appLegal || defaultLegal;

  let errorText;
  if (showError) {
    errorText = i18n.get("Wrong username or password");
  } else if (error != null || session.error === 500) {
    errorText =
      error || i18n.get("Sorry, something went wrong. Please try again later.");
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
      >
        <img
          className={styles.logo}
          src={appLogo}
          alt={appName}
          onError={(e) => {
            e.currentTarget.src = logo;
          }}
        />
        <Box as="form" w={100} onSubmit={handleSubmit}>
          <InputLabel htmlFor="username">{i18n.get("Username")}</InputLabel>
          <Input
            id="username"
            name="username"
            autoComplete="username"
            autoFocus
            mb={3}
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
          <InputLabel htmlFor="password">{i18n.get("Password")}</InputLabel>
          <Input
            name="password"
            type="password"
            id="password"
            autoComplete="current-password"
            mb={3}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Box d="flex" alignItems="center">
            <Input type="checkbox" p={0} m={0} me={1} />
            <Box as="p" mb={0}>
              {i18n.get("Remember me")}
            </Box>
          </Box>
          {errorText && (
            <Alert mt={3} mb={1} p={2} variant="danger">
              {errorText}
            </Alert>
          )}
          <Button type="submit" variant="primary" mt={3} w={100}>
            {i18n.get("Log in")}
          </Button>
        </Box>
      </Box>
      {children}
      <Box as="p" textAlign="center">
        {copyright}
      </Box>
    </Box>
  );
}

function Reconnecting() {
  return (
    <Box className={styles.container}>
      <Box
        className={styles.paper}
        d="flex"
        flexDirection="column"
        alignItems="center"
        p={3}
      >
        <Box as="h4" fontWeight="normal" my={2}>
          {i18n.get("Reconnecting…")}
        </Box>
      </Box>
    </Box>
  );
}
