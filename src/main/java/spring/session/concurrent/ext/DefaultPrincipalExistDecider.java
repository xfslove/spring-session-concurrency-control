package spring.session.concurrent.ext;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by hanwen on 15-8-10.
 */
public class DefaultPrincipalExistDecider implements PrincipalExistDecider {

	@Override
	public boolean isExistPrincipal(HttpServletRequest request) {
		if (null == request) {
			return false;
		}
		if (null == request.getSession()) {
			return false;
		}
		if (null == request.getSession().getAttribute("principal")) {
			return false;
		}
		return true;
	}
}
