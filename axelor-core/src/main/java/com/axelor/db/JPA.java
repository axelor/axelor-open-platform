/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.db;

import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Provider;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.OneToMany;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.collection.spi.PersistentCollection;

/**
 * This class provides easy access to {@link EntityManager} and related API. It also provides some
 * convenient methods like create {@link Model} instances or creating {@link Query}.<br>
 * <br>
 * This class should be initialized using Guice container as eager singleton during application
 * startup.
 */
@Singleton
public final class JPA {

  private Provider<EntityManager> emp;
  private static JPA INSTANCE = null;

  @Inject
  private JPA(Provider<EntityManager> emp) {
    this.emp = emp;
    INSTANCE = this;
  }

  private static JPA get() {
    if (INSTANCE == null) {
      throw new RuntimeException("JPA context not initialized.");
    }
    return INSTANCE;
  }

  /** Get an instance of {@link EntityManager}. */
  public static EntityManager em() {
    return get().emp.get();
  }

  /**
   * Execute a JPQL update query.
   *
   * @param query JPQL query
   */
  public static int execute(String query) {
    return em().createQuery(query).executeUpdate();
  }

  /**
   * Prepare a {@link Query} for the given model class.
   *
   * @param klass the model class
   */
  public static <T extends Model> Query<T> all(Class<T> klass) {
    return Query.of(klass);
  }

  /**
   * Find by primary key.
   *
   * @see EntityManager#find(Class, Object)
   */
  public static <T extends Model> T find(Class<T> klass, Long id) {
    return em().find(klass, id);
  }

  private static boolean isAutoFlushEnabled() {
    return !Objects.equal(
        "false", em().getEntityManagerFactory().getProperties().get("JPA.auto_flush"));
  }

  /**
   * Make an entity managed and persistent.
   *
   * @see EntityManager#persist(Object)
   */
  public static <T extends Model> T persist(T entity) {
    // optimistic concurrency check
    checkVersion(entity, entity.getVersion());
    em().persist(entity);
    if (isAutoFlushEnabled()) {
      em().flush();
    }
    return entity;
  }

  /**
   * Merge the state of the given entity into the current persistence context.
   *
   * @see EntityManager#merge(Object)
   */
  public static <T extends Model> T merge(T entity) {
    // optimistic concurrency check
    checkVersion(entity, entity.getVersion());
    T result = em().merge(entity);
    if (isAutoFlushEnabled()) {
      em().flush();
    }
    return result;
  }

  /**
   * Save the state of the entity.<br>
   * <br>
   * It uses either {@link #persist(Model)} or {@link #merge(Model)} and calls {@link #flush()} to
   * synchronize values with database.
   *
   * @see #persist(Model)
   * @see #merge(Model)
   */
  public static <T extends Model> T save(T entity) {
    if (em().contains(entity) || entity.getId() == null) {
      return persist(entity);
    }
    return merge(entity);
  }

  /**
   * Remove the entity instance.
   *
   * @see EntityManager#remove(Object)
   */
  public static <T extends Model> void remove(T entity) {
    EntityManager manager = em();
    if (manager.contains(entity)) {
      manager.remove(entity);
    } else {
      // optimistic concurrency check
      checkVersion(entity, entity.getVersion());
      Model attached = manager.find(entity.getClass(), entity.getId());
      manager.remove(attached);
    }
  }

  /**
   * Refresh the state of the instance from the database, overwriting changes made to the entity, if
   * any.
   *
   * @see EntityManager#refresh(Object)
   */
  public static <T extends Model> void refresh(T entity) {
    em().refresh(entity);
  }

  /**
   * Synchronize the persistence context to the underlying database.
   *
   * @see EntityManager#flush()
   */
  public static void flush() {
    em().flush();
  }

  /**
   * Clear the persistence context, causing all managed entities to become detached.
   *
   * @see EntityManager#clear()
   */
  public static void clear() {
    em().clear();
  }

  private static <T extends Model> void checkVersion(T bean, Object version) {
    if (bean == null || version == null) {
      return;
    }
    final Class<T> klass = EntityHelper.getEntityClass(bean);
    final Model entity = JPA.em().find(klass, bean.getId());
    if (entity == null || !Objects.equal(version, entity.getVersion())) {
      Exception cause = new StaleObjectStateException(klass.getName(), bean.getId());
      throw new OptimisticLockException(cause.getMessage(), cause, bean);
    }
  }

  /**
   * Verify the values against the database values to ensure the records involved are not modified.
   *
   * @throws OptimisticLockException if version mismatch of any a record is deleted
   */
  @SuppressWarnings("all")
  public static void verify(Class<? extends Model> model, Map<String, Object> values) {
    if (values == null) {
      return;
    }
    Long id = null;
    try {
      id = Long.parseLong(values.get("id").toString());
    } catch (Exception e) {
    }

    Object version = values.get("version");
    Mapper mapper = Mapper.of(model);
    Model entity = id == null ? null : JPA.find(model, id);

    if (id != null && version != null) {
      if (entity == null || !Objects.equal(version, entity.getVersion())) {
        Exception cause = new StaleObjectStateException(model.getName(), id);
        throw new OptimisticLockException(cause);
      }
    }

    for (String key : values.keySet()) {
      Object value = values.get(key);
      Property property = mapper.getProperty(key);
      if (property == null || property.getTarget() == null) continue;
      if (!(value instanceof Map || value instanceof Collection)) {
        continue;
      }
      if (property.isCollection() && value instanceof Collection) {
        int size = 0;
        try {
          size = ((Collection) property.get(entity)).size();
        } catch (Exception e) {
        }
        if (size > ((Collection) value).size()) {
          Exception cause = new StaleObjectStateException(model.getName(), id);
          throw new OptimisticLockException(cause);
        }
      }

      if (value instanceof Map) {
        value = Lists.newArrayList(value);
      }
      for (Object item : (Collection<?>) value) {
        if (item instanceof Map) {
          verify((Class) property.getTarget(), (Map) item);
        }
      }
    }
  }

  /**
   * Edit an instance of the given model class using the given values.<br>
   * <br>
   * This is a convenient method to reconstruct model object from a key value map, for example HTTP
   * params.
   *
   * @param klass a model class
   * @param values key value map where key represents a field name
   * @return a JPA managed object of the given model class
   */
  public static <T extends Model> T edit(Class<T> klass, Map<String, Object> values) {
    Set<Model> visited = Sets.newHashSet();
    Multimap<String, Long> edited = HashMultimap.create();
    try {
      return _edit(klass, values, visited, edited);
    } finally {
      visited.clear();
      edited.clear();
    }
  }

  @SuppressWarnings("all")
  private static <T extends Model> T _edit(
      Class<T> klass,
      Map<String, Object> values,
      Set<Model> visited,
      Multimap<String, Long> edited) {

    if (values == null) return null;

    Mapper mapper = Mapper.of(klass);
    Long id = null;
    T bean = null;

    try {
      id = Long.valueOf(values.get("id").toString());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    } catch (NullPointerException e) {
    }

    if (id == null || id <= 0) {
      id = null;
      try {
        bean = klass.newInstance();
      } catch (Exception ex) {
        throw new IllegalArgumentException(ex);
      }
    } else {
      bean = JPA.em().find(klass, id);
      if (bean == null) {
        throw new OptimisticLockException(new StaleObjectStateException(klass.getName(), id));
      }
    }

    // optimistic concurrency check
    Integer beanVersion = (Integer) values.get("version");
    boolean beanChanged = false;

    if (visited.contains(bean) && beanVersion == null) {
      return bean;
    }

    visited.add(bean);

    // don't update reference objects
    if (id != null && (beanVersion == null || edited.containsEntry(klass.getName(), id))) {
      return bean;
    }
    if (id != null) {
      edited.put(klass.getName(), id);
    }

    for (String name : values.keySet()) {

      Property p = mapper.getProperty(name);
      if (p == null || p.isPrimary() || p.isVersion() || mapper.getSetter(name) == null) continue;

      Object value = values.get(name);
      Class<Model> target = (Class<Model>) p.getTarget();

      if (p.isCollection()) {

        Collection items = new ArrayList();
        if (Set.class.isAssignableFrom(p.getJavaType())) items = new HashSet();

        if (value instanceof Collection) {
          for (Object val : (Collection) value) {
            if (val instanceof Map) {
              if (p.getMappedBy() != null) {
                if (val instanceof ImmutableMap) val = Maps.newHashMap((Map) val);
                ((Map) val).remove(p.getMappedBy());
              }
              Model item = _edit(target, (Map) val, visited, edited);
              items.add(p.setAssociation(item, bean));
            } else if (val instanceof Number) {
              items.add(JPA.find(target, Long.parseLong(val.toString())));
            }
          }
        }

        Object old = mapper.get(bean, name);
        if (old instanceof Collection) {
          boolean changed = ((Collection) old).size() != items.size();
          if (!changed) {
            for (Object item : items) {
              if (!((Collection) old).contains(item)) {
                changed = true;
                break;
              }
            }
          }
          if (changed) {
            if (p.isOrphan()) {
              for (Object item : (Collection) old) {
                if (!items.contains(item)) {
                  p.setAssociation(item, null);
                }
              }
            }
            p.clear(bean);
            p.addAll(bean, items);
            beanChanged = true;
          }
          continue;
        }
        if (p.getType() == PropertyType.MANY_TO_MANY && p.getMappedBy() != null) {
          p.addAll(bean, items);
        }
        value = items;
      } else if (p.isReference() && value instanceof Map) {
        value = _edit(target, (Map) value, visited, edited);
      }
      Object oldValue = mapper.set(bean, name, value);
      if (p.valueChanged(bean, oldValue)) {
        beanChanged = true;
      }
    }

    if (beanChanged) {
      checkVersion(bean, beanVersion);
    } else if (id != null) {
      edited.remove(klass.getName(), id);
    }

    return bean;
  }

  /**
   * A convenient method to persist reconstructed unmanaged objects.<br>
   * <br>
   * This method takes care of relational fields by inspecting the managed state of the referenced
   * objects and also sets reverse lookup fields annotated with {@link OneToMany#mappedBy()}
   * annotation.
   *
   * @see JPA#edit(Class, Map)
   * @param bean model instance
   * @return JPA managed model instance
   */
  public static <T extends Model> T manage(T bean) {
    Set<Model> visited = Sets.newHashSet();
    try {
      T managed = _manage(bean, visited);
      if (EntityHelper.isUninitialized(managed)) {
        return managed;
      }
      return persist(managed);
    } finally {
      visited.clear();
    }
  }

  private static <T extends Model> T _manage(T bean, Set<Model> visited) {

    if (visited.contains(bean) || EntityHelper.isUninitialized(bean)) {
      return bean;
    }
    visited.add(bean);

    Mapper mapper = Mapper.of(bean.getClass());
    for (Property property : mapper.getProperties()) {

      if (property.getTarget() == null || property.isReadonly()) continue;

      Object value = property.get(bean);
      if (value == null) continue;

      if (value instanceof PersistentCollection && !((PersistentCollection) value).wasInitialized())
        continue;

      // bind M2O
      if (property.isReference()) {
        _manage((Model) value, visited);
      }

      // bind O2M & M2M
      else if (property.isCollection()) {
        for (Object val : (Collection<?>) value) {
          _manage(property.setAssociation((Model) val, bean), visited);
        }
      }
    }
    return bean;
  }

  /**
   * Return all the non-abstract models found in all the activated modules.
   *
   * @return Set of model classes
   */
  public static Set<Class<?>> models() {
    return JpaScanner.findModels().stream()
        .filter(c -> !Modifier.isAbstract(c.getModifiers()))
        .collect(Collectors.toSet());
  }

  /**
   * Return the model class for the given name.
   *
   * @param name name of the model
   * @return model class
   */
  public static Class<?> model(String name) {
    return JpaScanner.findModel(name);
  }

  /** Return all the properties of the given model class. */
  public static <T extends Model> Property[] fields(Class<T> klass) {
    return Mapper.of(klass).getProperties();
  }

  /**
   * Return a {@link Property} of the given model class.
   *
   * @param klass a model class
   * @param name name of the property
   * @return property or null if property doesn't exist
   */
  public static <T extends Model> Property field(Class<T> klass, String name) {
    return Mapper.of(klass).getProperty(name);
  }

  /**
   * Create a duplicate copy of the given bean instance.<br>
   * <br>
   * In case of deep copy, one-to-many records are duplicated. Otherwise, one-to-many records will
   * be skipped.
   *
   * @param bean the bean to copy
   * @param deep whether to create a deep copy
   * @return a copy of the given bean
   */
  public static <T extends Model> T copy(T bean, boolean deep) {
    Set<String> visited = Sets.newHashSet();
    try {
      return _copy(bean, deep, visited);
    } finally {
      visited.clear();
    }
  }

  @SuppressWarnings("all")
  private static <T extends Model> T _copy(T bean, boolean deep, Set<String> visited) {

    if (bean == null) {
      return bean;
    }

    bean = EntityHelper.getEntity(bean);

    final Class<?> beanClass = bean.getClass();
    final String key = beanClass.getName() + "#" + bean.getId();
    if (visited.contains(key)) {
      return null;
    }
    visited.add(key);

    Mapper mapper = Mapper.of(beanClass);
    final T obj = Mapper.toBean((Class<T>) beanClass, null);
    final int random = new Random().nextInt();
    for (final Property p : mapper.getProperties()) {

      if (p.isVirtual() || p.isPrimary() || p.isVersion() || p.isSequence() || !p.isCopyable()) {
        continue;
      }

      if (p.getType() == PropertyType.ONE_TO_ONE) {
        continue;
      }

      Object value = p.get(bean);

      if (value instanceof List && deep) {
        List items = Lists.newArrayList();
        for (Object item : (List) value) {
          Object val = copy((Model) item, true);
          // break bi-directional association
          p.setAssociation(val, null);
          items.add(val);
        }
        value = items;
      } else if (value instanceof List) {
        value = null;
      } else if (value instanceof Set) {
        value = new HashSet((Set) value);
      }

      if (value instanceof String && p.isUnique()) {
        value = ((String) value) + " Copy (" + random + ")";
      }

      p.set(obj, value);
    }

    return obj;
  }

  /**
   * Run the given <code>task</code> inside a transaction that is committed after the task is
   * completed.
   *
   * @param task the task to run.
   */
  public static void runInTransaction(Runnable task) {
    Preconditions.checkNotNull(task);
    EntityTransaction txn = em().getTransaction();
    boolean txnStarted = false;
    try {
      if (!txn.isActive()) {
        txn.begin();
        txnStarted = true;
      }
      task.run();
      if (txnStarted && txn.isActive() && !txn.getRollbackOnly()) {
        txn.commit();
      }
    } finally {
      if (txnStarted && txn.isActive()) {
        txn.rollback();
      }
    }
  }

  /**
   * Perform JDBC related work using the {@link Connection} managed by the current {@link
   * EntityManager}.
   *
   * @param work The work to be performed
   * @throws PersistenceException Generally indicates wrapped {@link SQLException}
   */
  public static void jdbcWork(final JDBCWork work) {
    Session session = (Session) em().getDelegate();
    try {
      session.doWork(
          new org.hibernate.jdbc.Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
              work.execute(connection);
            }
          });
    } catch (HibernateException e) {
      throw new PersistenceException(e);
    }
  }

  public static interface JDBCWork {

    /**
     * Execute the discrete work encapsulated by this work instance using the supplied connection.
     *
     * <p>Generally, you should not close the connection as it's being used by the current {@link
     * EntityManager}.
     *
     * @param connection The connection on which to perform the work.
     * @throws SQLException Thrown during execution of the underlying JDBC interaction.
     */
    void execute(Connection connection) throws SQLException;
  }
}
