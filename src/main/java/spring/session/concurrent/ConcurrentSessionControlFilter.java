package spring.session.concurrent;

import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import spring.session.concurrent.ext.KickOutRedirectUrlGetter;
import spring.session.concurrent.ext.LogoutDecider;
import spring.session.concurrent.ext.PrincipalExistDecider;
import spring.session.concurrent.ext.PrincipalGetter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Created by hanwen on 15-7-30.
 */
@Order(ConcurrentSessionControlFilter.DEFAULT_ORDER)
public class ConcurrentSessionControlFilter extends OncePerRequestFilter {

	/**
	 * after {@link SessionInformationRegisterFilter#DEFAULT_ORDER}
	 */
	public static final int DEFAULT_ORDER = SessionInformationRegisterFilter.DEFAULT_ORDER + 1;

	private SessionInformationRepository sessionInformationRepository;

	private PrincipalGetter principalGetter;

	private KickOutRedirectUrlGetter kickOutRedirectUrlGetter;

	private PrincipalExistDecider principalExistDecider;

  private LogoutDecider logoutDecider;

	public ConcurrentSessionControlFilter(
      SessionInformationRepository sessionInformationRepository,
      PrincipalGetter principalGetter,
      KickOutRedirectUrlGetter kickOutRedirectUrlGetter,
      PrincipalExistDecider principalExistDecider,
      LogoutDecider logoutDecider
  ) {
		this.sessionInformationRepository = sessionInformationRepository;
		this.principalGetter = principalGetter;
		this.kickOutRedirectUrlGetter = kickOutRedirectUrlGetter;
		this.principalExistDecider = principalExistDecider;
    this.logoutDecider = logoutDecider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		if (principalExistDecider.isExistPrincipal(request)) {
			SessionInformation info =
					sessionInformationRepository.getSessionInformation(
							session.getId(),
							principalGetter.getPrincipal(request)
					);
			if (info != null) {
				if (info.isExpired()) {

          // logout
          logoutDecider.doLogout(request);

          // Expired - abort processing
          String targetUrl = kickOutRedirectUrlGetter.getRedirectUrl(request);
          if (targetUrl != null) {
            //	redirect to targetUrl
						response.sendRedirect(targetUrl);
            
						return;
					} else {
						response.getWriter().print("This session has been expired (possibly due to multiple concurrent " +
								"logins being attempted as the same user).");
						response.flushBuffer();
					}
					return;
				} else {
					// Non-expired - update last request date/time
					sessionInformationRepository.refreshLastRequest(info.getSessionId());
				}
			}
		}
		filterChain.doFilter(request, response);
	}
}
