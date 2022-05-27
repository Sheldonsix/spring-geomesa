package cn.edu.whut.springgeomesa.config.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName RedisConfig
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/23 15:17
 * @Version 1.0
 **/
@Configuration
@EnableCaching
public class RedisConfig {
    /**
     * @author sheldon
     * @date 2022/5/23
     * @description 重新配置 RedisTemplate<String, Object>
     * @param: factory
     * @return org.springframework.data.redis.core.RedisTemplate<java.lang.String,java.lang.Object>
     **/
    @Bean(name = "template")
    public RedisTemplate<String, Object> template(RedisConnectionFactory factory) {
        // 创建 RedisTemplate<String, Object> 对象
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 配置连接工厂
        template.setConnectionFactory(factory);
        // 定义 Jackson2JsonRedisSerializer 序列化对象
        Jackson2JsonRedisSerializer<Object> jacksonSerial = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        // 指定要序列化的域，field、get 和 set，以及修饰符范围，ANY 是包含 private 和 public
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 指定序列化输入的类型，类必须是非 final 修饰的，否则会报异常
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);
        jacksonSerial.setObjectMapper(om);
        StringRedisSerializer stringSerial = new StringRedisSerializer();
        // redis key 序列化方式使用 stringSerial
        template.setKeySerializer(stringSerial);
        // redis value 序列化方式使用 Jackson
        template.setValueSerializer(jacksonSerial);
        // redis hash key 序列化方式使用 stringSerial
        template.setHashKeySerializer(stringSerial);
        // redis hash value 序列化方式使用 Jackson
        template.setHashValueSerializer(jacksonSerial);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * @author sheldon
     * @date 2022/5/23
     * @description redis string
     * @param: redisTemplate
     * @return org.springframework.data.redis.core.ValueOperations<java.lang.String,java.lang.Object>
     **/
    @Bean
    public ValueOperations<String, Object> valueOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForValue();
    }

    /**
     * @author sheldon
     * @date 2022/5/23
     * @description redis hash
     * @param: redisTemplate
     * @return org.springframework.data.redis.core.HashOperations<java.lang.String,java.lang.String,java.lang.Object>
     **/
    @Bean
    public HashOperations<String, String, Object> hashOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForHash();
    }

    /**
     * @author sheldon
     * @date 2022/5/23
     * @description redis list
     * @param: redisTemplate
     * @return org.springframework.data.redis.core.ListOperations<java.lang.String,java.lang.Object>
     **/
    @Bean
    public ListOperations<String, Object> listOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForList();
    }

    /**
     * @author sheldon
     * @date 2022/5/23
     * @description redis set
     * @param: redisTemplate
     * @return org.springframework.data.redis.core.SetOperations<java.lang.String,java.lang.Object>
     **/
    @Bean
    public SetOperations<String, Object> setOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForSet();
    }

    /**
     * @author sheldon
     * @date 2022/5/23
     * @description redis zset
     * @param: redisTemplate
     * @return org.springframework.data.redis.core.ZSetOperations<java.lang.String,java.lang.Object>
     **/
    @Bean
    public ZSetOperations<String, Object> zSetOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForZSet();
    }

}
