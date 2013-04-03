package com.axelor.web;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.meta.MetaLoader;

@Singleton
public class InitServlet extends HttpServlet {
	
	private static final long serialVersionUID = -2493577642638670615L;
	
	private static final Logger LOG = LoggerFactory.getLogger(InitServlet.class);
	
	@Inject
	private AppSettings settings;
	
	@Inject
	private MetaLoader metaLoader;

	@Override
	public void init() throws ServletException {
		LOG.info("Initializing...");
		try {
			String output = settings.get("temp.dir");
			metaLoader.load(output);
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.init();
	}
}
