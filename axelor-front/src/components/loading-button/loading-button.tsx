import { useCallback, useState } from "react";

import { Button, CircularProgress } from "@axelor/ui";
import styled, { withStyled } from "@axelor/ui/core/styled";

const LoadingButtonComponent = styled(Button)<{ loading?: boolean }>(() => []);

export const LoadingButton = withStyled(LoadingButtonComponent)((
  { loading, disabled, children, onClick, ...props },
  ref,
) => {
  const [progressing, setIsProgressing] = useState(false);

  const handleClick = useCallback<React.MouseEventHandler<HTMLButtonElement>>(
    async (e) => {
      setIsProgressing(true);

      try {
        await onClick?.(e);
      } finally {
        setIsProgressing(false);
      }
    },
    [onClick],
  );

  return (
    <Button
      ref={ref}
      {...props}
      disabled={disabled || progressing || loading}
      onClick={handleClick}
    >
      {(progressing || loading) && <CircularProgress size={16} indeterminate />}
      {children}
    </Button>
  );
});
