package spring.session.concurrent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Created by hanwen on 15-7-30.
 */
@Configuration
public class ConcurrentConfiguration {

	@Bean
	public RedisTemplate<String,SessionInformation> sessionInformationRedisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, SessionInformation> template = new RedisTemplate<String, SessionInformation>();
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setConnectionFactory(connectionFactory);
		return template;
	}

	@Bean
	public SessionRepository sessionRepository(RedisTemplate<String, SessionInformation> sessionInformationRedisTemplate) {
		SessionRepository sessionRepository = new SessionRepository(sessionInformationRedisTemplate);
		return sessionRepository;
	}

	@Bean
	public SessionRegisterFilter sessionRegisterFilter(SessionRepository sessionRepository) {
		SessionRegisterFilter sessionRegisterFilter = new SessionRegisterFilter(sessionRepository);
		return sessionRegisterFilter;
	}

	@Bean
	public ConcurrentSessionControlFilter concurrentSessionControlFilter(SessionRepository sessionRepository) {
		ConcurrentSessionControlFilter concurrentSessionControlFilter = new ConcurrentSessionControlFilter(sessionRepository);
		return concurrentSessionControlFilter;
	}
}
