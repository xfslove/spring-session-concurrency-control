package spring.session.concurrent;

import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

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

	/** after {@link SessionRegisterFilter#DEFAULT_ORDER}*/
	public static final int DEFAULT_ORDER = Integer.MIN_VALUE + 52;

	private SessionRepository sessionRepository;

	public ConcurrentSessionControlFilter(SessionRepository sessionRepository) {
		this.sessionRepository = sessionRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		if (session != null && session.getAttribute(SessionRepository.VALUE_KEY_PREFIX) != null) {
			SessionInformation info =
					sessionRepository.getSessionInformation(
							session.getId(),
							session.getAttribute(SessionRepository.VALUE_KEY_PREFIX).toString()
					);
			if (info != null) {
				if (info.isExpired()) {
					// Expired - abort processing
					// doLogout
					String targetUrl = null;
					if (targetUrl != null) {
						//	redirect to targetUrl
						return;
					} else {
						response.getWriter().print("This session has been expired (possibly due to multiple concurrent " +
								"logins being attempted as the same user).");
						response.flushBuffer();
					}
					return;
				} else {
					// Non-expired - update last request date/time
					sessionRepository.refreshLastRequest(info.getSessionId());
				}
			}
		}
		filterChain.doFilter(request, response);
	}
}