/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.meta.service.tags;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.inject.Beans;
import com.axelor.meta.ActionExecutor;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.service.menu.MenuChecker;
import com.axelor.meta.service.menu.MenuNode;
import com.axelor.meta.service.menu.MenuUtils;
import com.axelor.meta.service.menu.SimpleMenuNodeVisitor;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.filter.Filter;
import com.axelor.rpc.filter.JPQLFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagsService {

  private static final Logger LOG = LoggerFactory.getLogger(TagsService.class);

  @Inject private ActionExecutor actionExecutor;

  public List<TagItem> get(List<String> names) {
    return get(names, AuthUtils.getUser());
  }

  public List<TagItem> get(List<String> names, User user) {

    if (ObjectUtils.isEmpty(names) || user == null) {
      return Collections.emptyList();
    }

    // fetch menus
    final List<MetaMenu> metaMenus = MenuUtils.fetchMetaMenu(names);
    if (ObjectUtils.isEmpty(metaMenus)) {
      return Collections.emptyList();
    }

    // add parent to metaMenu list in order to check perms and conditions
    final List<MetaMenu> parentMetaMenus = new ArrayList<>();
    for (MetaMenu metaMenu : metaMenus) {
      while (metaMenu.getParent() != null) {
        metaMenu = metaMenu.getParent();
        if (!parentMetaMenus.contains(metaMenu)) {
          parentMetaMenus.add(metaMenu);
        }
      }
    }
    metaMenus.addAll(parentMetaMenus);

    List<TagItem> tagItems = new ArrayList<>();
    MenuChecker checker = new MenuChecker(metaMenus, user);

    // build tree
    MenuNode rootNode = MenuNode.buildTree(metaMenus);

    // traverse tree
    rootNode.traverse(
        new SimpleMenuNodeVisitor() {

          /** Check whether the node can be visited */
          @Override
          public MenuNodeResult preChildVisit(MenuNode childNode) {
            if (checker.isAllowed(childNode.getMetaMenu())
                && checker.canShow(childNode.getMetaMenu())) {
              return MenuNodeResult.CONTINUE;
            }
            return MenuNodeResult.TERMINATE;
          }

          /** On each node visited */
          @Override
          public MenuNodeResult visit(MenuNode node) {
            MetaMenu metaMenu = node.getMetaMenu();
            if (!node.isRoot() && hasTag(metaMenu)) {
              tagItems.add(buildTagItem(node.getMetaMenu()));
            }
            return MenuNodeResult.CONTINUE;
          }
        });

    return tagItems;
  }

  /**
   * Check whether the {@link MetaMenu} has tag
   *
   * @param metaMenu the meta menu to check
   * @return true if the meta menu has tag, false otherwise
   */
  private boolean hasTag(MetaMenu metaMenu) {
    return StringUtils.notEmpty(metaMenu.getTag())
        || StringUtils.notEmpty(metaMenu.getTagGet())
        || metaMenu.getTagCount();
  }

  /**
   * Build {@link TagItem} from the given {@link MetaMenu}
   *
   * @param metaMenu the meta menu
   * @return created {@link TagItem}
   */
  private TagItem buildTagItem(MetaMenu metaMenu) {
    return new TagItem(metaMenu.getName(), getTagValue(metaMenu), metaMenu.getTagStyle());
  }

  /**
   * Get the tag value of the given {@link MetaMenu}
   *
   * @param item the meta menu
   * @return value of the meta menu
   */
  public String getTagValue(MetaMenu item) {

    final String staticTag = item.getTag();
    final String tagGetAction = item.getTagGet();
    final Boolean hasTagCount = item.getTagCount();

    if (staticTag != null) {
      return staticTag;
    }

    try {
      if (tagGetAction != null) {
        return callTagGet(item);
      }

      if (hasTagCount) {
        if (item.getAction() != null) {
          return callTagCount(item);
        }
        LOG.error("No action defined on menu {} to get tag count", item.getName());
      }
    } catch (Exception e) {
      LOG.error("Unable to read tag for menu: {}", item.getName());
      LOG.trace("Error", e);
    }

    return null;
  }

  /**
   * Call tag-count of the given {@link MetaMenu}
   *
   * @param item the meta menu
   * @return result of the meta menu tag-count
   */
  @SuppressWarnings("all")
  private String callTagCount(MetaMenu item) {
    final MetaAction action = item.getAction();
    final ActionView act = (ActionView) MetaStore.getAction(action.getName());

    if (act == null) {
      return null;
    }

    final ActionRequest request = new ActionRequest();
    request.setAction(action.getName());
    request.setModel(action.getModel());
    request.setData(new HashMap<>());

    final JpaSecurity security = Beans.get(JpaSecurity.class);
    final List<Filter> filters = new ArrayList<>();
    final Class<? extends Model> modelClass = (Class<? extends Model>) request.getBeanClass();
    final Filter securityFilter = security.getFilter(JpaSecurity.CAN_READ, modelClass);

    if (securityFilter != null) {
      filters.add(securityFilter);
    } else if (!security.isPermitted(JpaSecurity.CAN_READ, modelClass)) {
      return null;
    }

    final Map<String, Object> data =
        (Map) ((Map) actionExecutor.execute(request).getItem(0)).get("view");
    final Map<String, Object> params = (Map<String, Object>) data.get("params");
    if (params == null || !Boolean.TRUE.equals(params.get("showArchived"))) {
      filters.add(new JPQLFilter("self.archived IS NULL OR self.archived = FALSE"));
    }

    final String domain = (String) data.get("domain");
    if (StringUtils.notBlank(domain)) {
      filters.add(JPQLFilter.forDomain(domain));
    }

    final Filter filter = Filter.and(filters);
    final Map<String, Object> context = (Map) data.get("context");
    return String.valueOf(filter.build(modelClass).bind(context).count());
  }

  /**
   * Call tag-get of the given {@link MetaMenu}
   *
   * @param item the meta menu
   * @return result of the meta menu tag-get
   */
  private String callTagGet(MetaMenu item) {
    final ActionRequest request = new ActionRequest();
    request.setAction(item.getTagGet());
    return (String) actionExecutor.execute(request).getItem(0);
  }
}
