import { useCallback, useMemo } from "react";
import { Navigate, useLocation } from "react-router-dom";

import { Alert, Box, Button, Image } from "@axelor/ui";

import { LoginForm } from "@/components/login-form";
import { ClientInfo, useLoginInfo } from "@/components/login-form/login-info";
import { useAppTitle } from "@/hooks/use-app-title";
import { useSession } from "@/hooks/use-session";
import { i18n } from "@/services/client/i18n";

import logo from "@/assets/axelor.svg";
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
  const { state, data, redirectUrl } = useSession();
  const queryParams = useMemo(
    () => new URLSearchParams(window.location.search),
    []
  );
  const clientNameParam = queryParams.get(CLIENT_NAME_PARAM);
  const clientName =
    CLIENT_NAME_ALIASES[clientNameParam || ""] ?? clientNameParam;

  const publicInfo = useLoginInfo();

  useAppTitle();

  const errorMessage = useMemo(() => {
    const error = queryParams.get(ERROR_PARAM);
    if (error != null) {
      return (
        error ||
        i18n.get("Sorry, something went wrong. Please try again later.")
      );
    }
  }, [queryParams]);

  if (publicInfo.state === "loading" || state === "loading") return null;

  if (redirectUrl != null) {
    if (redirectUrl) {
      window.location.href = redirectUrl;
    } else {
      window.location.reload();
    }
    return null;
  }

  const { exclusive, clients = [], defaultClient } = publicInfo.data || {};
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

  if (data) {
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
    </Box>
  );
}

function CentralClients({ clients }: { clients: ClientInfo[] }) {
  return clients.length > 0 ? (
    <Box as="form" w={100} className={styles.clients}>
      {clients.map((client) => (
        <CentralClient
          key={client.name}
          name={client.name}
          title={client.title}
          icon={client.icon}
        />
      ))}
    </Box>
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
    [name]
  );

  return (
    <Box d="flex" flexDirection="column">
      <Button
        d="flex"
        alignItems="center"
        justifyContent="center"
        type="submit"
        variant="secondary"
        mt={2}
        w={100}
        onClick={handleClick}
      >
        {icon && <Image className={styles.socialLogo} src={icon} alt={title} />}
        <Box>{i18n.get("Log in with {0}", title)}</Box>
      </Button>
    </Box>
  );
}

function ServerError({ error }: { error: string }) {
  const publicInfo = useLoginInfo();
  const { logo: appLogo = logo, name: appName = "Axelor" } =
    publicInfo.data?.application || {};

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
        <Image className={styles.logo} src={appLogo} alt={appName} />
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
      `?${queryParams}${window.location.hash}`
    );
  }
}
