import {
  FormEventHandler,
  useCallback,
  useId,
  useMemo,
  useRef,
  useState,
} from "react";
import { Navigate, Link as RouterLink, useLocation } from "react-router";

import { Alert, Box, Input, InputLabel, Select } from "@axelor/ui";

import { AppSignInLogo } from "@/components/app-logo/app-logo";
import { LoadingButton } from "@/components/loading-button";
import { useAppSettings } from "@/hooks/use-app-settings";
import { useSession } from "@/hooks/use-session";
import { request } from "@/services/client/client";
import { i18n } from "@/services/client/i18n";
import { getGenericErrorMessage } from "../reset-password/utils";

import styles from "./forgot-password.module.scss";

export function ForgotPassword() {
  const location = useLocation();
  const { state: locationState } = location ?? {};

  const session = useSession();
  const { copyright } = useAppSettings();
  const appInfo = session.data;
  const { authentication, application } = appInfo ?? {};
  const { resetPasswordEnabled } = application ?? {};

  const [emailAddress, setEmailAddress] = useState("");
  const emailAddressRef = useRef<HTMLInputElement>(null);
  const [alertMessage, setAlertMessage] = useState("");
  const [alertError, setError] = useState(locationState?.error ?? "");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const errorId = useId();
  const successId = useId();
  const emailId = useId();

  const [tenantId, setTenantId] = useState(
    locationState?.tenantId ?? authentication?.tenant,
  );

  const tenants = useMemo(() => {
    const val = authentication?.tenants ?? {};
    return Object.entries(val).map(([name, title]) => ({
      name,
      title,
    }));
  }, [authentication?.tenants]);

  const tenant = useMemo(() => {
    return tenants?.find((x) => x.name === tenantId) ?? tenants?.[0];
  }, [tenantId, tenants]);

  const handleTenantChange = useCallback(
    (value?: { name: string; title: string } | null) => {
      const id = value?.name ?? "";
      if (id) {
        setTenantId(id);
      }
    },
    [],
  );

  const hasTenantSelect = tenants.length > 0;

  const handleSubmit: FormEventHandler<HTMLFormElement> = useCallback(
    async (event) => {
      event.preventDefault();
      setIsSubmitting(true);

      const headers =
        hasTenantSelect && tenantId ? { "X-Tenant-ID": tenantId } : undefined;

      const response = await request({
        url: "ws/public/password-reset/forgot",
        method: "POST",
        body: {
          email: emailAddress,
        },
        headers,
      });

      setIsSubmitting(false);

      if (!response.ok) {
        try {
          const { message } = await response.json();
          setError(message || getGenericErrorMessage());
        } catch {
          setError(getGenericErrorMessage());
        }
        return;
      }

      setAlertMessage(
        i18n.get(
          "If an account exists with this email address, you will receive a password reset link.",
        ),
      );
    },
    [emailAddress, hasTenantSelect, tenantId],
  );

  if (session.state === "loading" || session.state === "hasError") {
    return null;
  }

  if (!resetPasswordEnabled) {
    return <Navigate to="/" />;
  }

  return (
    <Box
      as="main"
      mt={5}
      ms="auto"
      me="auto"
      className={styles.main}
      data-testid="forgot-password-page"
    >
      <Box className={styles.container}>
        <Box
          className={styles.paper}
          shadow={"2xl"}
          d="flex"
          flexDirection="column"
          alignItems="center"
          p={3}
        >
          <AppSignInLogo className={styles.logo} data-testid="logo" />
          <Box as="legend" style={{ textWrap: "balance" }} data-testid="title">
            {i18n.get("Reset your password")}
          </Box>
          {
            <Box as="form" w={100} onSubmit={handleSubmit} data-testid="form">
              {alertMessage ? (
                <Alert
                  mb={1}
                  p={2}
                  variant="info"
                  id={successId}
                  role="status"
                  data-testid="success"
                >
                  {alertMessage}
                </Alert>
              ) : (
                <>
                  {hasTenantSelect && (
                    <Box mb={4} data-testid="field-tenant">
                      <InputLabel htmlFor="tenant" data-testid="label">
                        {i18n.get("Tenant")}
                      </InputLabel>
                      <Select
                        id="tenant"
                        multiple={false}
                        value={tenant}
                        options={tenants}
                        optionKey={(x) => x.name}
                        optionLabel={(x) => x.title}
                        onChange={handleTenantChange}
                        clearIcon={false}
                        data-testid="input"
                      />
                    </Box>
                  )}
                  <Box
                    className={styles.inputContainer}
                    data-testid="field-email"
                  >
                    <InputLabel htmlFor="emailAddress" data-testid="label">
                      {i18n.get("Email address")}
                    </InputLabel>
                    <Input
                      ref={emailAddressRef}
                      id={emailId}
                      name="emailAddress"
                      type="email"
                      autoFocus
                      mb={3}
                      value={emailAddress}
                      onChange={(e) => {
                        setEmailAddress(e.target.value);
                      }}
                      spellCheck="false"
                      aria-required="true"
                      aria-describedby={alertError ? errorId : undefined}
                      data-testid="input"
                    />
                  </Box>

                  {alertError && (
                    <Alert
                      mt={2}
                      mb={1}
                      p={2}
                      variant="danger"
                      id={errorId}
                      role="alert"
                      data-testid="error"
                    >
                      {alertError}
                    </Alert>
                  )}

                  <LoadingButton
                    type="submit"
                    variant="primary"
                    d="flex"
                    justifyContent="center"
                    gap={4}
                    mt={2}
                    w={100}
                    loading={isSubmitting}
                    disabled={!emailAddress}
                    data-testid="btn-reset-password"
                    aria-label={i18n.get("Reset password")}
                  >
                    {i18n.get("Reset password")}
                  </LoadingButton>
                </>
              )}
            </Box>
          }

          <Box d="flex" justifyContent="center" mt={3}>
            <RouterLink
              to="/"
              className={styles.navLink}
              data-testid="link-back-to-signin"
            >
              {i18n.get("Go back to sign in page")}
            </RouterLink>
          </Box>
        </Box>
        <Box as="p" textAlign="center">
          {copyright}
        </Box>
      </Box>
    </Box>
  );
}
