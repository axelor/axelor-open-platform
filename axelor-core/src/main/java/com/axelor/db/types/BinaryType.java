package com.axelor.db.types;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import com.axelor.db.Binary;

/**
 * Custom Hibernate user type for the {@link Binary} type.
 * 
 * This should be moved to a separate library as it depends on Hibernate and
 * thus breaks JPA 2.0 compatibility.
 * 
 */
public class BinaryType implements UserType {

	public static final BinaryType INSTANCE = new BinaryType();

	public static final int[] SQL_TYPES = { Types.VARBINARY, };

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<Binary> returnedClass() {
		return Binary.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		return x == null ? false : x.equals(y);
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		assert (x != null);
		return x.hashCode();
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		byte[] data = rs.getBytes(names[0]);
		Binary value = new Binary();
		if (data != null)
			value.setData(data);
		return value;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
			throws HibernateException, SQLException {
		if (value != null) {
			st.setBytes(index, ((Binary) value).getData());
		} else {
			st.setNull(index, Types.VARBINARY);
		}
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}
}