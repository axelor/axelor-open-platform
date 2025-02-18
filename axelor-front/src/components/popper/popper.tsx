import { Popper as AxPopper, PopperProps, Fade, Portal } from "@axelor/ui/core";

/**
 * `@axelor/ui/core/Popper` wrapper to avoid early initialisation.
 *
 * This is used for `ToolTip` that show tooltip on mouse over : early
 * initialisation of `Popper` is heavy for large views. The wrapper makes
 * `Popper` lazy and improve views rendering.
 *
 * This is a temporary wrapper before it is applied on core component.
 */
export function Popper(props: PopperProps) {
  const { container, disablePortal, ...rest } = props;

  if (disablePortal) {
    return <PopperInner {...rest} />;
  }

  return (
    <Portal container={container}>
      <PopperInner {...rest} />
    </Portal>
  );
}

function PopperInner(props: PopperProps) {
  const { open, transition, ...rest } = props;
  const Transition = transition || Fade;
  return (
    <Transition in={open} appear mountOnEnter unmountOnExit>
      <div>
        <AxPopper {...rest} open={true} disablePortal transition={null} />
      </div>
    </Transition>
  );
}
