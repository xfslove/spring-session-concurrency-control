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
	public ConcurrentRepository concurrentRepository(RedisTemplate<String, SessionInformation> sessionInformationRedisTemplate) {
		ConcurrentRepository concurrentRepository = new ConcurrentRepository(sessionInformationRedisTemplate);
		return concurrentRepository;
	}

	@Bean
	public ConcurrentRegisterFilter concurrentRegisterFilter(ConcurrentRepository concurrentRepository) {
		ConcurrentRegisterFilter concurrentRegisterFilter = new ConcurrentRegisterFilter(concurrentRepository);
		return concurrentRegisterFilter;
	}

	@Bean
	public ConcurrentControlFilter concurrentControlFilter(ConcurrentRepository concurrentRepository) {
		ConcurrentControlFilter ConcurrentControlFilter = new ConcurrentControlFilter(concurrentRepository);
		return ConcurrentControlFilter;
	}
}
