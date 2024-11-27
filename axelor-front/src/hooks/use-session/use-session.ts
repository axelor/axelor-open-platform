import { atom, useAtom } from "jotai";
import { useCallback, useEffect, useState } from "react";

import { SessionInfo, session } from "@/services/client/session";
import { useAsync } from "../use-async";

const redirectUrlAtom = atom<string | null>(null);

async function init() {
  let info = session.info;
  if (info) {
    return info;
  }
  return await session.init();
}

const login = session.login.bind(session);

export function useSession() {
  const { state, error } = useAsync(init, []);
  const [data, setData] = useState<SessionInfo | null>(session.info);

  useEffect(() => {
    return session.subscribe((info) => {
      setData(info);
    });
  }, []);

  const logout = useCallback(() => {
    session.logout();
  }, []);

  return {
    state,
    data,
    error,
    login,
    logout,
  };
}
