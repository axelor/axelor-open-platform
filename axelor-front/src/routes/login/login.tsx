import { FormEventHandler, useCallback, useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { atom, useAtomValue } from "jotai";
import { loadable } from "jotai/utils";
import { Box, Button, Image, Input, InputLabel } from "@axelor/ui";

import { useSession } from "@/hooks/use-session";
import { request } from "@/services/client/client";
import { i18n } from "@/services/client/i18n";

import defaultLogo from "@/assets/axelor.svg";
import styles from "./login.module.scss";

interface ApplicationInfo {
  name?: string;
  logo?: string;
  copyright?: string;
  language?: string;
  callbackUrl?: string;
}
interface ClientInfo {
  name: string;
  icon?: string;
  title?: string;
}
interface PublicInfo {
  application: ApplicationInfo;
  clients?: ClientInfo[];
}

const publicInfoAtom = atom(async () => {
  const url = "ws/public/app/info";
  const response = await request({ url });
  const data = await response.json();
  return data as PublicInfo;
});

const loadablePublicInfoAtom = loadable(publicInfoAtom);

const LOGIN_ENDPOINT = "login";
const FORCE_CLIENT_PARAM = "force_client";
const HASH_LOCATION_PARAM = "hash_location";

const YEAR = new Date().getFullYear();

const t = i18n.get;

function loginWithClient(client: string) {
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
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState<string>();

  const location = useLocation();
  const { state, data, error, login } = useSession();

  const handleSubmit: FormEventHandler<HTMLFormElement> = useCallback(
    (event) => {
      event.preventDefault();
      const credentialsError = t("Wrong username or password");
      login({ username, password })
        .then(() => {
          if (error) {
            setErrorMessage(credentialsError);
          }
        })
        .catch((err: any) => {
          setErrorMessage(credentialsError);
        });
    },
    [error, login, username, password]
  );

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const error = params.get("error");
    if (error != null) {
      setErrorMessage(
        error || t("Sorry, something went wrong. Please try again later.")
      );
    }
  }, []);

  const publicInfo = useAtomValue<any>(loadablePublicInfoAtom);

  const [centralClients, setCentralClients] = useState<any[]>([]);
  const [application, setApplication] = useState<any>({});
  const {
    name = "Axelor",
    logo = defaultLogo,
    copyright = `&copy; 2005 - ${YEAR} Axelor. ${t("All Rights Reserved")}.`,
  } = application || {};

  useEffect(() => {
    const info = (publicInfo.data || {}) as PublicInfo;
    setCentralClients(info.clients || []);
    setApplication(info.application);
  }, [publicInfo.data]);

  const queryParams = new URLSearchParams(window.location.search);
  if (queryParams.get(FORCE_CLIENT_PARAM)) {
    window.location.href = `${LOGIN_ENDPOINT}?${queryParams}`;
    return null;
  }

  if (state === "loading") return null;
  if (data) {
    let { from } = location.state || { from: { pathname: "/" } };
    if (from === "/login") from = "/";
    return <Navigate to={from} />;
  }

  return (
    <Box as="main" ms="auto" me="auto" className={styles.main}>
      <Box
        className={styles.paper}
        shadow="2xl"
        d="flex"
        flexDirection="column"
        alignItems="center"
        p={3}
      >
        <Image className={styles.logo} src={logo} alt={name} />
        <Box as="form" w={100} onSubmit={handleSubmit}>
          <InputLabel htmlFor="username">{t("Username")}</InputLabel>
          <Input
            id="username"
            name="username"
            autoComplete="username"
            autoFocus
            mb={2}
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
          <InputLabel htmlFor="password">{t("Password")}</InputLabel>
          <Input
            name="password"
            type="password"
            id="password"
            autoComplete="current-password"
            mb={2}
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Box d="flex" alignItems="center">
            <Input type="checkbox" p={0} m={0} me={1} />
            <Box as="p" mb={0}>
              {t("Remember me")}
            </Box>
          </Box>
          {errorMessage && (
            <Box
              as="p"
              color="danger"
              mb={0}
              rounded
              p={1}
              pt={2}
              pb={2}
              className={styles.error}
            >
              <span>{errorMessage}</span>
            </Box>
          )}
          <Button type="submit" variant="primary" mt={2} w={100}>
            {t("Log in")}
          </Button>
        </Box>

        <Box as="form" w={100}>
          <CentralClients centralClients={centralClients} />
        </Box>
      </Box>

      <Box as="p" textAlign="center">
        {copyright}
      </Box>
    </Box>
  );
}

function CentralClients(props: { centralClients: any[] }) {
  const { centralClients } = props;

  if (!centralClients.length) {
    return null;
  }

  return (
    <>
      {centralClients.map((client) => {
        const { name, title, icon } = client;
        return (
          <CentralClient key={name} name={name} title={title} icon={icon} />
        );
      })}
    </>
  );
}

function CentralClient(props: { name: string; title?: string; icon?: string }) {
  const { name, title: _title, icon } = props;
  const title = _title || name;

  const handleClick = useCallback(
    (e: React.SyntheticEvent) => {
      e.preventDefault();
      loginWithClient(name);
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
        <Box>{t("Log in with {0}", title)}</Box>
      </Button>
    </Box>
  );
}
