import { useCallback, useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";

export function useRoute() {
  const location = useLocation();
  const navigate = useNavigate();

  const refs = useRef({ location, navigate });

  const redirect = useCallback((path: string) => {
    if (path !== refs.current.location.pathname) {
      refs.current.navigate(path);
    }
  }, []);

  useEffect(() => {
    refs.current = { location, navigate };
  }, [location, navigate]);

  return {
    location,
    redirect,
  };
}
