package spring.session.concurrent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Created by hanwen on 15-7-30.
 */
@Configuration
@EnableRedisHttpSession
public class SessionConfiguration {

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
	public SessionInformationRegisterFilter sessionInformationRegisterFilter(SessionInformationRepository sessionInformationRepository) {
		SessionInformationRegisterFilter sessionInformationRegisterFilter = new SessionInformationRegisterFilter(sessionInformationRepository);
		return sessionInformationRegisterFilter;
	}

	@Bean
	public ConcurrentSessionControlFilter concurrentSessionControlFilter(SessionInformationRepository sessionInformationRepository) {
		ConcurrentSessionControlFilter concurrentSessionControlFilter = new ConcurrentSessionControlFilter(sessionInformationRepository);
		return concurrentSessionControlFilter;
	}
}
