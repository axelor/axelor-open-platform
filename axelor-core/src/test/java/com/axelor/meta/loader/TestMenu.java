/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.meta.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.RoleRepository;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.common.ResourceUtils;
import com.axelor.meta.MetaTest;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.repo.MetaMenuRepository;
import com.axelor.meta.schema.views.MenuItem;
import com.axelor.meta.service.menu.MenuService;
import com.axelor.meta.service.tags.TagItem;
import com.axelor.meta.service.tags.TagsService;
import com.google.inject.persist.Transactional;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestMenu extends MetaTest {

  @Inject private ViewLoader loader;

  @Inject private UserRepository users;
  @Inject private RoleRepository roles;
  @Inject private MetaMenuRepository metaMenuRepository;
  @Inject private MenuService menuService;
  @Inject private TagsService tagsService;

  @BeforeEach
  @Transactional
  public void init() throws JAXBException, IOException {
    if (users.all().count() == 0) {
      createDemoData();
    }
    if (metaMenuRepository.all().count() == 0) {
      final URL url = ResourceUtils.getResource("com/axelor/meta/Menu.xml");
      loader.process(url, new Module("test"), false);

      MetaMenu menu = metaMenuRepository.findByName("menu-root-3-2");
      menu.addRole(roles.findByName("guest.user"));
      metaMenuRepository.save(menu);
    }
  }

  @Test
  @Transactional
  public void testMenus() {
    List<MenuItem> adminMenus = menuService.getMenus(users.findByCode("admin"));
    assertNotNull(adminMenus);
    assertEquals(7, adminMenus.size());
    // hidden menu
    assertFalse(adminMenus.stream().anyMatch(it -> it.getName().equals("menu-root-3-3")));

    List<MenuItem> demoMenus = menuService.getMenus(users.findByCode("demo"));
    assertNotNull(demoMenus);
    assertEquals(4, demoMenus.size());
    // guest role only
    assertFalse(demoMenus.stream().anyMatch(it -> it.getName().equals("menu-root-3-2")));
    // root menu with no groups or roles
    assertFalse(demoMenus.stream().anyMatch(it -> it.getName().equals("menu-root-2")));
    // hidden menu
    assertFalse(demoMenus.stream().anyMatch(it -> it.getName().equals("menu-root-3-3")));

    List<MenuItem> guestMenus = menuService.getMenus(users.findByCode("guest"));
    assertNotNull(guestMenus);
    assertEquals(5, guestMenus.size());
    // root menu with no groups or roles
    assertFalse(demoMenus.stream().anyMatch(it -> it.getName().equals("menu-root-2")));
    // hidden menu
    assertFalse(demoMenus.stream().anyMatch(it -> it.getName().equals("menu-root-3-3")));
  }

  @Test
  @Transactional
  public void testTags() {
    List<TagItem> adminTags =
        tagsService.get(List.of("menu-root-1-1", "menu-root-1-2"), users.findByCode("admin"));
    assertNotNull(adminTags);
    assertEquals(1, adminTags.size());
    // menu-root-1-1 doesn't have tag
    assertFalse(adminTags.stream().anyMatch(it -> it.getName().equals("menu-root-1-1")));

    assertEquals("important", adminTags.get(0).getStyle());

    List<TagItem> demoTags =
        tagsService.get(
            List.of("menu-root-1-2", "menu-root-3-2", "menu-root-3-3"), users.findByCode("demo"));
    assertNotNull(demoTags);
    assertEquals(1, demoTags.size());
    // not allowed
    assertFalse(demoTags.stream().anyMatch(it -> it.getName().equals("menu-root-3-2")));
    // hidden menu
    assertFalse(demoTags.stream().anyMatch(it -> it.getName().equals("menu-root-3-3")));

    List<TagItem> guestTags =
        tagsService.get(
            List.of("menu-root-1-2", "menu-root-3-2", "menu-root-3-3"), users.findByCode("guest"));
    assertNotNull(guestTags);
    assertEquals(2, guestTags.size());
    // hidden menu
    assertFalse(guestTags.stream().anyMatch(it -> it.getName().equals("menu-root-3-3")));
  }

  private void createDemoData() {

    User admin = new User("admin", "Administrator");
    User demo = new User("demo", "Demo");
    User guest = new User("guest", "Guest");

    admin.setPassword("admin");
    demo.setPassword("demo");
    guest.setPassword("guest");

    Group adminGroup = new Group("admins", "Administrators");
    Group userGroup = new Group("users", "Users");

    admin.setGroup(adminGroup);
    demo.setGroup(userGroup);
    guest.setGroup(userGroup);

    users.save(admin);
    users.save(demo);
    users.save(guest);

    Role superUserRole = new Role("super.user");
    Role normalUserRole = new Role("normal.user");
    Role guestUserRole = new Role("guest.user");

    adminGroup.addRole(superUserRole);
    userGroup.addRole(normalUserRole);
    guest.addRole(guestUserRole);

    users.save(admin);
    users.save(demo);
    users.save(guest);
  }
}
