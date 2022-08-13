package com.test.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;

import java.util.Map;

public class TestRedis
{
    public static void main(String[] args)
    {

        JedisShardInfo shardInfo = new JedisShardInfo("127.0.0.1",6379);
        shardInfo.setPassword("61376554");
        Jedis jedis = new Jedis(shardInfo);
        String result = jedis.ping();
        System.out.println(result);

        jedis.set("a","abc");
        String a = jedis.get("a");
        System.out.println(a);

        Map<String, String> userInfo = jedis.hgetAll("userInfo");
        System.out.println(userInfo);

        jedis.close();
    }
}
