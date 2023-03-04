import { fireEvent, render } from "@testing-library/react";
import { vi } from "vitest";
import { useSession } from "./use-session";

vi.mock("../../services/client/session", () => {
  let user: string | null = null;

  const info = async () => {
    return user
      ? {
          app: {
            name: "Axelor",
          },
          user: {
            name: user,
          },
        }
      : Promise.reject(401);
  };

  const login = async ({
    username,
    password,
  }: {
    username: string;
    password: string;
  }) => {
    if (username === password) {
      user = username;
    }
    return await info();
  };

  const logout = async () => {
    user = null;
  };

  return {
    info,
    login,
    logout,
  };
});

afterEach(() => {
  vi.resetAllMocks();
});

const App = () => {
  const { loading, info } = useSession();
  if (loading) {
    return <div data-testid="loading">Loading...</div>;
  }
  if (info) {
    return (
      <div data-testid="logged-in">
        <h4>{info.user.name}</h4>
        <LogoutButton />
      </div>
    );
  }
  return (
    <div data-testid="logged-out">
      <h4>Please login</h4>
      <LoginButton />
    </div>
  );
};

const LoginButton = () => {
  const { login } = useSession();
  return (
    <button data-testid="login" onClick={() => login("test", "test")}>
      Login
    </button>
  );
};

const LogoutButton = () => {
  const { logout } = useSession();
  return (
    <button data-testid="logout" onClick={() => logout()}>
      Logout
    </button>
  );
};

describe("use-session tests", async () => {
  it("should handle session", async () => {
    const res = render(<App />);

    const login = await res.findByTestId("login");
    expect(login).toHaveTextContent("Login");

    fireEvent.click(login);

    const logout = await res.findByTestId("logout");
    expect(logout).toHaveTextContent("Logout");
  });
});
