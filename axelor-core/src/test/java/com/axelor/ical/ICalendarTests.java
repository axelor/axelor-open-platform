package com.axelor.ical;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;

import net.fortuna.ical4j.connector.dav.CalDavCalendarCollection;
import net.fortuna.ical4j.connector.dav.PathResolver;

import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;

import com.axelor.JpaTest;
import com.axelor.common.ClassUtils;
import com.axelor.ical.db.ICalendar;
import com.axelor.ical.db.ICalendarEvent;
import com.google.inject.Inject;

public class ICalendarTests extends JpaTest {

	@Inject
	private ICalendarService service;

	private static final String SCHEME = "https";
	private static final String HOST = "www.google.com";
	private static final int PORT = 443;
	private static final PathResolver RESOLVER = PathResolver.GCAL;

	private static final String USER = null;
	private static final String PASS = null;

	@Test
	public void testLoad() throws Exception {

		ICalendar cal = new ICalendar();

		// load from reader
		InputStream in = ClassUtils.getResourceStream("test.ics");
		Reader reader = new InputStreamReader(in);
		try {
			service.load(cal, reader);
		} finally {
			reader.close();
		}

		Assert.assertNotNull(cal.getEvents());

		// export to writer
		StringWriter writer = new StringWriter();
		service.export(cal, writer);
		String text = writer.toString();

		Assert.assertTrue(text.contains("BEGIN:VEVENT"));
	}

	@Test
	public void testSync() throws Exception {

		if (USER == null) {
			return;
		}

		ICalendar cal = new ICalendar();
		cal.setName("My Calendar");

		ICalendarEvent e1 = new ICalendarEvent();
		e1.setSummary("Hello...");
		e1.setStartDate(new LocalDateTime());
		e1.setEndDate(e1.getStartDate().plusHours(1));
		cal.addEvent(e1);

		URL url = new URL(SCHEME, HOST, PORT, "");
		ICalendarStore store = new ICalendarStore(url, RESOLVER);

		store.connect(USER, PASS);
		try {
			CalDavCalendarCollection collection = store.getCollections().get(0);
			cal = service.sync(cal, collection);
		} finally {
			store.disconenct();
		}
	}
}
