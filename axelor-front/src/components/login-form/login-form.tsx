import { useSession } from "@/hooks/use-session";
import { Alert, Box, Button, Image, Input, InputLabel } from "@axelor/ui";
import { FormEventHandler, useCallback, useState } from "react";

import logo from "@/assets/axelor.svg";
import { i18n } from "@/services/client/i18n";
import { SessionInfo } from "@/services/client/session";

import { useLoginInfo } from "./login-info";

import {
  CLIENT_NAME_PARAM,
  FORM_CLIENT_NAME,
  requestLogin,
} from "@/routes/login";
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

  const appInfo = useLoginInfo();
  const session = useSession();

  const defaultClient = appInfo.data?.defaultClient;

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
        if (info && info.user) {
          onSuccess?.(info);
        } else {
          setShowError(true);
        }
      } catch (e) {
        setShowError(true);
      }
    },
    [session, username, password, onSuccess, defaultClient]
  );

  if (appInfo.state === "loading" || appInfo.state === "hasError") {
    return null;
  }

  const authClient = session.data?.auth?.client;

  if (authClient) {
    requestLogin(authClient);
    return <Reconnecting />;
  }

  const { logo: appLogo = logo, name: appName = "Axelor" } =
    appInfo.data?.application || {};
  const appLegal = appInfo.data?.application.copyright?.replace("&copy;", "©");
  const defaultLegal = `© 2005–${YEAR} Axelor. ${i18n.get(
    "All Rights Reserved"
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
        <Image className={styles.logo} src={appLogo} alt={appName} />
        <Box as="form" w={100} onSubmit={handleSubmit}>
          <InputLabel htmlFor="username">{i18n.get("Username")}</InputLabel>
          <Input
            id="username"
            name="username"
            autoComplete="username"
            autoFocus
            mb={3}
            required
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
            required
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
