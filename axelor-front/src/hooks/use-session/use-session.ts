import * as session from "@/services/client/session";
import { SessionInfo } from "@/services/client/session";
import { atom, useAtom } from "jotai";

import { useCallback, useEffect, useState } from "react";

const infoAtom = atom<SessionInfo | null>(null);

export function useSessionInfo() {
  const [info, setInfo] = useAtom(infoAtom);
  const [error, setError] = useState<any>(null);
  const [loading, setLoading] = useState(!info);

  useEffect(() => {
    if (info) return;
    // this code will execute twice during development
    // this is because of the react strict-mode
    setLoading(true);
    session
      .info()
      .then((x) => (x.user ? x : null))
      .then((x) => setInfo(x))
      .catch((x) => setError(x))
      .finally(() => setLoading(false));
  }, []);

  return {
    info,
    error,
    loading,
  };
}

export function useSession() {
  const [info, setInfo] = useAtom(infoAtom);
  const [error, setError] = useState<any>(null);
  const [loading, setLoading] = useState(!info);

  const login = useCallback((username: string, password: string) => {
    setLoading(true);
    session
      .login({ username, password })
      .then((x) => (x.user ? x : null))
      .then((x) => setInfo(x))
      .catch((x) => setError(x))
      .finally(() => setLoading(false));
  }, []);

  const logout = useCallback(() => {
    setInfo(null);
    setLoading(true);
    session.logout().finally(() => setLoading(false));
  }, []);

  return {
    info,
    error,
    login,
    logout,
    loading,
  };
}
