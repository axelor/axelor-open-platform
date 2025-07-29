import {
  createHashRouter,
  Navigate,
  RouterProvider,
  useLocation,
} from "react-router";

import { useSession } from "@/hooks/use-session";
import { Swagger } from "@/routes/swagger";

import { ChangePassword } from "./change-password";
import { ErrorPage } from "./error";
import { ForgotPassword } from "./forgot-password";
import { Login } from "./login";
import { ResetPassword } from "./reset-password";
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
    path: "/forgot-password",
    element: <ForgotPassword />,
  },
  {
    path: "/reset-password",
    element: <ResetPassword />,
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
      {
        path: "/api-documentation",
        element: <Swagger />,
      },
    ],
  },
]);

export function Routes() {
  return <RouterProvider router={router} />;
}
