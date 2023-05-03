import {
  createHashRouter,
  Navigate,
  RouterProvider,
  useLocation,
} from "react-router-dom";

import { useSession } from "@/hooks/use-session";

import { Login } from "./login";
import { Profile } from "./profile";
import { Root } from "./root";
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
        path: "/profile",
        element: <Profile />,
      },
    ],
  },
]);

export function Routes() {
  return <RouterProvider router={router} />;
}
