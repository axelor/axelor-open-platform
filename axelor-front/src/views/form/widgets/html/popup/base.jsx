import { ClickAwayListener, Popper } from "@axelor/ui";

function Popup({ data, ...rest }) {
  const {
    component,
    target,
  } = data || {};
  const { commands } = rest;
  const open = Boolean(data);

  function renderComponent(Component, props) {
    return <Component {...rest} {...props} />;
  }

  return (
    <Popper
      open={open}
      target={target}
      placement="bottom-start"
      shadow
    >
      <ClickAwayListener onClickAway={commands.closePopup}>
        <div>
          {component && renderComponent(component, data?.props)}
        </div>
      </ClickAwayListener>
    </Popper>
  );
}

export default Popup;
