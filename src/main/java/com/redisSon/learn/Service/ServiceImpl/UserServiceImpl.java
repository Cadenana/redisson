package com.redisSon.learn.Service.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.redisSon.learn.Service.UserService;
import com.redisSon.learn.util.BloomFilterUtil;
import com.redisSon.learn.entity.User;
import com.redisSon.learn.mapper.UserMapper;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    // 预期插入数量
    static long expectedInsertions = 200L;
    // 误判率
    static double falseProbability = 0.01;

    // 非法请求所返回的JSON
    static String illegalJson = "[\"com.company.springboot.entity.User\",{\"id\":null,\"userName\":\"null\",\"sex\":null,\"age\":null}]";

    private RBloomFilter<Long> bloomFilter = null;

    @Resource
    private BloomFilterUtil bloomFilterUtil;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserMapper userMapper;

@PostConstruct //表示该方法在Spring容器初始化时执行
    public void init()
{
    List<User> userList = userMapper.selectList(null);
    bloomFilter = bloomFilterUtil.create("idWhiteList", expectedInsertions, falseProbability);
    for (User user : userList) {
        bloomFilter.add(user.getId());
    }
}
//用在方法上
//@Cacheable方法执行结果被缓存，下次调用时先从缓存中获取结果
    //cacheNames或value表示存储在哪一区域
    //key存储的键 #id表示id为键
    //unless表达式，表达式为true则不缓存，如#result==null
//@CacheEvict方法被调用，从缓存中移除某条目，
    //cacheNames同上
    //key:要移除的缓存项的键
    //allEntries：是否移除全部
//@CachePut：执行方法后将结果更新缓存，
    //cacheNames 或 value: 同上，指定要更新缓存项的缓存名称。
    //key: 计算要更新的缓存项的键。
@Cacheable(cacheNames = "user", key = "#id", unless = "#result==null")
public User findById(Long id) {
    // bloomFilter中不存在该key,为非法访问
    if (!bloomFilter.contains(id)) {
        System.out.println("所要查询的数据既不在缓存中，也不在数据库中，为非法key");
        /**
         * 设置unless = "#result==null"并在非法访问的时候返回null的目的是不将该次查询返回的null使用
         * RedissonConfig-->RedisCacheManager-->RedisCacheConfiguration-->entryTtl设置的过期时间存入缓存。
         *
         * 因为那段时间太长了，在那段时间内可能该非法key又添加到bloomFilter，比如之前不存在id为1234567的用户，
         * 在那段时间可能刚好id为1234567的用户完成注册，使该key成为合法key。
         *
         * 所以我们需要在缓存中添加一个可容忍的短期过期的null或者是其它自定义的值,使得短时间内直接读取缓存中的该值。
         *
         * 因为Spring Cache本身无法缓存null，因此选择设置为一个其中所有值均为null的JSON，
         */
        redissonClient.getBucket("user::" + id, new StringCodec()).set(illegalJson, new Random().nextInt(200) + 300, TimeUnit.SECONDS);
        return null;
    }
    // 不是非法访问，可以访问数据库
    System.out.println("数据库中得到数据*****");
    return userMapper.selectById(id);
}

    // 先执行方法体中的代码，成功执行之后删除缓存
    @CacheEvict(cacheNames = "user", key = "#id")
    public boolean delete(Long id) {
        // 删除数据库中具有的数据,就算此key从此之后不再出现，也不能从布隆过滤器删除
        return userMapper.deleteById(id) == 1;
    }

    // 如果缓存中先前存在，则更新缓存;如果不存在，则将方法的返回值存入缓存
    @CachePut(cacheNames = "user", key = "#user.id")
    public User update(User user) {
        userMapper.updateById(user);
        // 新生成key的加入布隆过滤器，此key从此合法,因为该更新方法并不更新id,所以也不会产生新的合法的key
        bloomFilter.add(user.getId());
        return user;
    }

    @CachePut(cacheNames = "user", key = "#user.id")
    public User insert(User user) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getId, user.getId()));
        if (!users.isEmpty())
        {
            throw new RuntimeException("id已存在");
        }
        userMapper.insert(user);
        // 新生成key的加入布隆过滤器，此key从此合法
        bloomFilter.add(user.getId());
        return user;
    }
}
