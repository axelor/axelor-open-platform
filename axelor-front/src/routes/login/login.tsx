import { useCallback, useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";

import { Box, Button, Image } from "@axelor/ui";

import { LoginForm } from "@/components/login-form";
import { useLoginInfo } from "@/components/login-form/login-info";
import { useSession } from "@/hooks/use-session";
import { i18n } from "@/services/client/i18n";

import styles from "./login.module.scss";

const LOGIN_ENDPOINT = "login";
const FORCE_CLIENT_PARAM = "force_client";

const CLIENT_NAME_PARAM = "client_name";

const HASH_LOCATION_PARAM = "hash_location";
const FORM_CLIENT_NAME = "AxelorFormClient";

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

function doLogin(client?: string) {
  const currentParams = new URLSearchParams(window.location.search);
  const forceClient = client || currentParams.get(FORCE_CLIENT_PARAM);
  const hashLocation =
    window.location.hash || currentParams.get(HASH_LOCATION_PARAM);
  const params = new URLSearchParams();

  if (forceClient) {
    params.append(FORCE_CLIENT_PARAM, forceClient);
  }

  if (hashLocation) {
    params.append(HASH_LOCATION_PARAM, hashLocation);
  }

  window.location.href = `${LOGIN_ENDPOINT}?${params}`;
}

export function Login() {
  const [errorMessage, setErrorMessage] = useState<string>();

  const location = useLocation();
  const { state, data } = useSession();

  const queryParams = new URLSearchParams(window.location.search);
  const clientNameParam = queryParams.get(CLIENT_NAME_PARAM);
  const clientName =
    CLIENT_NAME_ALIASES[clientNameParam || ""] ?? clientNameParam;

  const publicInfo = useLoginInfo();

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const error = params.get("error");
    if (error != null) {
      setErrorMessage(
        error ||
          i18n.get("Sorry, something went wrong. Please try again later.")
      );
    }
  }, []);

  if (publicInfo.state === "loading" || state === "loading") return null;

  const { exclusive, clients = [], defaultClient } = publicInfo.data || {};
  const client = clientName || defaultClient;

  if (exclusive) {
    doLogin();
    return null;
  }

  if (client && client !== FORM_CLIENT_NAME) {
    doLogin(client);
    return null;
  }

  if (data) {
    let { from } = location.state || { from: { pathname: "/" } };
    if (from === "/login") from = "/";
    return <Navigate to={from} />;
  }

  return (
    <Box as="main" mt={5} ms="auto" me="auto" className={styles.main}>
      <LoginForm error={errorMessage} shadow>
        {clients.length > 0 && (
          <Box as="form" w={100}>
            {clients.map((client) => {
              return (
                <CentralClient
                  key={client.name}
                  name={client.name}
                  title={client.title}
                  icon={client.icon}
                />
              );
            })}
          </Box>
        )}
      </LoginForm>
    </Box>
  );
}

function CentralClient(props: { name: string; title?: string; icon?: string }) {
  const { name, title: _title, icon } = props;
  const title = _title || name;

  const handleClick = useCallback(
    (e: React.SyntheticEvent) => {
      e.preventDefault();
      doLogin(name);
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
