import { Box, Button, Input } from "@axelor/ui";
import { useMemo } from "react";

export function ViewerInput({ value }: { value: string | number }) {
  return (
    <Input
      type="text"
      defaultValue={value}
      disabled
      readOnly
      bg="body"
      border={false}
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
      padding: "0.375rem 0.75rem",
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
      variant="link"
      alignSelf="start"
      textDecoration="none"
      onClick={onClick}
    >
      {children}
    </Button>
  );
}
