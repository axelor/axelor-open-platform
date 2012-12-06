package com.axelor.wkf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.wkf.service.WkfService;

public class WkfRunner {
	
	private static final Logger LOG = LoggerFactory.getLogger(WkfRunner.class);

	public static <T extends Model> void runBean(final T bean){
		
		LOG.debug("Run workflow for {}...", bean.getClass().getName());
		
		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {
				try {
					
					WkfService wkf = new WkfService();
					wkf.run(bean);
					
				} catch (Exception e) {
					
					e.printStackTrace();
					
				}
			}
			
		});
		
	}

	public static void runClass(final Class<? extends Model> klass){
		
		LOG.debug("Run workflow for {}...", klass.getName());
		
		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {
				try {
					
					WkfService wkf = new WkfService();
					wkf.run(klass);
					
				} catch (Exception e) {
					
					e.printStackTrace();
					
				}
			}
			
		});
		
	}

}
