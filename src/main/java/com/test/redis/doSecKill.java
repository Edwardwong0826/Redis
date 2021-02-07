package com.test.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

public class doSecKill
{
    public static void getConnection()
    {
        Jedis jedis = new Jedis("127.0.0.1",6379);
        String result = jedis.ping();
        System.out.println(result);
        doSeckill("wong","1010");
        jedis.close();
    }

    public static boolean doSeckill(String uid, String productId)
    {
        String kcKey = "Seckill:" + productId + ":kc";
        String userKey = "Seckill:" + productId + ":user";

        Jedis jedis = new Jedis("127.0.0.1", 6379);
        jedis.watch(kcKey);

        String kc = jedis.get(kcKey);

        if(kc == null)
        {
            System.out.println("秒杀还没开始");
            jedis.close();
            return false;
        }

        if(jedis.sismember(userKey,uid))
        {
            System.out.println("已经秒杀成功，不能重复秒杀");
            jedis.close();
            return false;
        }

        if(Integer.parseInt(kc) <= 0)
        {
            System.out.println("秒杀已结束");
            jedis.close();
            return false;
        }

        Transaction transaction = jedis.multi();
        transaction.decr(kcKey);
        transaction.sadd(userKey,uid);

        List<Object> exec = transaction.exec();

        if(exec == null || exec.size() == 0)
        {
            System.out.println("秒杀失败");
            jedis.close();
            return false;
        }

        System.out.println("秒杀成功");
        jedis.close();
        return true;
    }

    public static void main(String[] args)
    {
        getConnection();
        Jedis jedis = new Jedis("127.0.0.1",6379);
        String s = jedis.get("Seckill:1010:kc");
        System.out.println(s);

    }
}
