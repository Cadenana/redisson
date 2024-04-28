package com.redisSon.learn.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Random;
import java.time.Duration;

@Configuration
@EnableCaching
public class RedissonConfig {
    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private String port;

    @Value("${spring.redis.password}")
    private String password;
@Bean(destroyMethod = "shutdown")
    public RedissonClient redisson()
    {
        Config config=new Config();
        //redis没有密码这里不用设置
        config.useSingleServer().setAddress("redis://"+host+":"+port)
                .setTimeout(5000);
        return Redisson.create(config);
    }
@Bean
    public CacheManager RedisCacheManager(RedisConnectionFactory factory)
{
    RedisSerializer<String> redisSerializer=new StringRedisSerializer();
    Jackson2JsonRedisSerializer jackson2JsonRedisSerializer=new Jackson2JsonRedisSerializer(Object.class);
    ObjectMapper om=new ObjectMapper();
    om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    /**
     * 新版本中om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)已经被废弃
     * 建议替换为om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL)
     */
    om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    jackson2JsonRedisSerializer.setObjectMapper(om);
    // 配置序列化解决乱码的问题
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            // 设置缓存过期时间  为解决缓存雪崩,所以将过期时间加随机值
            .entryTtl(Duration.ofSeconds(60 * 60 + new Random().nextInt(60 * 10)))
            // 设置key的序列化方式
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
            // 设置value的序列化方式
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));
    // .disableCachingNullValues(); //为防止缓存击穿，所以允许缓存null值
    RedisCacheManager cacheManager = RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            // 启用RedisCache以将缓存 put/evict 操作与正在进行的 Spring 管理的事务同步
            .transactionAware()
            .build();
    return cacheManager;

}

}
