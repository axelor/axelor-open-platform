import { useMemo } from "react";
import { ErrorBoundary } from "react-error-boundary";

import {
  Alert,
  AlertHeader,
  AlertLink,
  Badge,
  Barcode,
  Box,
  Button,
  ButtonGroup,
  CircularProgress,
  Divider,
  Image,
  LinearProgress,
  List,
  ListItem,
  Panel,
  Popper,
  QrCode,
  Rating,
  Scrollable,
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableFoot,
  TableHead,
  TableRow,
} from "@axelor/ui";

import { Icon } from "@/components/icon";
import { TextLink as Link } from "@/components/text-link";
import { CUSTOM_COMPONENTS } from "@/views/custom";

import { legacyClassNames } from "@/styles/legacy";
import { LoadingCache } from "@/utils/cache";
import { parseSafe } from "./parser";

const COMPONENTS = {
  Alert,
  AlertHeader,
  AlertLink,
  Badge,
  Barcode,
  Box,
  Button,
  ButtonGroup,
  CircularProgress,
  Divider,
  Image,
  LinearProgress,
  Link,
  List,
  ListItem,
  Panel,
  Popper,
  QrCode,
  Rating,
  Scrollable,
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableFoot,
  TableHead,
  TableRow,
  Icon,
  legacyClassNames,
  ...CUSTOM_COMPONENTS,
};

function createContext(context) {
  return new Proxy(context, {
    get(target, p, receiver) {
      return p in COMPONENTS ? COMPONENTS[p] : Reflect.get(target, p, receiver);
    },
    set(target, p, newValue, receiver) {
      Reflect.set(target, p, newValue);
    },
  });
}

const cache = new LoadingCache();

export function processReactTemplate(template) {
  return cache.get(template, () => {
    try {
      const render = parseSafe(template);
      const ReactComponent = ({ context }) => {
        const ctx = useMemo(() => createContext(context), [context]);
        return render(ctx);
      };
      return ({ context }) => (
        <ErrorBoundary fallback={<Box />}>
          <ReactComponent context={context} />
        </ErrorBoundary>
      );
    } catch (err) {
      console.error("React template error : ", err);
      return () => null;
    }
  });
}
