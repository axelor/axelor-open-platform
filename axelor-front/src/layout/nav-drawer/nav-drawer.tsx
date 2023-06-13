import { useCallback, useMemo } from "react";

import { Box, NavBar, NavItemProps } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { useMenu } from "@/hooks/use-menu";
import { useTabs } from "@/hooks/use-tabs";
import { useTagsList } from "@/hooks/use-tags";
import { MenuItem, Tag } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { useSidebar } from "./hook";
import { NavBarSearch, SearchItem } from "./search";

function load(res: MenuItem[], tags: Tag[]) {
  const menus = res.filter((item) => item.left !== false);
  const toNavItemProps = (item: MenuItem): NavItemProps => {
    const { name, title, action, tag, tagStyle, icon, iconBackground } = item;
    const items = action
      ? undefined
      : menus.filter((x) => x.parent === name).map(toNavItemProps);
    const $tag = tags?.find((t) => t.name === name);

    const props: NavItemProps = {
      id: name,
      title,
      items,
      tag: $tag?.value ?? tag,
      tagStyle: $tag?.style ?? tagStyle,
    };

    if (icon || !item.parent) {
      props.icon = icon
        ? ({ color }) => (
            <i className={legacyClassNames("fa", icon)} style={{ color }} />
          )
        : ({ color }) => (
            <Box d="inline-flex" style={{ color }}>
              <MaterialIcon icon="list" />
            </Box>
          );
    }

    if (iconBackground) {
      props.iconColor = iconBackground;
    }

    return props;
  };
  return menus.filter((item) => !item.parent).map(toNavItemProps);
}

function toSearchItems(data: MenuItem[]) {
  const items = data;
  const nodes: Record<string, MenuItem> = {};
  const searchItems: SearchItem[] = [];

  items.forEach(function (item) {
    nodes[item.name] = item;
  });

  items.forEach(function (item) {
    let label = item.title;
    let parent = nodes[item.parent ?? ""];
    let lastParent;
    while (parent) {
      lastParent = parent;
      parent = nodes[parent.parent ?? ""];
      if (parent) {
        label = lastParent.title + "/" + label;
      }
    }
    item.action &&
      searchItems.push({
        id: item.name,
        title: item.title,
        label: label,
        action: item.action,
        category: lastParent ? lastParent.name : "",
        categoryTitle: lastParent ? lastParent.title : "",
      });
  });

  return searchItems;
}

export function NavDrawer() {
  const { open: openTab } = useTabs();
  const { loading, menus = [] } = useMenu();
  const { small, setSidebar } = useSidebar();

  const handleClick = useCallback(
    async (e: any, item: any) => {
      const menu = menus.find((x) => x.name === item.id);
      if (menu?.action) {
        if (small) setSidebar(false);
        await openTab(menu.action);
      }
    },
    [menus, small, setSidebar, openTab]
  );

  const tags = useTagsList();

  const items = useMemo(() => load(menus, tags), [menus, tags]);
  const searchItems = useMemo(() => toSearchItems(menus), [menus]);

  if (loading) return null;

  return (
    <Box flex={1}>
      <NavBarSearch items={searchItems} onClick={handleClick} />
      <NavBar items={items} onClick={handleClick} />
    </Box>
  );
}
