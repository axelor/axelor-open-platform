import {
  createHashRouter,
  Navigate,
  RouterProvider,
  useLocation,
} from "react-router-dom";

import { useSession } from "@/hooks/use-session";

import { About } from "./about";
import { Login } from "./login";
import { Profile } from "./profile";
import { Root } from "./root";
import { System } from "./system";
import { View } from "./view";

function ProtectedRoute({ children }: { children: JSX.Element }) {
  const { state, data } = useSession();
  const location = useLocation();
  if (state === "loading") return <div>Loading</div>;
  if (data) {
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
        path: "/",
        element: <View />,
      },
      {
        path: "/ds/:action/:mode?/:id?",
        element: <View />,
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
