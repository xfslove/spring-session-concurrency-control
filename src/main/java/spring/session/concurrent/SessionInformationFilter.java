package spring.session.concurrent;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by hanwen on 15-8-6.
 */
public class SessionInformationFilter extends OncePerRequestFilter {

	protected ConfigDataProvider configDataProvider;

	protected SessionInformationRepository sessionInformationRepository;

	public SessionInformationFilter(SessionInformationRepository sessionInformationRepository, ConfigDataProvider configDataProvider) {
		if(sessionInformationRepository == null) {
			throw new IllegalArgumentException("sessionInformationRepository cannot be null");
		}
		if(configDataProvider == null) {
			throw new IllegalArgumentException("configDataProvider cannot be null");
		}
		this.configDataProvider = configDataProvider;
		this.sessionInformationRepository = sessionInformationRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

	}


}
