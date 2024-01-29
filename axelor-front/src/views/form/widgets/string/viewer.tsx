import { Box, Button, Input } from "@axelor/ui";
import { useCallback, useEffect, useState, useMemo } from "react";

import { useAfterActions } from "../../builder/scope";
import styles from "./string.module.scss";

const noop = () => {};

function DottedViewerInput({ value: _value, ...inputProps }: any) {
  const [value, _setValue] = useState(_value);

  const setValue = useAfterActions(
    useCallback(async (value: string) => {
      _setValue(value);
    }, []),
  );

  useEffect(() => {
    setValue(_value);
  }, [setValue, _value]);

  return <Input value={value} {...inputProps} />;
}

export function ViewerInput({
  name,
  value,
  type,
}: {
  name?: string;
  type?: string;
  value: string | number;
}) {
  const InputComponent = name?.includes?.(".") ? DottedViewerInput : Input;
  return (
    <InputComponent
      px={0}
      type={type || "text"}
      value={value}
      disabled
      readOnly
      bg="body"
      onChange={noop}
      className={styles.viewer}
    />
  );
}

export function ViewerBox({
  style,
  ...props
}: React.ComponentProps<typeof Box>) {
  const newStyle = useMemo(
    () => ({
      ...style,
      padding: "0.375rem 0",
    }),
    [style],
  );
  return <Box {...props} style={newStyle} />;
}

export function ViewerLink({
  children,
  onClick,
}: {
  children: React.ReactNode;
  onClick: React.MouseEventHandler<HTMLButtonElement>;
}) {
  return (
    <Button
      px={0}
      variant="link"
      alignSelf="start"
      textAlign="start"
      textDecoration="none"
      overflow="hidden"
      onClick={onClick}
      tabIndex={-1}
      className={styles.viewer}
    >
      {children}
    </Button>
  );
}
