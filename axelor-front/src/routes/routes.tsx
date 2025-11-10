import {
  createHashRouter,
  Navigate,
  RouterProvider,
  useLocation,
} from "react-router";
import { lazy, type JSX } from "react";

import { useSession } from "@/hooks/use-session";

import { ErrorPage } from "./error";
import { View } from "./view";

const Root = lazy(() => import("./root").then((m) => ({ default: m.Root })));
const Login = lazy(() => import("./login").then((m) => ({ default: m.Login })));
const MFA = lazy(() => import("./mfa").then((m) => ({ default: m.MFA })));
const ChangePassword = lazy(() =>
  import("./change-password").then((m) => ({ default: m.ChangePassword })),
);
const ForgotPassword = lazy(() =>
  import("./forgot-password").then((m) => ({ default: m.ForgotPassword })),
);
const ResetPassword = lazy(() =>
  import("./reset-password").then((m) => ({ default: m.ResetPassword })),
);
const Swagger = lazy(() =>
  import("./swagger").then((m) => ({ default: m.Swagger })),
);

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
    path: "/mfa",
    element: <MFA />,
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
