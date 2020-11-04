/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package com.axelor.db.hibernate.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.UniqueKey;

import java.util.Iterator;

/**

 */
public class SqlServer2008UniqueDelegate extends DefaultUniqueDelegate {


	public SqlServer2008UniqueDelegate( Dialect dialect ) {
		super( dialect );
	}

	// legacy model ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected String uniqueConstraintSql1(UniqueKey uniqueKey) {
		final StringBuilder sb = new StringBuilder();
		sb.append( "unique (" );
		final Iterator<org.hibernate.mapping.Column> columnIterator = uniqueKey.columnIterator();
		while ( columnIterator.hasNext() ) {
			final org.hibernate.mapping.Column column = columnIterator.next();
			sb.append( column.getQuotedName( dialect ) );
			if ( uniqueKey.getColumnOrderMap().containsKey( column ) ) {
				sb.append( " " ).append( uniqueKey.getColumnOrderMap().get( column ) );
			}
			if ( columnIterator.hasNext() ) {
				sb.append( ", " );
			}
		}

		return sb.append( ')' ).toString();
	}

	protected String uniqueConstraintSql(UniqueKey uniqueKey) {
		final StringBuilder sb = new StringBuilder();
		final StringBuilder whereIsNotNull = new StringBuilder();
		boolean firstColumn = true;

		sb.append( "unique (" );
		final Iterator<org.hibernate.mapping.Column> columnIterator = uniqueKey.columnIterator();
		while ( columnIterator.hasNext() ) {
			final org.hibernate.mapping.Column column = columnIterator.next();
			if (column.isNullable()){
				if (!firstColumn){
					whereIsNotNull.append( " AND " );
				}else{
					whereIsNotNull.append( " WHERE " );
					firstColumn = false;
				}

				whereIsNotNull.append( column.getQuotedName( dialect ) ).append( " IS NOT NULL " );
			}
			sb.append( column.getQuotedName( dialect ) );
			if ( uniqueKey.getColumnOrderMap().containsKey( column ) ) {
				sb.append( " " ).append( uniqueKey.getColumnOrderMap().get( column ) );
			}
			if ( columnIterator.hasNext() ) {
				sb.append( ", " );
			}
		}
		sb.append( ')' );

		if (whereIsNotNull.length() > 0){
			sb.append( whereIsNotNull.toString() );
		}



		return sb.toString();
	}


}
