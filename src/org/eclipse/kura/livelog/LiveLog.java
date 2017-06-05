/**
 * author: dario Maranta
 */

package org.eclipse.kura.livelog;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;

public class LiveLog {

	private static final String APP_ID = "LIVE_LOG";
	private static final String TOPIC = "log";
	private static final int QoS = 0;
	private static final Boolean RETAIN = false;
	private static final String METRIC = "log_line";
	private static final int PUB_RATE = 1;

	private CloudService m_cloudService;
	private CloudClient m_cloudClient;
	private ScheduledExecutorService m_worker;
	private String strLine;
	private SortedSet<String> set = new TreeSet<String>();
	private String ultimateLineInsert;
	private int year = Calendar.getInstance().get(Calendar.YEAR);

	public LiveLog() {

		super();

		m_worker = Executors.newSingleThreadScheduledExecutor();

	}

	public void setCloudService(CloudService cloudService) {
		m_cloudService = cloudService;
	}

	public void unsetCloudService(CloudService cloudService) {
		m_cloudService = null;
	}

	protected void activate(ComponentContext componentContext) {

		try {
			m_cloudClient = m_cloudService.newCloudClient(APP_ID);
		} catch (KuraException e) {
		}

		m_worker.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				publish();
			}
		}, 0, PUB_RATE, TimeUnit.MINUTES);

	}

	protected void deactivate(ComponentContext componentContext) {

		m_worker.shutdown();

	}

	private void publish() {

		tail();

		Iterator<String> it = set.iterator();

		if (ultimateLineInsert != null) {
			while (!it.next().equals(ultimateLineInsert)) {
			}
		}

		while (it.hasNext()) {
			KuraPayload payload = new KuraPayload();
			payload.setTimestamp(new Date());

			ultimateLineInsert = it.next();

			payload.addMetric(METRIC, ultimateLineInsert);

			try {
				m_cloudClient.publish(TOPIC, payload, QoS, RETAIN);
			} catch (KuraException e) {
			}

		}
	}

	private void tail() {

		try {
			FileInputStream fstream = new FileInputStream("/var/log/kura.log");
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

			while ((strLine = br.readLine()) != null) {

				if (!strLine.contains(APP_ID) && strLine.contains(Integer.toString(year))) {
					set.add(strLine.substring(0, 16) + " " + strLine.substring(24));
				}
			}

			fstream.close();

		} catch (Exception e) {
		}
	}
}