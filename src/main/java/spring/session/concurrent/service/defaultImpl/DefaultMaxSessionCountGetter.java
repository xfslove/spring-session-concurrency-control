package spring.session.concurrent.service.defaultImpl;

import spring.session.concurrent.service.MaxSessionCountGetter;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by hanwen on 15-8-10.
 */
public class DefaultMaxSessionCountGetter implements MaxSessionCountGetter {

	@Override
	public int getMaximumSessions(HttpServletRequest request) {
		return 1;
	}
}
