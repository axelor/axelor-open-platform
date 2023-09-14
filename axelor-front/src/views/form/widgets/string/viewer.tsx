import { Box, Button, Input } from "@axelor/ui";
import { useMemo } from "react";

const noop = () => {};

export function ViewerInput({
  value,
  type,
}: {
  type?: string;
  value: string | number;
}) {
  return (
    <Input
      px={0}
      type={type || "text"}
      value={value}
      disabled
      readOnly
      bg="body"
      border={false}
      onChange={noop}
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
    [style]
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
      textAlign="start"
      textDecoration="none"
      overflow="hidden"
      onClick={onClick}
    >
      {children}
    </Button>
  );
}
