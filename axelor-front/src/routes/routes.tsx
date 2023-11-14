import {
  createHashRouter,
  Navigate,
  RouterProvider,
  useLocation,
} from "react-router-dom";

import { useSession } from "@/hooks/use-session";

import { ChangePassword } from "./change-password";
import { ErrorPage } from "./error";
import { Login } from "./login";
import { Root } from "./root";
import { View } from "./view";

function ProtectedRoute({ children }: { children: JSX.Element }) {
  const { state, data } = useSession();
  const location = useLocation();
  if (state === "loading") return null;
  if (data?.user) {
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
    path: "/change-password",
    element: <ChangePassword />,
  },
  {
    path: "/",
    element: (
      <ProtectedRoute>
        <Root />
      </ProtectedRoute>
    ),
    errorElement: <ErrorPage />,
    children: [
      {
        path: "/",
        element: <View />,
      },
      {
        path: "/ds/:action/:mode?/:id?",
        element: <View />,
      },
    ],
  },
]);

export function Routes() {
  return <RouterProvider router={router} />;
}
