import { useSession } from "@/hooks/use-session";
import { Box, Button } from "@axelor/ui";
import { Link } from "react-router-dom";

export function NavHeader() {
  const { logout } = useSession();

  return (
    <Box d="flex">
      <Box m="auto">Header</Box>
      <Box p={1}>
        <Button variant="primary" as={Link} to="/" me={1}>
          Home
        </Button>
        <Button variant="primary" as={Link} to="/about" me={1}>
          About
        </Button>
        <Button variant="primary" as={Link} to="/system" me={1}>
          System
        </Button>
        <Button variant="danger" onClick={(e) => logout()}>
          Logout
        </Button>
      </Box>
    </Box>
  );
}
