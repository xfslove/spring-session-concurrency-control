package spring.session.concurrent.service;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by hanwen on 15-8-10.
 */
public interface PrincipalGetter {

	String getPrincipal(HttpServletRequest request);
}
