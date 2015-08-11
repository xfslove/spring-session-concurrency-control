package spring.session.concurrent.ext;

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
