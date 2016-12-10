/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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
package com.axelor.db.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.app.AppSettings;
import com.axelor.auth.db.User;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.dms.db.DMSFile;
import com.axelor.meta.db.MetaFile;
import com.axelor.rpc.filter.Filter;
import com.axelor.rpc.filter.JPQLFilter;
import com.google.inject.persist.Transactional;

/**
 * Provides methods to initialize search indexes and do search against them.
 *
 */
@Singleton
public class SearchService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);
	
	private boolean enabled;

	@Inject
	private Provider<EntityManager> emp;

	@Inject
	private Provider<JpaSecurity> security;

	public SearchService() {
		this.enabled = !"none".equalsIgnoreCase(AppSettings.get().get(SearchModule.CONFIG_DIRECTORY_PROVIDER));
	}

	private FullTextEntityManager getFullTextEntityManager() {
		if (isEnabled()) {
			return Search.getFullTextEntityManager(emp.get());
		}
		throw new IllegalStateException("Full-text search is not enabled.");
	}

	/**
	 * Whether full-text search support is enabled.
	 * 
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Initialize search indexes if not created yet, unless forced.
	 * 
	 * @param force
	 *            if true, indexes will be re-created.
	 * @throws InterruptedException
	 */
	@Transactional
	public void createIndex(boolean force) throws InterruptedException {
		final FullTextEntityManager ftem = getFullTextEntityManager();
		final QueryBuilder builder = ftem.getSearchFactory().buildQueryBuilder().forEntity(User.class).get();
		final Query query = builder.keyword().wildcard().onField("code").matching("*").createQuery();
		if (!force & ftem.createFullTextQuery(query, User.class).setMaxResults(1).getResultList().size() > 0) {
			return;
		}
		LOGGER.info("Initializing search indexes....");
		ftem.createIndexer().startAndWait();
	}

	/**
	 * Perform full-text search.
	 *
	 * @param runner
	 *            the search runner
	 */
	public void doSearch(Consumer<FullTextEntityManager> runner) {
		runner.accept(getFullTextEntityManager());
	}

	/**
	 * Do full-text search on the given entity type with the given search text.
	 * 
	 * <p>
	 * The method will do search in batches and apply security filter on them
	 * until max records are not found or there are no more records to search.
	 * </p>
	 * 
	 * @param entityType
	 *            the entity type to search on
	 * @param searchText
	 *            the search text
	 * @param limit
	 *            maximum number of result
	 * @return list of record ids
	 * @throws IOException
	 *             if any error reading indexes
	 */
	public List<Long> fullTextSearch(Class<? extends Model> entityType, String searchText, int limit)
			throws IOException {

		// check for read permission
		security.get().check(JpaSecurity.CAN_READ, entityType);

		final Filter filter = security.get().getFilter(JpaSecurity.CAN_READ, entityType);

		final FullTextEntityManager em = getFullTextEntityManager();
		final List<Long> all = new ArrayList<>();

		if (!em.getSearchFactory().getIndexedTypes().contains(entityType)) {
			return all;
		}

		final Mapper mapper = Mapper.of(entityType);
		final Property nameField = mapper.getNameField();

		final List<String> names = new ArrayList<>();
		final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity(entityType).get();

		if (nameField != null) {
			names.add(nameField.getName());
		}

		if (DMSFile.class.isAssignableFrom(entityType)) {
			names.add("metaFile.filePath");
		}
		if (MetaFile.class.isAssignableFrom(entityType)) {
			names.add("filePath");
		}
		if (names.isEmpty()) {
			return all;
		}

		final org.apache.lucene.search.Query query = builder.keyword().wildcard()
				.onFields(names.toArray(new String[] {})).ignoreFieldBridge().matching("*" + searchText + "*")
				.createQuery();

		LOGGER.debug("Searching {} for {}", entityType.getName(), query);

		// find documents in batches and filter them according to security
		// filter
		try (IndexReader reader = em.getSearchFactory().getIndexReaderAccessor().open(entityType)) {
			IndexSearcher searcher = new IndexSearcher(reader);
			ScoreDoc after = null;
			int topCount = limit;
			while (true) {
				final List<Long> found = new ArrayList<>();
				try {
					final TopDocs docs = searcher.searchAfter(after, query, limit);
					for (ScoreDoc doc : docs.scoreDocs) {
						found.add(Long.parseLong(searcher.doc(doc.doc).get("id")));
					}
					if (docs.totalHits < topCount) {
						after = null;
					} else {
						after = docs.scoreDocs[docs.scoreDocs.length - 1];
					}
				} catch (IOException e) {
				}
				if (filter == null) {
					return found;
				}

				// filter by security filter
				final Filter check = Filter.and(new JPQLFilter("self.id in ?", found), filter);
				final com.axelor.db.Query<? extends Model> qm = check.build(entityType);
				for (List<?> item : qm.select("id").values(found.size(), 0)) {
					all.add((Long) item.get(0));
					if (all.size() == limit) {
						return all;
					}
				}

				topCount = topCount - all.size();
				if (after == null || topCount <= 0) {
					return all;
				}
			}
		}
	}
}
