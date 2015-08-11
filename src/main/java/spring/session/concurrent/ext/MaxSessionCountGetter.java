package spring.session.concurrent.ext;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by hanwen on 15-8-10.
 */
public interface MaxSessionCountGetter {

	int getMaximumSessions(HttpServletRequest request);
}
