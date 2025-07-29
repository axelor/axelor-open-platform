import { FormEventHandler, useCallback, useMemo, useState } from "react";
import { Navigate, useLocation } from "react-router";

import { useAsync } from "@/hooks/use-async";
import { request } from "@/services/client/client";
import { i18n } from "@/services/client/i18n";
import { ChangePassword } from "../change-password";
import { getGenericErrorMessage } from "./utils";

export function ResetPassword() {
  const location = useLocation();
  const [token, tenantId] = useMemo(() => {
    const queryParams = new URLSearchParams(location.search);
    return [queryParams.get("token"), queryParams.get("tenant")];
  }, [location.search]);
  const [errorMessage, setErrorMessage] = useState<string | undefined>();
  const [done, setDone] = useState(false);

  const info = useAsync<{
    message?: string;
    passwordPattern?: string;
    passwordPatternTitle?: string;
  }>(async () => {
    if (!token) {
      return {
        message: i18n.get("Your password reset token is invalid or expired."),
      };
    }

    const headers = tenantId ? { "X-Tenant-ID": tenantId } : undefined;

    const response = await request({
      url: "ws/public/password-reset/verify",
      method: "POST",
      body: {
        token,
      },
      headers,
    });

    if (!response.ok) {
      try {
        const { message } = await response.json();
        return {
          message: message || getGenericErrorMessage(),
        };
      } catch {
        return {
          message: getGenericErrorMessage(),
        };
      }
    }

    return await response.json();
  }, [token, tenantId]);

  const handleSubmit = useCallback<FormEventHandler<HTMLFormElement>>(
    async (event) => {
      event.preventDefault();

      const form = event.target as HTMLFormElement;
      const password =
        form.querySelector<HTMLInputElement>("[name=newPassword]")?.value;

      const headers = tenantId ? { "X-Tenant-ID": tenantId } : undefined;

      const response = await request({
        url: "ws/public/password-reset/reset",
        method: "POST",
        body: {
          token,
          password,
        },
        headers,
      });

      if (!response.ok) {
        try {
          const { message } = await response.json();
          setErrorMessage(message || getGenericErrorMessage());
        } catch {
          setErrorMessage(getGenericErrorMessage());
        }
        return;
      }

      setDone(true);
    },
    [token, tenantId],
  );

  if (info.state === "loading") {
    return null;
  }

  const { message, passwordPattern, passwordPatternTitle } = info.data || {};
  const infoMessage = (info.state === "hasError" && info.error) || message;

  if (infoMessage) {
    return <Navigate to="/forgot-password" state={{ error: infoMessage }} />;
  }

  if (done) {
    return (
      <Navigate
        to="/login"
        state={{
          message: i18n.get("Your password has been successfully changed."),
        }}
      />
    );
  }

  return (
    <ChangePassword
      onSubmit={handleSubmit}
      passwordPattern={passwordPattern}
      passwordPatternTitle={passwordPatternTitle}
      error={errorMessage}
    />
  );
}
