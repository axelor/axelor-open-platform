import { useMenu } from "@/hooks/use-menu/use-menu";
import { useTabs } from "@/hooks/use-tabs/use-tabs";
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

    return props;
  };
  return menus.filter((item) => !item.parent).map(toNavItemProps);
}

export function NavDrawer() {
  const tabs = useTabs();
  const { loading, execute, menus } = useMenu();

  const handleClick = useCallback(
    async (e: any, item: any) => {
      const menu = menus.find((x) => x.name === item.id);
      if (menu?.action) {
        const view = await execute(menu.action);
        tabs.open(view);
      }
    },
    [tabs, menus, execute]
  );

  const items = useMemo(() => load(menus), [menus]);

  if (loading) return null;

  return (
    <Box>
      <NavBar items={items} onClick={handleClick} />
    </Box>
  );
}
