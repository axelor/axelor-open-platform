import { session, SessionInfo } from "@/services/client/session";
import { useEffect, useState } from "react";
import { useAsync } from "../use-async";

async function init() {
  let info = session.info;
  if (info) {
    return info;
  }
  return await session.init();
}

const login = session.login.bind(session);
const logout = session.logout.bind(session);

export function useSession() {
  const { state, error } = useAsync(init, []);
  const [data, setData] = useState<SessionInfo | null>(session.info);

  useEffect(() => {
    return session.subscribe((info) => {
      setData(info);
    });
  }, []);

  return {
    state,
    data,
    error,
    login,
    logout,
  };
}
