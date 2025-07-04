import { useCallback, useMemo } from "react";
import { Navigate, useLocation } from "react-router";
import { Box } from "@axelor/ui";

import { AlertsProvider } from "@/components/alerts";
import { useSession } from "@/hooks/use-session";
import { useRoute } from "@/hooks/use-route";
import { MFAForm } from "@/components/mfa-form";
import styles from "./mfa.module.scss";

export function MFA() {
  const location = useLocation();
  const session = useSession();
  const { navigate } = useRoute();
  const { route } = location.state ?? {};

  const params = useMemo(
    () => new URLSearchParams(location.search),
    [location.search],
  );

  const routeState = useMemo(() => {
    if (params.size > 0) {
      const username = params.get("username");
      const methods = params.getAll("methods");
      const emailRetryAfter = params.get("emailRetryAfter");
      return {
        username,
        methods,
        emailRetryAfter,
        ...route,
      };
    }
    return route;
  }, [params, route]);

  const handleBackToLogin = useCallback(() => {
    navigate("/login");
  }, [navigate]);

  if (session?.data?.user) {
    let { from } = location.state || { from: { pathname: "/" } };
    if (from === "/login") from = "/";
    return <Navigate to={from} />;
  }

  if (!routeState?.username) {
    return <Navigate to="/login" state={{}} />;
  }

  return (
    <Box as="main" mt={5} ms="auto" me="auto" className={styles.main}>
      <MFAForm
        shadow
        state={{ ...routeState, params }}
        onBackToLogin={handleBackToLogin}
      />
      <AlertsProvider />
    </Box>
  );
}
