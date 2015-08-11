package spring.session.concurrent.ext;

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
