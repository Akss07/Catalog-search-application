package com.info7255.advancebigdata.repositorey;

import com.info7255.advancebigdata.service.MedicalPlanService;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.Set;

@Repository
public class MedicalPlanRepository {

    private final Jedis jedis;

    public MedicalPlanRepository(Jedis jedis){
        this.jedis = jedis;
    }

    public void saveToRedis(String key, String value){
        try (Jedis jedis = new Jedis("localhost")) {
            jedis.sadd(key, value);
        }
    }

    public void addSetValue(String key, String value) {
        try (Jedis jedis = new Jedis("localhost")) {
            jedis.sadd(key, value);
        }
    }
    public void hSet(String key, String field, String value ) {
        try (Jedis jedis = new Jedis("localhost")) {
            jedis.hset(key, field, value);
        }
    }
    public Set<String> getKeys(String pattern){
        try (Jedis jedis = new Jedis("localhost")) {
            return jedis.keys(pattern);
        }
    }

    public long deleteKeys(String[] keys) {
        try (Jedis jedis = new Jedis("localhost")) {
            return jedis.del(keys);
        }
    }
    public Map<String,String> getAllValuesByKey(String key) {
        try (Jedis jedis = new Jedis("localhost")) {
            return jedis.hgetAll(key);
        }
    }
    public Set<String> sMembers(String key) {
        try (Jedis jedis = new Jedis("localhost")) {
            return jedis.smembers(key);
        }
    }

    public boolean isKeyPresent(String key){
        Map<String, String> value = jedis.hgetAll(key);
        jedis.close();
        return !(value == null || value.isEmpty());
    }

    public String getEtag(String key){
        return jedis.hget(key, "eTag");
    }
}
