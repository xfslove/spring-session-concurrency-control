package spring.session.concurrent;

import org.springframework.core.annotation.Order;

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
public class ConcurrentSessionControlFilter extends SessionInformationFilter {

	/** after {@link SessionInformationRegisterFilter#DEFAULT_ORDER}*/
	public static final int DEFAULT_ORDER = SessionInformationRegisterFilter.DEFAULT_ORDER + 1;

	public ConcurrentSessionControlFilter(SessionInformationRepository sessionInformationRepository, ConfigDataProvider configDataProvider) {
		super(sessionInformationRepository, configDataProvider);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		if (session != null && session.getAttribute(configDataProvider.getPrincipalAttr()) != null) {
			SessionInformation info =
					sessionInformationRepository.getSessionInformation(
							session.getId(),
							session.getAttribute(configDataProvider.getPrincipalAttr()).toString()
					);
			if (info != null) {
				if (info.isExpired()) {
					// Expired - abort processing
					String targetUrl = configDataProvider.getTargetUrl();
					if (targetUrl != null) {
						//	redirect to targetUrl
						session.invalidate();
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
