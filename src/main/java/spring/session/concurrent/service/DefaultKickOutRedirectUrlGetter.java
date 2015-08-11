package spring.session.concurrent.service;

import spring.session.concurrent.service.KickOutRedirectUrlGetter;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by hanwen on 15-8-10.
 */
public class DefaultKickOutRedirectUrlGetter implements KickOutRedirectUrlGetter {

	@Override
	public String getRedirectUrl(HttpServletRequest request) {
		return "/";
	}
}
