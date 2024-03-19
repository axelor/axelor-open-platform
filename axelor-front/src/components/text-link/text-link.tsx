import { Link, LinkProps } from "@axelor/ui";
import { StyledComponentProps } from "@axelor/ui/core/styled";

export function TextLink(props: StyledComponentProps<"a", LinkProps>) {
  const { children, target = "_blank" } = props;

  return (
    <Link
      target={target}
      {...(target === "_blank" && { rel: "noopener noreferrer" })}
      {...props}
    >
      {children}
    </Link>
  );
}
