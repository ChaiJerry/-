package bundle_system.io;
import redis.clients.jedis.Jedis;

public class RedisUtils {
    public static void test() {
        Jedis jedis = new Jedis("localhost", 6379);
        jedis.auth("000000"); // 替换为你的Redis密码

        jedis.set("key1", "value1");
        String value = jedis.get("key1");
        System.out.println("Value: " + value);

        jedis.close();
    }
}
