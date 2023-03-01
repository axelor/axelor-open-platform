import * as session from "@/services/client/session";
import { SessionInfo } from "@/services/client/session";
import { atom, useAtom } from "jotai";

import { useCallback, useEffect, useState } from "react";

const infoAtom = atom<SessionInfo | null>(null);

export function useSession() {
  const [info, setInfo] = useAtom(infoAtom);
  const [error, setError] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  const login = useCallback(async (username: string, password: string) => {
    await session
      .login({ username, password })
      .then((x) => (x.user ? x : null))
      .then((x) => setInfo(x))
      .catch((x) => setError(x));
  }, []);

  const logout = useCallback(async () => {
    try {
      await session.logout();
    } catch (e) {
    } finally {
      setInfo(null);
    }
  }, []);

  useEffect(() => {
    if (loading || info) return;
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
    login,
    logout,
    loading,
  };
}
