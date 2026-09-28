package com.qiangpiao.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * MyBatis 慢 SQL 拦截器。
 * <p>
 * 拦截 Executor 的 query / update，统计真实执行耗时，超过阈值（默认 100ms，
 * 由 {@code sql.slow-threshold-ms} 配置）时输出到 logger {@code SLOW_SQL}，
 * 由 logback 单独落到 {@code D:/rizi1/qiangpiao/SQL/slow-sql-yyyy-MM-dd.log}。
 * <p>
 * 只做「计时 + 记录」，不改变任何执行结果，异常照常向上抛出。
 */
@Slf4j(topic = "SLOW_SQL")
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class SlowSqlInterceptor implements Interceptor {

    /** 慢 SQL 阈值（毫秒），默认 100ms */
    private long thresholdMs;

    /** 单条 SQL 最长打印长度，避免超长 SQL 撑爆日志 */
    private static final int MAX_SQL_LENGTH = 4000;

    public SlowSqlInterceptor() {
        this(100L);
    }

    public SlowSqlInterceptor(long thresholdMs) {
        this.thresholdMs = thresholdMs > 0 ? thresholdMs : 100L;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (cost > thresholdMs) {
                logSlowSql(invocation, cost);
            }
        }
    }

    private void logSlowSql(Invocation invocation, long cost) {
        try {
            Object[] args = invocation.getArgs();
            MappedStatement ms = (MappedStatement) args[0];
            Object parameter = args.length > 1 ? args[1] : null;
            BoundSql boundSql = ms.getBoundSql(parameter);
            String sql = formatSql(ms.getConfiguration(), boundSql);
            if (sql.length() > MAX_SQL_LENGTH) {
                sql = sql.substring(0, MAX_SQL_LENGTH) + "...(truncated)";
            }
            log.warn("慢SQL cost={}ms, threshold={}ms, statement={}, sql={}",
                    cost, thresholdMs, ms.getId(), sql);
        } catch (Exception e) {
            // 日志不能影响业务：格式化失败时退化为只记录耗时
            log.warn("慢SQL cost={}ms（SQL 格式化失败：{}）", cost, e.getMessage());
        }
    }

    /**
     * 把 ? 占位符替换为真实参数值，便于直接复现慢 SQL。
     */
    private String formatSql(Configuration configuration, BoundSql boundSql) {
        String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (parameterObject == null || parameterMappings == null || parameterMappings.isEmpty()) {
            return sql;
        }
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
            return sql.replaceFirst("\\?", quote(parameterObject));
        }
        MetaObject metaObject = configuration.newMetaObject(parameterObject);
        for (ParameterMapping mapping : parameterMappings) {
            String property = mapping.getProperty();
            if (metaObject.hasGetter(property)) {
                Object value = metaObject.getValue(property);
                sql = sql.replaceFirst("\\?", quote(value));
            } else if (boundSql.hasAdditionalParameter(property)) {
                sql = sql.replaceFirst("\\?", quote(boundSql.getAdditionalParameter(property)));
            }
        }
        return sql;
    }

    private String quote(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String || value instanceof Date) {
            if (value instanceof Date) {
                return "'" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) value) + "'";
            }
            return "'" + value + "'";
        }
        return String.valueOf(value);
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        String value = properties.getProperty("thresholdMs");
        if (value != null) {
            this.thresholdMs = Long.parseLong(value);
        }
    }
}
