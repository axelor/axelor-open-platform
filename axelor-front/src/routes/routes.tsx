import { useSession } from "@/hooks/use-session";
import {
  createHashRouter,
  Navigate,
  RouterProvider,
  useLocation,
} from "react-router-dom";
import { About } from "./about";
import { Login } from "./login";
import { Profile } from "./profile";
import { Root } from "./root";
import { System } from "./system";

function ProtectedRoute({ children }: { children: JSX.Element }) {
  const { info, loading } = useSession();
  const location = useLocation();
  if (loading) return <div>Loading</div>;
  if (info) {
    return children;
  }
  return <Navigate to="/login" state={{ from: location }} />;
}

const router = createHashRouter([
  {
    path: "/login",
    element: <Login />,
  },
  {
    path: "/",
    element: (
      <ProtectedRoute>
        <Root />
      </ProtectedRoute>
    ),
    children: [
      {
        path: "/ds",
        element: <div></div>,
      },
      {
        path: "/about",
        element: <About />,
      },
      {
        path: "/profile",
        element: <Profile />,
      },
      {
        path: "/system",
        element: <System />,
      },
    ],
  },
]);

export function Routes() {
  return <RouterProvider router={router} />;
}
