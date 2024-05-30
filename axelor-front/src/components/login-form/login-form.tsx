import { FormEventHandler, useCallback, useMemo, useState } from "react";
import { useLocation } from "react-router-dom";

import {
  AdornedInput,
  Alert,
  Box,
  Button,
  Input,
  InputLabel,
  Select,
} from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";

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
  const [showPassword, setShowPassword] = useState(false);

  const location = useLocation();
  const locationState = location.state;
  const { navigate } = useRoute();

  const session = useSession();
  const appInfo = session.data;

  const defaultClient = appInfo?.authentication?.defaultClient;

  const [tenantId, setTenantId] = useState(appInfo?.authentication?.tenant);

  const tenants = useMemo(() => {
    const val = appInfo?.authentication?.tenants ?? {};
    return Object.entries(val).map(([name, title]) => ({
      name,
      title,
    }));
  }, [appInfo?.authentication?.tenants]);

  const tenant = useMemo(() => {
    return tenants?.find((x) => x.name === tenantId) ?? tenants?.[0];
  }, [tenantId, tenants]);

  const handleTenantChange = useCallback(
    (value?: { name: string; title: string } | null) => {
      const id = value?.name ?? "";
      if (id) {
        setTenantId(id);
      }
    },
    [],
  );

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
          { username, password },
          { params, tenant: tenantId },
        );
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
      tenantId,
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
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck="false"
          />
          <InputLabel htmlFor="password">{i18n.get("Password")}</InputLabel>
          <Box d="flex" position="relative">
            <AdornedInput
              name="password"
              type={showPassword ? "text" : "password"}
              id="password"
              autoComplete="current-password"
              mb={3}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              spellCheck="false"
              endAdornment={
                <Button
                  onClick={() => setShowPassword((value) => !value)}
                  title={
                    showPassword
                      ? i18n.get("Hide password")
                      : i18n.get("Show password")
                  }
                >
                  <BootstrapIcon icon={showPassword ? "eye-slash" : "eye"} />
                </Button>
              }
            />
          </Box>
          {tenants.length > 1 && (
            <Box mb={4}>
              <InputLabel htmlFor="teannt">{i18n.get("Tenant")}</InputLabel>
              <Select
                id="teannt"
                multiple={false}
                value={tenant}
                options={tenants}
                optionKey={(x) => x.name}
                optionLabel={(x) => x.title}
                onChange={handleTenantChange}
                clearIcon={false}
              />
            </Box>
          )}
          <Box d="flex">
            <InputLabel d="flex" alignItems="center" gap={8}>
              <Input
                type="checkbox"
                p={0}
                m={0}
                me={1}
                id="rememberme"
                name="rememberme"
              />
              {i18n.get("Remember me")}
            </InputLabel>
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
