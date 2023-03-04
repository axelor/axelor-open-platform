import { useMenu } from "@/hooks/use-menu";
import { useMeta } from "@/hooks/use-meta";
import { useTabs } from "@/hooks/use-tabs";
import { MenuItem } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { Box, Icon, NavBar, NavItemProps } from "@axelor/ui";
import { ReactComponent as BiListIcon } from "bootstrap-icons/icons/list.svg";
import { useCallback, useMemo } from "react";

function load(res: MenuItem[]) {
  const menus = res.filter((item) => item.left !== false);
  const toNavItemProps = (item: MenuItem): NavItemProps => {
    const { name, title, action, icon, iconBackground } = item;
    const items = action
      ? undefined
      : menus.filter((x) => x.parent === name).map(toNavItemProps);

    const props: NavItemProps = {
      id: name,
      title,
      items,
    };

    if (icon || !item.parent) {
      props.icon = icon
        ? ({ color }) => <i className={legacyClassNames("fa", icon)} />
        : ({ color }) => <Icon as={BiListIcon} style={{ color }} />;
    }

    if (iconBackground) {
      props.iconColor = iconBackground;
    }

    return props;
  };
  return menus.filter((item) => !item.parent).map(toNavItemProps);
}

export function NavDrawer() {
  const tabs = useTabs();
  const { loading, menus } = useMenu();
  const { findActionView } = useMeta();

  const handleClick = useCallback(
    async (e: any, item: any) => {
      const menu = menus.find((x) => x.name === item.id);
      if (menu?.action) {
        const view = await findActionView(menu.action);
        if (view) {
          tabs.open(view);
        }
      }
    },
    [tabs, menus, findActionView]
  );

  const items = useMemo(() => load(menus), [menus]);

  if (loading) return null;

  return (
    <Box>
      <NavBar items={items} onClick={handleClick} />
    </Box>
  );
}
