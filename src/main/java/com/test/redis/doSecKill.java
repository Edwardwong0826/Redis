package com.test.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Transaction;

import java.util.List;

public class doSecKill
{

    // redis distributed lock features
    // 1. mutual exclusion
    // 2. no dead lock
    // 3. fault tolerance - if old master node is down, the lock information is not replicate to slave node then slave node promote to new master node
    // 4. add lock and delete lock need to be same client/server
    // 5. lock cannot expire itself when normal operating
    // if we use watch, multi(transaction), UUID and lua script(to ensure atomicity operation) we can have 1,2,4 feature and solve oversold and 库存遗留问题
    // Reddison is a redis Java client with features of In-Memory Data Grid can do above all, inside also use LUA script, with features like
    // 可重入锁（Reentrant Lock）、公平锁（Fair Lock、联锁（MultiLock）、 红锁（RedLock）、 读写锁（ReadWriteLock）等
    // by using Reddison, we can have feature 3(using RedLock) and 5(watchdog to auto extend lock expire time when not set)
    public static void getConnection()
    {
        JedisShardInfo shardInfo = new JedisShardInfo("127.0.0.1",6379);
        shardInfo.setPassword("61376554");
        Jedis jedis = new Jedis(shardInfo);
        jedis.connect();


        String result = jedis.ping();
        System.out.println(result);
        doSeckill("wong","1010");
        jedis.close();
    }

    public static boolean doSeckill(String uid, String productId)
    {
        String kcKey = "Seckill:" + productId + ":quantity";
        String userKey = "Seckill:" + productId + ":user";

        // use watch and multi to solve oversold problem
        // redis watch use optimistic lock mechanism
        // we can Jmeter or ApacheBench to test multi thread concurrent scenario in redis
        JedisShardInfo shardInfo = new JedisShardInfo("127.0.0.1",6379);
        shardInfo.setPassword("61376554");
        Jedis jedis = new Jedis(shardInfo);

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

        // multi redis transaction command
        // redis transaction dont have isolation and atomocity
        // if watch key change , operation in transaction for that key will failed
        // unlike SQL transaction, either all success or all didn't success
        // redis only that operation not success, rest of operation will continue
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

        // but there is also 库存遗留 problem, because of watch
        // when the thread tried to update , the key version is already different, so it cannot perform update
        // causing some thread unable to get and cannot sell some stock
        // here we can use lua script to solve oversold and this 库存遗留 problem
        System.out.println("秒杀成功");
        jedis.close();
        return true;
    }

    public static void main(String[] args)
    {
        getConnection();

        JedisShardInfo shardInfo = new JedisShardInfo("127.0.0.1",6379);
        shardInfo.setPassword("61376554");
        Jedis jedis = new Jedis(shardInfo);
        String s = jedis.get("Seckill:1010:kc");
        System.out.println(s);

    }
}
