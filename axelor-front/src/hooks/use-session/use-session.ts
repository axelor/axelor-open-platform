import { session, SessionInfo } from "@/services/client/session";
import { useCallback, useEffect, useState } from "react";
import { useAsync } from "../use-async";

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

  const logout = useCallback(async () => {
    window.location.href = "logout";
  }, []);

  return {
    state,
    data,
    error,
    login,
    logout,
  };
}
