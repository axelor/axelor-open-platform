package com.axelor.meta.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("chart")
public class ChartView extends AbstractView {
	
	@XmlAttribute
	private Boolean stacked;

	@XmlAttribute
	private String categoryKey;
	
	@XmlAttribute
	private String categoryType;
	
	@XmlElement
	private ChartQuery query;
	
	@XmlElement
	private List<ChartSeries> series;
	
	public Boolean getStacked() {
		return stacked;
	}

	public ChartQuery getQuery() {
		return query;
	}
	
	public String getCategoryKey() {
		return categoryKey;
	}
	
	public String getCategoryType() {
		return categoryType;
	}
	
	public List<ChartSeries> getSeries() {
		return series;
	}
	
	@XmlType
	public static class ChartQuery {
		
		@XmlAttribute(name = "native")
		private Boolean nativeQuery;
		
		@XmlValue
		private String text;
		
		public Boolean getNativeQuery() {
			return nativeQuery;
		}
		
		public String getText() {
			return text;
		}
	}

	@XmlType
	@JsonTypeName("series")
	public static class ChartSeries {
		
		@XmlAttribute
		private String key;
		
		@XmlAttribute
		private String expr;
		
		@XmlAttribute
		private String type;
		
		@XmlAttribute
		private String side;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getExpr() {
			return expr;
		}

		public void setExpr(String expr) {
			this.expr = expr;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getSide() {
			return side;
		}

		public void setSide(String side) {
			this.side = side;
		}
	}
}
