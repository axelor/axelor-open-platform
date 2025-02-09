import { useCallback, useEffect, useMemo } from "react";
import { Navigate, useLocation } from "react-router-dom";

import { Alert, Box, Button, Image } from "@axelor/ui";

import { alerts, AlertsProvider } from "@/components/alerts";
import { AppSignInLogo } from "@/components/app-logo/app-logo";
import { LoginForm } from "@/components/login-form";
import { useAppHead } from "@/hooks/use-app-head";
import { useSession } from "@/hooks/use-session";
import { i18n } from "@/services/client/i18n";
import { ClientInfo } from "@/services/client/session";

import styles from "./login.module.scss";

const LOGIN_ENDPOINT = "login";
const FORCE_CLIENT_PARAM = "force_client";

export const CLIENT_NAME_PARAM = "client_name";

const HASH_LOCATION_PARAM = "hash_location";
const ERROR_PARAM = "error";
export const FORM_CLIENT_NAME = "AxelorFormClient";

const CLIENT_NAME_ALIASES: Record<string, string> = {
  form: FORM_CLIENT_NAME,
  oidc: "OidcClient",
  keycloak: "KeycloakOidcClient",
  google: "GoogleOidcClient",
  azure: "AzureAd2Client",
  apple: "AppleClient",
  oauth: "GenericOAuth20Client",
  facebook: "FacebookClient",
  github: "GitHubClient",
  saml: "SAML2Client",
  cas: "CasClient",
};

export function requestLogin(client?: string) {
  const currentParams = new URLSearchParams(window.location.search);
  const forceClient = client || currentParams.get(FORCE_CLIENT_PARAM);
  const clientName = currentParams.get(CLIENT_NAME_PARAM);
  const hashLocation =
    window.location.hash || currentParams.get(HASH_LOCATION_PARAM);
  const params = new URLSearchParams();

  if (forceClient) {
    params.append(FORCE_CLIENT_PARAM, forceClient);
  }

  if (clientName) {
    params.append(CLIENT_NAME_PARAM, clientName);
  }

  if (hashLocation) {
    params.append(HASH_LOCATION_PARAM, hashLocation);
  }

  window.location.href = `${LOGIN_ENDPOINT}?${params}`;
}

export function Login() {
  const location = useLocation();
  const { state: locationState } = location ?? {};

  const { state, data } = useSession();
  const queryParams = useMemo(
    () => new URLSearchParams(window.location.search),
    [],
  );
  const clientNameParam = queryParams.get(CLIENT_NAME_PARAM);
  const clientName =
    CLIENT_NAME_ALIASES[clientNameParam || ""] ?? clientNameParam;

  useAppHead();

  const errorMessage = useMemo(() => {
    const error = queryParams.get(ERROR_PARAM);
    if (error != null) {
      return (
        error ||
        i18n.get("Sorry, something went wrong. Please try again later.")
      );
    }
  }, [queryParams]);

  useEffect(() => {
    if (locationState?.message) {
      alerts.info({ message: locationState.message });
      delete locationState.message;
    }
  }, [locationState?.message]);

  if (state === "loading") return null;

  const { clients = [], defaultClient, exclusive } = data?.authentication ?? {};
  const client = clientName || defaultClient;
  const notFormClient = client && client !== FORM_CLIENT_NAME;

  // Server error and no form client
  if (errorMessage != null && (exclusive || notFormClient)) {
    return <ServerError error={errorMessage} />;
  }

  if (exclusive) {
    requestLogin();
    return null;
  }

  if (notFormClient) {
    requestLogin(client);
    return null;
  }

  if (data?.user) {
    removeErrorParam();
    let { from } = location.state || { from: { pathname: "/" } };
    if (from === "/login") from = "/";
    return <Navigate to={from} />;
  }

  return (
    <Box as="main" mt={5} ms="auto" me="auto" className={styles.main}>
      <LoginForm error={errorMessage} shadow>
        <CentralClients clients={clients} />
      </LoginForm>
      <AlertsProvider />
    </Box>
  );
}

function CentralClients({ clients }: { clients: ClientInfo[] }) {
  return clients.length > 0 ? (
    <>
      <Box d="flex" alignItems="center" mt={3}>
        <Box as="hr" flexGrow={1} />
        <Box mx={3}>{i18n.get("or sign in with")}</Box>
        <Box as="hr" flexGrow={1} />
      </Box>
      <Box as="form" w={100} mb={3}>
        {clients.map((client) => (
          <CentralClient
            key={client.name}
            name={client.name}
            title={client.title}
            icon={client.icon}
          />
        ))}
      </Box>
    </>
  ) : null;
}

function CentralClient(props: { name: string; title?: string; icon?: string }) {
  const { name, title: _title, icon } = props;
  const title = _title || name;

  const handleClick = useCallback(
    (e: React.SyntheticEvent) => {
      e.preventDefault();
      requestLogin(name);
    },
    [name],
  );

  return (
    <Box d="flex" flexDirection="column">
      <Button
        className={styles.socialButton}
        d="flex"
        alignItems="center"
        justifyContent="center"
        border={true}
        mt={2}
        w={100}
        onClick={handleClick}
        gap={4}
      >
        {icon && <Image className={styles.socialLogo} src={icon} alt={title} />}
        {title}
      </Button>
    </Box>
  );
}

function ServerError({ error }: { error: string }) {
  return (
    <Box as="main" mt={5} ms="auto" me="auto" className={styles.main}>
      <Box
        className={styles.paper}
        shadow="2xl"
        d="flex"
        flexDirection="column"
        alignItems="center"
        p={3}
      >
        <AppSignInLogo className={styles.logo} />
        <Alert mt={3} mb={1} p={2} variant="danger">
          {error}
        </Alert>
      </Box>
    </Box>
  );
}

function removeErrorParam() {
  const queryParams = new URLSearchParams(window.location.search);
  if (queryParams.has(ERROR_PARAM)) {
    queryParams.delete(ERROR_PARAM);

    window.history.replaceState(
      {},
      document.title,
      `?${queryParams}${window.location.hash}`,
    );
  }
}
