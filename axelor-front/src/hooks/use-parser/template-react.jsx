import {
  Alert,
  AlertHeader,
  AlertLink,
  Badge,
  Box,
  Button,
  ButtonGroup,
  Divider,
  Image,
  Link,
  List,
  ListItem,
  Panel,
  Popper,
  Scrollable,
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableFoot,
  TableHead,
  TableRow,
} from "@axelor/ui";

import { BootstrapIcon as Icon } from "@axelor/ui/icons/bootstrap-icon";

import { useMemo } from "react";
import { parseSafe } from "./parser";

const COMPONENTS = {
  Box,
  Badge,
  Alert,
  AlertHeader,
  AlertLink,
  Button,
  ButtonGroup,
  Divider,
  Image,
  Link,
  List,
  ListItem,
  Panel,
  Popper,
  Scrollable,
  Table,
  TableBody,
  TableHead,
  TableFoot,
  TableRow,
  TableCell,
  TableCaption,
  Icon,
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

export function processReactTemplate(template) {
  const render = parseSafe(template);
  const ReactComponent = ({ context }) => {
    const ctx = useMemo(() => createContext(context), [context]);
    return render(ctx);
  };
  return ReactComponent;
}
