package spring.session.concurrent;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import spring.session.concurrent.service.DefaultMaxSessionCountGetter;
import spring.session.concurrent.service.DefaultPrincipalExistDecider;
import spring.session.concurrent.service.DefaultPrincipalGetter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;

/**
 * Created by hanwen on 15-8-5.
 */
public class SessionInformationRegisterFilterTest {

	@Mock
	SessionInformationRepository sessionInformationRepository;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private MockFilterChain chain;

	private SessionInformationRegisterFilter filter;

	private String PRINCIPAL_VAL_1 = "user1";

	private String PRINCIPAL_ATTR = "principal";

	@BeforeMethod(alwaysRun = true)
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.filter = new SessionInformationRegisterFilter(sessionInformationRepository, new DefaultPrincipalGetter(), new DefaultMaxSessionCountGetter(), new DefaultPrincipalExistDecider());
		setupRequest(PRINCIPAL_VAL_1);
	}

	@Test
	public void testRegisterNewOne() throws Exception {
		when(sessionInformationRepository.getAllSessions(request.getSession().getAttribute(PRINCIPAL_ATTR).toString(), false))
				.thenReturn(new ArrayList<SessionInformation>());
		doFilter(new DoInFilter() {
			@Override
			public void doFilter(HttpServletRequest wrappedRequest, HttpServletResponse wrappedResponse) throws IOException {
				verify(sessionInformationRepository).registerNewSessionInformation(request.getSession().getId(), request.getSession().getAttribute(PRINCIPAL_ATTR).toString());
				assertNotNull(response);
			}
		});
	}

	@Test
	public void TestExpiredOldOne() throws Exception {
		final List<SessionInformation> sessionInformationList = new ArrayList<SessionInformation>();
		sessionInformationList.add(new SessionInformation(PRINCIPAL_VAL_1, request.getSession().getId(), 1l));
		when(sessionInformationRepository.getAllSessions(request.getSession().getAttribute(PRINCIPAL_ATTR).toString(), false))
				.thenReturn(sessionInformationList);
		doFilter(new DoInFilter() {
			@Override
			public void doFilter(HttpServletRequest wrappedRequest, HttpServletResponse wrappedResponse) throws IOException {
				verify(sessionInformationRepository).registerNewSessionInformation(request.getSession().getId(), request.getSession().getAttribute(PRINCIPAL_ATTR).toString());
				assertNotNull(response);
			}
		});

		final String id1 = request.getSession().getId();
		nextRequest(PRINCIPAL_VAL_1);

		sessionInformationList.add(new SessionInformation(PRINCIPAL_VAL_1, request.getSession().getId(), 2l));
		doFilter(new DoInFilter() {
			@Override
			public void doFilter(HttpServletRequest wrappedRequest, HttpServletResponse wrappedResponse) throws IOException {
				verify(sessionInformationRepository).registerNewSessionInformation(request.getSession().getId(), request.getSession().getAttribute(PRINCIPAL_ATTR).toString());
				verify(sessionInformationRepository).expireNow(id1);
				assertNotNull(response);
			}
		});
	}

	private void setupRequest(String principal) {
		request = new MockHttpServletRequest();
		request.getSession().setAttribute(PRINCIPAL_ATTR, principal);
		response = new MockHttpServletResponse();
		chain = new MockFilterChain();
	}

	private void nextRequest(String principal) throws Exception {
		Map<String, Cookie> nameToCookie = new HashMap<String, Cookie>();
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				nameToCookie.put(cookie.getName(), cookie);
			}
		}
		if (response.getCookies() != null) {
			for (Cookie cookie : response.getCookies()) {
				nameToCookie.put(cookie.getName(), cookie);
			}
		}
		Cookie[] nextRequestCookies = new ArrayList<Cookie>(nameToCookie.values()).toArray(new Cookie[0]);

		setupRequest(principal);

		request.setCookies(nextRequestCookies);
	}

	@SuppressWarnings("serial")
	private void doFilter(final DoInFilter doInFilter) throws ServletException, IOException {
		chain = new MockFilterChain(new HttpServlet() {
		}, new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
				doInFilter.doFilter(request, response);
			}
		});
		filter.doFilter(request, response, chain);
	}

	abstract class DoInFilter {
		void doFilter(HttpServletRequest wrappedRequest, HttpServletResponse wrappedResponse) throws ServletException, IOException {}
	}
}
