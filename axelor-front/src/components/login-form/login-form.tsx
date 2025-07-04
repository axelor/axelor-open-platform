import { useCallback, useMemo, useRef, useState } from "react";
import { useLocation } from "react-router";

import {
  AdornedInput,
  Alert,
  Box,
  Button,
  InputLabel,
  Select,
} from "@axelor/ui";
import { BootstrapIcon } from "@axelor/ui/icons/bootstrap-icon";

import { LoadingButton } from "../loading-button";
import { useAppSettings } from "@/hooks/use-app-settings";
import { useRoute } from "@/hooks/use-route";
import { useSession } from "@/hooks/use-session";
import {
  CLIENT_NAME_PARAM,
  FORM_CLIENT_NAME,
  requestLogin,
} from "@/routes/login";
import { i18n } from "@/services/client/i18n";
import { mfaSession } from "@/services/client/mfa";
import { SessionInfo, SignInButtonType } from "@/services/client/session";
import { sanitize } from "@/utils/sanitize";
import { AppSignInLogo } from "../app-logo/app-logo";
import { Icon } from "../icon";
import { TextLink as Link } from "../text-link";

import styles from "./login-form.module.scss";

export type LoginFormProps = {
  shadow?: boolean;
  error?: string;
  children?: React.ReactNode;
  onSuccess?: (info: SessionInfo) => void;
  onRequireMFA?: (state: Record<string, unknown>) => void;
};

export function LoginForm({
  error,
  shadow,
  children,
  onSuccess,
  onRequireMFA,
}: LoginFormProps) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showError, setShowError] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const isPage = !onSuccess;
  const location = useLocation();
  const locationState = location.state;
  const { navigate } = useRoute();

  const session = useSession();
  const { copyright } = useAppSettings();
  const appInfo = session.data;
  const { authentication, application } = appInfo ?? {};
  const { signIn } = application ?? {};

  const defaultClient = authentication?.defaultClient;

  const [tenantId, setTenantId] = useState(authentication?.tenant);

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

  const handleSubmit: (event: React.SyntheticEvent) => Promise<void> =
    useCallback(
      async (event) => {
        event.preventDefault();
        setIsSubmitting(true);

        // Need to specify client if default client is not form client.
        const params =
          defaultClient && defaultClient !== FORM_CLIENT_NAME
            ? new URLSearchParams({
                [CLIENT_NAME_PARAM]: FORM_CLIENT_NAME,
              })
            : undefined;

        try {
          // reset mfa session before login
          mfaSession.reset(username);

          const info = await session.login(
            { username, password },
            { params, tenant: hasTenantSelect ? tenantId : undefined },
          );
          const { user, route } = info;

          if (route) {
            const { path, state } = route;

            const routeState = {
              ...state,
              username,
              ...(hasTenantSelect && { tenant: tenantId }),
            };

            if (onRequireMFA && path === "/mfa") {
              onRequireMFA({ ...routeState, params });
            } else {
              navigate(
                { pathname: path, search: params?.toString() },
                {
                  state: {
                    ...locationState,
                    route: routeState,
                  },
                },
              );
            }
          } else if (user) {
            onSuccess?.(info);
          } else {
            setShowError(true);
          }
        } catch (e) {
          setShowError(true);
        } finally {
          setIsSubmitting(false);
        }
      },
      [
        defaultClient,
        session,
        username,
        password,
        hasTenantSelect,
        tenantId,
        navigate,
        locationState,
        onSuccess,
        onRequireMFA,
      ],
    );

  const { resetPasswordEnabled } = appInfo?.application || {};
  const {
    title: signInTitle,
    footer: signInFooter,
    fields: signInFields,
    buttons: signInButtons,
  } = signIn ?? {};

  const {
    username: usernameField = {},
    password: passwordField = {},
    tenant: tenantField = {},
  } = signInFields ?? {};

  const usernameFieldIcon =
    usernameField.icon !== "none" ? usernameField.icon : undefined;
  const passwordFieldIcon =
    passwordField.icon !== "none" ? passwordField.icon : undefined;

  const usernameRef = useRef<HTMLInputElement>(null);

  const formButtons = useMemo(() => {
    const { submit, ...buttonsRest } = signInButtons ?? {};
    const buttons = {
      submit: {
        title: i18n.get("Sign in"),
        order: 0,
        submitting: isSubmitting,
        onSubmit: handleSubmit,
        ...submit,
      },
      ...buttonsRest,
    };

    return isPage
      ? Object.values(buttons)
          .map((button) => {
            const { link, ...buttonRest } = button;
            return {
              ...(link?.includes(":username") && {
                usernameRef,
              }),
              link,
              ...buttonRest,
            };
          })
          .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
      : [buttons.submit];
  }, [signInButtons, isSubmitting, handleSubmit, isPage]);

  const handleForgotPassword = useCallback<
    React.MouseEventHandler<HTMLAnchorElement>
  >(
    (event) => {
      event.preventDefault();
      event.stopPropagation();
      navigate("/forgot-password", { state: { tenantId } });
    },
    [tenantId, navigate],
  );

  if (session.state === "loading" || session.state === "hasError") {
    return null;
  }

  const currentClient = session.data?.authentication?.currentClient;

  if (currentClient) {
    requestLogin(currentClient);
    return <Reconnecting />;
  }

  let errorText;
  if (showError) {
    errorText = i18n.get("Wrong username or password");
  } else if (error != null || session.error === 500) {
    errorText =
      error || i18n.get("Sorry, something went wrong. Please try again later.");
  }

  return (
    <Box className={styles.container}>
      <Box
        className={styles.paper}
        shadow={shadow ? "2xl" : false}
        d="flex"
        flexDirection="column"
        alignItems="center"
        p={3}
        mb={3}
      >
        <AppSignInLogo className={styles.logo} />
        {isPage && signInTitle && (
          <Box
            d="flex"
            justifyContent="center"
            mt={3}
            dangerouslySetInnerHTML={{
              __html: sanitize(signInTitle),
            }}
          />
        )}
        <Box as="form" w={100} onSubmit={handleSubmit} mt={3}>
          {isPage && hasTenantSelect && (
            <Box mb={4}>
              {tenantField.showTitle !== false && (
                <InputLabel htmlFor="tenant">
                  {tenantField.title ? tenantField.title : i18n.get("Tenant")}
                </InputLabel>
              )}
              <Select
                id="tenant"
                multiple={false}
                value={tenant}
                options={tenants}
                optionKey={(x) => x.name}
                optionLabel={(x) => x.title}
                onChange={handleTenantChange}
                clearIcon={false}
              />
            </Box>
          )}
          <Box>
            {usernameField.showTitle !== false && (
              <InputLabel htmlFor="username">
                {usernameField.title
                  ? usernameField.title
                  : i18n.get("Username")}
              </InputLabel>
            )}
            <AdornedInput
              ref={usernameRef}
              id="username"
              name="username"
              autoComplete="username"
              autoFocus
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck="false"
              placeholder={usernameField.placeholder}
              startAdornment={
                usernameFieldIcon ? (
                  <Icon icon={usernameFieldIcon} />
                ) : undefined
              }
            />
          </Box>
          <Box mt={3}>
            {passwordField.showTitle !== false && (
              <InputLabel htmlFor="password">
                {passwordField.title
                  ? passwordField.title
                  : i18n.get("Password")}
              </InputLabel>
            )}
            <AdornedInput
              name="password"
              type={showPassword ? "text" : "password"}
              id="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              spellCheck="false"
              placeholder={passwordField.placeholder}
              startAdornment={
                passwordFieldIcon ? (
                  <Icon icon={passwordFieldIcon} />
                ) : undefined
              }
              endAdornment={
                <Button
                  onClick={() => setShowPassword((value) => !value)}
                  title={
                    showPassword
                      ? i18n.get("Hide password")
                      : i18n.get("Show password")
                  }
                >
                  <BootstrapIcon icon={showPassword ? "eye-slash" : "eye"} />
                </Button>
              }
            />
            {isPage && resetPasswordEnabled && (
              <Box d="flex" justifyContent="flex-end" mt={1} mb={1}>
                <Link href="#" onClick={handleForgotPassword} underline={false}>
                  {i18n.get("Forgot password?")}
                </Link>
              </Box>
            )}
          </Box>
          {errorText && (
            <Alert mt={3} mb={1} p={2} variant="danger">
              {errorText}
            </Alert>
          )}
          <Box mt={4}>
            {formButtons.map((button, index) => (
              <LoginFormButton key={index} {...button} />
            ))}
          </Box>
        </Box>
      </Box>
      {isPage && signInFooter && (
        <Box
          d="flex"
          justifyContent="center"
          dangerouslySetInnerHTML={{ __html: sanitize(signInFooter) }}
        ></Box>
      )}
      {children}
      <Box mt={3} as="p" textAlign="center">
        {copyright}
      </Box>
    </Box>
  );
}

function Reconnecting() {
  return (
    <Box className={styles.container}>
      <Box
        className={styles.paper}
        d="flex"
        flexDirection="column"
        alignItems="center"
        p={3}
      >
        <Box as="h4" fontWeight="normal" my={2}>
          {i18n.get("Reconnecting…")}
        </Box>
      </Box>
    </Box>
  );
}

function LoginFormButton({
  type,
  title,
  icon,
  link,
  variant,
  onSubmit,
  usernameRef,
  submitting,
}: SignInButtonType & {
  onSubmit?: (e: React.SyntheticEvent) => Promise<void>;
  usernameRef?: React.RefObject<HTMLInputElement>;
  submitting?: boolean;
}) {
  const handleClick = useCallback(
    (e: React.SyntheticEvent) => {
      if (onSubmit) {
        return onSubmit(e);
      }

      if (link) {
        e.preventDefault();
        const username = usernameRef?.current?.value;
        window.location.href = username
          ? link.replace(/:username\b/, encodeURIComponent(username))
          : link;
      }
    },
    [onSubmit, link, usernameRef],
  );

  if (type === "link") {
    // Submit button is needed for submitting the form on Enter.
    return (
      <>
        {onSubmit && <Button type="submit" style={{ display: "none" }} />}
        <Link
          href="#"
          onClick={handleClick}
          d="flex"
          underline={false}
          mt={2}
          w={100}
          gap={4}
        >
          {icon && <Icon icon={icon} className={styles.icon} />}
          {title}
        </Link>
      </>
    );
  }

  return (
    <LoadingButton
      type={onSubmit ? "submit" : "button"}
      onClick={handleClick}
      variant={variant ?? "primary"}
      loading={submitting}
      d="flex"
      justifyContent="center"
      mt={2}
      w={100}
      gap={4}
    >
      {icon && <Icon icon={icon} className={styles.icon} />}
      {title}
    </LoadingButton>
  );
}
