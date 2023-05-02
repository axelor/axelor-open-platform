import { useSession } from "@/hooks/use-session";
import { Box, Button, Image, Input, InputLabel } from "@axelor/ui";
import { FormEventHandler, useCallback, useState } from "react";

import logo from "@/assets/axelor.svg";
import { SessionInfo } from "@/services/client/session";

import styles from "./login-form.module.scss";

const YEAR = new Date().getFullYear();

export type LoginFormProps = {
  onSuccess: (info: SessionInfo) => void;
  shadow?: boolean;
};

export function LoginForm({ onSuccess, shadow }: LoginFormProps) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showError, setShowError] = useState(false);

  const { error, login } = useSession();

  const handleSubmit: FormEventHandler<HTMLFormElement> = useCallback(
    (event) => {
      event.preventDefault();
      login({ username, password }).then((info) => {
        if (error) {
          setShowError(true);
        } else {
          onSuccess(info);
        }
      });
    },
    [login, username, password, error, onSuccess]
  );

  return (
    <Box className={styles.container}>
      <Box
        className={styles.paper}
        shadow={shadow ? "2xl" : false}
        d="flex"
        flexDirection="column"
        alignItems="center"
        p={3}
      >
        <Image className={styles.logo} src={logo} alt="Logo" />
        <Box as="h4" fontWeight="normal" my={2}>
          Log In to Your Account
        </Box>
        <Box as="form" w={100} onSubmit={handleSubmit}>
          <InputLabel htmlFor="username">Username</InputLabel>
          <Input
            id="username"
            name="username"
            autoComplete="username"
            autoFocus
            mb={2}
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
          <InputLabel htmlFor="password">Password</InputLabel>
          <Input
            name="password"
            type="password"
            id="password"
            autoComplete="current-password"
            mb={2}
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Box d="flex" alignItems="center">
            <Input type="checkbox" p={0} m={0} me={1} />
            <Box as="p" mb={0}>
              Remember me
            </Box>
          </Box>
          {error && showError && (
            <Box
              as="p"
              color="danger"
              mb={0}
              rounded
              p={1}
              pt={2}
              pb={2}
              className={styles.error}
            >
              <span>Wrong username or password</span>
            </Box>
          )}
          <Button type="submit" variant="primary" mt={2} w={100}>
            Login
          </Button>
        </Box>
      </Box>
      <Box as="p" textAlign="center">
        &copy; 2005 - {YEAR} Axelor. All Rights Reserved.
      </Box>
    </Box>
  );
}
