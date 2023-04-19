import { useSession } from "@/hooks/use-session";
import { Box, Button, Image, Input, InputLabel } from "@axelor/ui";
import {
  FormEventHandler,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";
import { atom, useAtom, useAtomValue } from "jotai";

import logo from "@/assets/axelor.svg";
import { Navigate, useLocation } from "react-router-dom";
import styles from "./login.module.scss";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { request } from "@/services/client/client";
import { i18n } from "@/services/client/i18n";
import { loadable } from "jotai/utils";

interface ApplicationInfo {
  name?: string;
  copyright?: string;
  logo?: string;
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

const SERVER_ERROR = t("Sorry, something went wrong. Please try again later.");
const CREDENTIALS_ERROR = t("Wrong username or password");

function loginWithClient(client: string): void {
  const currentParams = new URLSearchParams(window.location.search);
  const forceClient = client || currentParams.get(FORCE_CLIENT_PARAM);
  const hashLocation =
    window.location.hash || currentParams.get(HASH_LOCATION_PARAM);

  const queryParams = new URLSearchParams();

  if (forceClient) {
    queryParams.append(FORCE_CLIENT_PARAM, forceClient);
  }

  if (hashLocation) {
    queryParams.append(HASH_LOCATION_PARAM, hashLocation);
  }

  window.location.href = `${LOGIN_ENDPOINT}?${queryParams}`;
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
      login({ username, password })
        .then(() => {
          if (error) {
            setErrorMessage(CREDENTIALS_ERROR);
          }
        })
        .catch((err: any) => {
          setErrorMessage(CREDENTIALS_ERROR);
        });
    },
    [error, login, username, password]
  );

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const error = params.get("error");
    if (error != null) {
      setErrorMessage(error || SERVER_ERROR);
    }
  }, []);

  const publicInfo = useAtomValue<any>(loadablePublicInfoAtom);

  const [centralClients, setCentralClients] = useState<any[]>([]);
  const [application, setApplication] = useState<any>({});
  const {
    name = "Axelor",
    logo = "img/axelor.png",
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
        <Image className={styles.logo} src={logo} alt="Logo" />
        <Box as="h4" fontWeight="normal" my={2}>
          Log In to Your Account
        </Box>
        <Box as="form" w={100} onSubmit={handleSubmit}>
          <InputLabel htmlFor="username">Username</InputLabel>
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
          <InputLabel htmlFor="password">Password</InputLabel>
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
              Remember me
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
            Login
          </Button>
        </Box>

        <Box as="form" w={100}>
          <CentralClients centralClients={centralClients} />
        </Box>

      </Box>
      <Box as="p" textAlign="center">
        &copy; 2005 - {YEAR} Axelor. All Rights Reserved.
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
      {centralClients.map(client => {
        const { name, title, icon } = client;
        return <CentralClient key={name} name={name} title={title} icon={icon} />;
      })}
    </>
  );
}

function CentralClient(props: { name: string; title?: string; icon?: string }) {
  const { name, title: _title, icon } = props;
  const title = _title || name;
  const loginWith = useMemo(() => t('Log in with {0}', title), [title, t]);

  const handleClick = useCallback(
    (e: React.SyntheticEvent) => {
      e.preventDefault();
      loginWithClient(name);
    },
    [name],
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
        <Box>{loginWith}</Box>
      </Button>
    </Box>
  );
}
