package com.axelor.meta.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("calendar")
public class CalendarView extends AbstractView {
	
	@XmlAttribute
	private String mode;
	
	@XmlAttribute
	private String colorBy;
	
	@XmlAttribute
	private String eventStart;
	
	@XmlAttribute
	private String eventStop;
	
	@XmlAttribute
	private Integer eventLength;

	@XmlElement(name="field", type=Field.class)
	private List<AbstractWidget> items;

	public List<AbstractWidget> getItems() {
		return items;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getColorBy() {
		return colorBy;
	}

	public void setColorBy(String colorBy) {
		this.colorBy = colorBy;
	}

	public String getEventStart() {
		return eventStart;
	}

	public void setEventStart(String eventStart) {
		this.eventStart = eventStart;
	}

	public String getEventStop() {
		return eventStop;
	}

	public void setEventStop(String eventStop) {
		this.eventStop = eventStop;
	}

	public Integer getEventLength() {
		return eventLength;
	}

	public void setEventLength(Integer eventLength) {
		this.eventLength = eventLength;
	}

	public void setItems(List<AbstractWidget> items) {
		this.items = items;
	}
}
