import { session, SessionInfo } from "@/services/client/session";
import { atom, useAtom } from "jotai";
import { useCallback, useEffect, useState } from "react";
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
  const [redirectUrl, setRedirectUrl] = useAtom(redirectUrlAtom);

  useEffect(() => {
    return session.subscribe((info) => {
      setData(info);
    });
  }, []);

  const logout = useCallback(async () => {
    try {
      const { redirectUrl: url } = await session.logout();
      setRedirectUrl(url ?? "");
    } catch (e) {
      console.error(e);
    }
  }, [setRedirectUrl]);

  return {
    state,
    data,
    error,
    login,
    logout,
    redirectUrl,
  };
}
