import { parseSafe } from "./parser";

export function processReactTemplate(template) {
  const render = parseSafe(template);
  const ReactComponent = ({ context }) => {
    return render(context);
  };
  return ReactComponent;
}
