package com.qiangpiao.config;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.wall.WallFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * Druid 数据库连接池配置。
 */
@Slf4j
@Configuration
public class DataSourceConfig {

    @Value("${jdbc.driverClassName}")
    private String driverClassName;
    @Value("${jdbc.url}")
    private String url;
    @Value("${jdbc.username}")
    private String username;
    @Value("${jdbc.password}")
    private String password;
    @Value("${jdbc.initialSize}")
    private int initialSize;
    @Value("${jdbc.minIdle}")
    private int minIdle;
    @Value("${jdbc.maxActive}")
    private int maxActive;
    @Value("${jdbc.maxWait}")
    private long maxWait;
    @Value("${jdbc.validationQuery}")
    private String validationQuery;
    @Value("${jdbc.testWhileIdle}")
    private boolean testWhileIdle;
    @Value("${jdbc.testOnBorrow}")
    private boolean testOnBorrow;
    @Value("${jdbc.testOnReturn}")
    private boolean testOnReturn;
    /** 慢 SQL 阈值（毫秒），与 MyBatis SlowSqlInterceptor 保持一致 */
    @Value("${sql.slow-threshold-ms:100}")
    private long slowSqlMillis;

    /**
     * 外部化配置：环境变量 &gt; JVM 系统属性 &gt; config.properties。
     * 支持 QP_DB_URL / QP_DB_USERNAME / QP_DB_PASSWORD 环境变量，
     * 或 -Djdbc.url=... -Djdbc.username=... -Djdbc.password=... 启动参数。
     */
    private String resolve(String envKey, String propKey, String defaultValue) {
        String env = System.getenv(envKey);
        if (StringUtils.isNotBlank(env)) {
            return env;
        }
        String prop = System.getProperty(propKey);
        if (StringUtils.isNotBlank(prop)) {
            return prop;
        }
        return defaultValue;
    }

    @Bean(name = "dataSource", initMethod = "init", destroyMethod = "close")
    public DataSource dataSource() {
        String finalUrl = resolve("QP_DB_URL", "jdbc.url", url);
        String finalUsername = resolve("QP_DB_USERNAME", "jdbc.username", username);
        String finalPassword = resolve("QP_DB_PASSWORD", "jdbc.password", password);

        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setUrl(finalUrl);
        ds.setUsername(finalUsername);
        ds.setPassword(finalPassword);
        ds.setInitialSize(initialSize);
        ds.setMinIdle(minIdle);
        ds.setMaxActive(maxActive);
        ds.setMaxWait(maxWait);
        ds.setValidationQuery(validationQuery);
        ds.setTestWhileIdle(testWhileIdle);
        ds.setTestOnBorrow(testOnBorrow);
        ds.setTestOnReturn(testOnReturn);
        ds.setPoolPreparedStatements(true);
        ds.setMaxPoolPreparedStatementPerConnectionSize(20);
        ds.setTimeBetweenEvictionRunsMillis(60000);
        ds.setMinEvictableIdleTimeMillis(300000);
        ds.setRemoveAbandoned(true);
        ds.setRemoveAbandonedTimeout(180);
        ds.setProxyFilters(Arrays.asList(statFilter(), wallFilter()));
        log.info("Druid 数据源初始化：url={}, username={}", finalUrl, finalUsername);
        return ds;
    }

    @Bean
    public StatFilter statFilter() {
        StatFilter statFilter = new StatFilter();
        statFilter.setSlowSqlMillis(slowSqlMillis);
        statFilter.setLogSlowSql(true);
        statFilter.setMergeSql(true);
        return statFilter;
    }

    @Bean
    public WallFilter wallFilter() {
        WallFilter wallFilter = new WallFilter();
        wallFilter.setDbType("mysql");
        return wallFilter;
    }
}
