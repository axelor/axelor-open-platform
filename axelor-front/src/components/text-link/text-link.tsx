import { Link } from "@axelor/ui";

export function TextLink(props: React.ComponentProps<typeof Link>) {
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
