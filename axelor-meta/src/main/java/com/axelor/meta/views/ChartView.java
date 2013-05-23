package com.axelor.meta.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.axelor.db.JPA;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("chart")
public class ChartView extends AbstractView {
	
	@XmlAttribute
	private Boolean stacked;

	@XmlElement(name = "dataset")
	private ChartQuery query;
	
	@XmlElement
	private ChartCategory category;

	@XmlElement
	private List<ChartSeries> series;
	
	public Boolean getStacked() {
		return stacked;
	}

	public ChartQuery getQuery() {
		return query;
	}

	public ChartCategory getCategory() {
		return category;
	}

	public List<ChartSeries> getSeries() {
		return series;
	}
	
	@XmlType
	public static class ChartQuery {
		
		@XmlAttribute
		private String type;
		
		@XmlValue
		private String text;
		
		public String getType() {
			return type;
		}
		
		public String getText() {
			return text;
		}
	}
	
	@XmlType
	@JsonTypeName("category")
	public static class ChartCategory {
		
		@XmlAttribute
		private String key;
		
		@XmlAttribute
		private String type;
		
		@XmlAttribute
		private String title;
		
		public String getKey() {
			return key;
		}
		
		public String getType() {
			return type;
		}

		public String getDefaultTitle() {
			return title;
		}

		public String getTitle() {
			return JPA.translate(title);
		}
	}

	@XmlType
	@JsonTypeName("series")
	public static class ChartSeries {
		
		@XmlAttribute
		private String key;
		
		@XmlAttribute
		private String groupBy;
		
		@XmlAttribute
		private String type;
		
		@XmlAttribute
		private String side;

		@XmlAttribute
		private String title;
		
		@XmlAttribute
		private String aggregate;

		public String getKey() {
			return key;
		}

		public String getGroupBy() {
			return groupBy;
		}

		public String getType() {
			return type;
		}

		public String getSide() {
			return side;
		}

		public String getDefaultTitle() {
			return title;
		}

		public String getTitle() {
			return JPA.translate(title);
		}
		
		public String getAggregate() {
			return aggregate;
		}
	}
}
