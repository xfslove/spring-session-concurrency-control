package spring.session.concurrent;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import spring.session.concurrent.ext.DefaultKickOutRedirectUrlGetter;
import spring.session.concurrent.ext.DefaultPrincipalExistDecider;
import spring.session.concurrent.ext.DefaultPrincipalGetter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;

/**
 * Created by hanwen on 15-8-4.
 */
public class ConcurrentSessionControlFilterTest {

	@Mock
	SessionInformationRepository sessionInformationRepository;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private MockFilterChain chain;

	private ConcurrentSessionControlFilter filter;

	private String PRINCIPAL_ATTR = "principal";

	private String PRINCIPAL_VAL_1 = "user1";

	private String PRINCIPAL_VAL_2 = "user2";

	@BeforeMethod(alwaysRun = true)
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.filter = new ConcurrentSessionControlFilter(sessionInformationRepository, new DefaultPrincipalGetter(), new DefaultKickOutRedirectUrlGetter(), new DefaultPrincipalExistDecider());
		setupRequest(PRINCIPAL_VAL_1);
	}

	@Test
	public void testFilterRefreshSession() throws Exception {
		final SessionInformation si1 = new SessionInformation(request.getSession().getAttribute(PRINCIPAL_ATTR).toString(), request.getSession().getId(), 1l, false);
		when(sessionInformationRepository.getSessionInformation(request.getSession().getId(), request.getSession().getAttribute(PRINCIPAL_ATTR).toString())).thenReturn(si1);
		doFilter(new DoInFilter() {
			@Override
			public void doFilter(HttpServletRequest wrappedRequest, HttpServletResponse wrappedResponse) throws IOException {
				verify(sessionInformationRepository).getSessionInformation(request.getSession().getId(), request.getSession().getAttribute(PRINCIPAL_ATTR).toString());
				assertNotNull(response);
				verify(sessionInformationRepository).refreshLastRequest(request.getSession().getId());
			}
		});
	}

	@Test
	public void testFilterExistSession() throws Exception {
		final SessionInformation si1 = new SessionInformation(request.getSession().getAttribute(PRINCIPAL_ATTR).toString(), request.getSession().getId(), 1l, false);
		when(sessionInformationRepository.getSessionInformation(request.getSession().getId(), request.getSession().getAttribute(PRINCIPAL_ATTR).toString())).thenReturn(si1);
		doFilter(new DoInFilter() {
			@Override
			public void doFilter(HttpServletRequest wrappedRequest, HttpServletResponse wrappedResponse) throws IOException {
				verify(sessionInformationRepository).getSessionInformation(request.getSession().getId(), request.getSession().getAttribute(PRINCIPAL_ATTR).toString());
				assertNotNull(response);
			}
		});

		nextRequest(PRINCIPAL_VAL_1);

		final SessionInformation si2 = new SessionInformation(request.getSession().getAttribute(PRINCIPAL_ATTR).toString(), request.getSession().getId(), 1l, true);
		when(sessionInformationRepository.getSessionInformation(request.getSession().getId(), request.getSession().getAttribute(PRINCIPAL_ATTR).toString())).thenReturn(si2);
		doFilter(new DoInFilter() {
			@Override
			public void doFilter(HttpServletRequest wrappedRequest, HttpServletResponse wrappedResponse) throws IOException {
				// verify an error parameter, because the response committed, will not exec the filter.
				verify(sessionInformationRepository).getSessionInformation(request.getSession().getId(), "error");
			}
		});
	}

	@Test
	public void testFilterNotExistSession() throws Exception {
		final SessionInformation si1 = new SessionInformation(request.getSession().getAttribute(PRINCIPAL_ATTR).toString(), request.getSession().getId(), 1l, false);
		when(sessionInformationRepository.getSessionInformation(request.getSession().getId(), request.getSession().getAttribute(PRINCIPAL_ATTR).toString())).thenReturn(si1);
		doFilter(new DoInFilter() {
			@Override
			public void doFilter(HttpServletRequest wrappedRequest, HttpServletResponse wrappedResponse) throws IOException {
				verify(sessionInformationRepository).getSessionInformation(request.getSession().getId(), request.getSession().getAttribute(PRINCIPAL_ATTR).toString());
				assertNotNull(response);
			}
		});

		nextRequest(PRINCIPAL_VAL_2);

		final SessionInformation si2 = new SessionInformation(request.getSession().getAttribute(PRINCIPAL_ATTR).toString(), request.getSession().getId(), 1l, true);
		when(sessionInformationRepository.getSessionInformation(request.getSession().getId(), request.getSession().getAttribute(PRINCIPAL_ATTR).toString())).thenReturn(si2);
		doFilter(new DoInFilter() {
			@Override
			public void doFilter(HttpServletRequest wrappedRequest, HttpServletResponse wrappedResponse) throws IOException {
				verify(sessionInformationRepository).getSessionInformation(request.getSession().getId(), request.getSession().getAttribute(PRINCIPAL_ATTR).toString());
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
