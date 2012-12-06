package com.axelor.db;

import java.lang.reflect.Field;

import javax.persistence.GeneratedValue;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * The base abstract model class to extend all domain objects.
 * 
 * The derived model classes should implement {@link #getId()} and
 * {@link #setId(Long)} using appropriate {@link GeneratedValue#strategy()}.
 * 
 * A generic implementation {@link JpaModel} should be used in most cases if
 * sequence of record ids are important.
 * 
 */
@MappedSuperclass
public abstract class Model {

	@Version
	private Integer version;
	
	// Represents the selected state of the record in the UI widgets
	private transient boolean selected;

	public abstract Long getId();

	public abstract void setId(Long id);

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}
	
	/**
	 * Set the selected state of the record. The UI widget will use this flag to
	 * mark/unmark the selection state.
	 * 
	 * @param selected
	 *            selected state flag
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * Check whether the record is selected in the UI widget.
	 * 
	 * @return selection state
	 */
	public boolean isSelected() {
		return selected;
	}
	
	@Override
	public String toString() {
		Mapper mapper = Mapper.of(this.getClass());
		ToStringHelper ts = Objects.toStringHelper(this);

		ts.add("id", getId());
		
		int count = 0;
		for(Field f : getClass().getDeclaredFields()) {
			Property p = mapper.getProperty(f.getName());
			if (p == null ||
				p.isPrimary() ||
				p.isVersion() ||
				p.isVirtual() ||
				p.getTarget() != null ||
				p.getType() == PropertyType.BINARY ||
				p.getType() == PropertyType.TEXT ||
				p.get(this) == null)
				continue;
			ts.add(p.getName(), p.get(this));
			if (count++ == 10) break;
		}
		
		return ts.toString();
	}
}
