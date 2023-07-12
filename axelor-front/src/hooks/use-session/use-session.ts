import { atom, useAtom } from "jotai";
import { useCallback, useEffect, useState } from "react";

import { AppInfo, SessionInfo, session } from "@/services/client/session";
import { useAsync } from "../use-async";

const redirectUrlAtom = atom<string | null>(null);

async function init() {
  let info = session.appInfo;
  if (info) {
    return info;
  }
  return await session.init();
}

const login = session.login.bind(session);

interface MultiInfo {
  sessionInfo: SessionInfo | null;
  appInfo: AppInfo | null;
}

export function useSession() {
  const { state, error } = useAsync(init, []);
  const [data, setData] = useState<MultiInfo>({
    sessionInfo: session.info,
    appInfo: session.appInfo,
  });
  const [redirectUrl, setRedirectUrl] = useAtom(redirectUrlAtom);

  useEffect(() => {
    return session.subscribe((info) => {
      setData({
        sessionInfo: info?.type === "session" ? info : null,
        appInfo: info,
      });
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
    data: data.sessionInfo,
    appData: data.appInfo,
    error,
    login,
    logout,
    redirectUrl,
  };
}
