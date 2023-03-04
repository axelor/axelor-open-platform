import { navigate } from "@/routes";
import { useCallback, useEffect, useRef } from "react";
import { useLocation } from "react-router-dom";

export function useRoute() {
  const location = useLocation();

  const refs = useRef({ location });

  const redirect = useCallback((path: string) => {
    if (path !== refs.current.location.pathname) {
      navigate(path);
    }
  }, []);

  useEffect(() => {
    refs.current = { location };
  }, [location]);

  return {
    location,
    redirect,
  };
}
