package spring.session.concurrent.service.defaultImpl;

import spring.session.concurrent.service.PrincipalGetter;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by hanwen on 15-8-10.
 */
public class DefaultPrincipalGetter implements PrincipalGetter {

	@Override
	public String getPrincipal(HttpServletRequest request) {
		if (null == request) {
			return null;
		}
		if (null == request.getSession()) {
			return null;
		}
		if (null == request.getSession().getAttribute("principal")) {
			return null;
		}
		return request.getSession().getAttribute("principal").toString();
	}
}
