package spring.session.concurrent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import spring.session.concurrent.ext.KickOutRedirectUrlGetter;
import spring.session.concurrent.ext.MaxSessionCountGetter;
import spring.session.concurrent.ext.PrincipalExistDecider;
import spring.session.concurrent.ext.PrincipalGetter;
import spring.session.concurrent.ext.DefaultKickOutRedirectUrlGetter;
import spring.session.concurrent.ext.DefaultMaxSessionCountGetter;
import spring.session.concurrent.ext.DefaultPrincipalExistDecider;
import spring.session.concurrent.ext.DefaultPrincipalGetter;

/**
 * Created by hanwen on 15-7-30.
 */
@Configuration
@EnableRedisHttpSession
public class ConcurrentSessionConfiguration {

	@Bean
	@ConditionalOnMissingBean(PrincipalGetter.class)
	public PrincipalGetter principalGetter() {
		return new DefaultPrincipalGetter();
	}

	@Bean
	@ConditionalOnMissingBean(PrincipalExistDecider.class)
	public PrincipalExistDecider principalExistDecider() {
		return new DefaultPrincipalExistDecider();
	}

	@Bean
	@ConditionalOnMissingBean(MaxSessionCountGetter.class)
	public MaxSessionCountGetter maxSessionCountGetter() {
		return new DefaultMaxSessionCountGetter();
	}

	@Bean
	@ConditionalOnMissingBean(KickOutRedirectUrlGetter.class)
	public  KickOutRedirectUrlGetter kickOutRedirectUrlGetter() {
		return new DefaultKickOutRedirectUrlGetter();
	}

	@Bean
	public RedisTemplate<String,SessionInformation> sessionInformationRedisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, SessionInformation> template = new RedisTemplate<String, SessionInformation>();
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setConnectionFactory(connectionFactory);
		return template;
	}

	@Bean
	public SessionInformationRepository sessionInformationRepository(RedisTemplate<String, SessionInformation> sessionInformationRedisTemplate) {
		SessionInformationRepository sessionInformationRepository = new SessionInformationRepository(sessionInformationRedisTemplate);
		return sessionInformationRepository;
	}

	@Bean
	public SessionInformationRegisterFilter sessionInformationRegisterFilter(SessionInformationRepository sessionInformationRepository, PrincipalGetter principalGetter, MaxSessionCountGetter maxSessionCountGetter, PrincipalExistDecider principalExistDecider) {
		SessionInformationRegisterFilter sessionInformationRegisterFilter = new SessionInformationRegisterFilter(sessionInformationRepository, principalGetter, maxSessionCountGetter, principalExistDecider);
		return sessionInformationRegisterFilter;
	}

	@Bean
	public ConcurrentSessionControlFilter concurrentSessionControlFilter(SessionInformationRepository sessionInformationRepository, PrincipalGetter principalGetter, KickOutRedirectUrlGetter kickOutRedirectUrlGetter, PrincipalExistDecider principalExistDecider) {
		ConcurrentSessionControlFilter concurrentSessionControlFilter = new ConcurrentSessionControlFilter(sessionInformationRepository, principalGetter, kickOutRedirectUrlGetter, principalExistDecider);
		return concurrentSessionControlFilter;
	}
}
