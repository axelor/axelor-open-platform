import { navigate } from "@/routes";
import * as session from "@/services/client/session";
import { SessionInfo } from "@/services/client/session";
import { atom, useAtom } from "jotai";

import { useState } from "react";
import { useAsyncEffect } from "../use-async-effect";

type LoginParams = Parameters<typeof session.login>[0];

const infoAtom = atom<SessionInfo | null>(null);
const errorAtom = atom<any>(null);

const loginAtom = atom(null, async (get, set, args: LoginParams) => {
  await session
    .login(args)
    .then((x) => (x.user ? x : null))
    .then((x) => set(infoAtom, x))
    .catch((x) => set(errorAtom, x));
});

const logoutAtom = atom(null, async (get, set) => {
  try {
    await session.logout();
  } catch (e) {
  } finally {
    set(errorAtom, null);
    navigate(0);
  }
});

export function useSession() {
  const [info, setInfo] = useAtom(infoAtom);
  const [error, setError] = useAtom(errorAtom);
  const [loading, setLoading] = useState(false);

  const [, login] = useAtom(loginAtom);
  const [, logout] = useAtom(logoutAtom);

  useAsyncEffect(async () => {
    if (loading || info) return;
    setLoading(true);
    await session
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
