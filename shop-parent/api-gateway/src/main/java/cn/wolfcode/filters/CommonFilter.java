package cn.wolfcode.filters;

import cn.wolfcode.common.constants.CommonConstants;
import cn.wolfcode.redis.CommonRedisKey;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 定义全局过滤器，功能如下:
 * 1.把客户端真实IP通过请求同的方式传递给微服务
 * 2.在请求头中添加FEIGN_REQUEST的请求头，值为0，标记请求不是Feign调用，而是客户端调用
 * 3.刷新Token的有效时间
 */
@Component
public class CommonFilter implements GlobalFilter {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        /**
         * pre拦截逻辑
         * 在请求去到微服务之前，做了两个处理
         * 1.把客户端真实IP通过请求同的方式传递给微服务
         * 2.在请求头中添加FEIGN_REQUEST的请求头，值为0，标记请求不是Feign调用，而是客户端调用
         */
        ServerHttpRequest request = exchange.getRequest().mutate().
                //将前端的ip地址放在 X-REAL-IP=192.168.33.1
                        header(CommonConstants.REAL_IP, exchange.getRequest().getRemoteAddress().getHostString()).
                // FEIGN_REQUEST = 0 | false    true表示是微服务之间的调用，false表示不是微服务调用，是前端调用
                        header(CommonConstants.FEIGN_REQUEST_KEY, CommonConstants.FEIGN_REQUEST_FALSE).
                        build();
        return chain.filter(exchange.mutate().request(request).build()).then(Mono.fromRunnable(() -> {
            /**
             * post拦截逻辑
             * 在请求执行完微服务之后,需要刷新token在redis的时间
             * 判断token不为空 && Redis还存在这个token对于的key,这时候需要延长Redis中对应key的有效时间.
             */
            String token, redisKey;
            if (!StringUtils.isEmpty(token = exchange.getRequest().getHeaders().getFirst(CommonConstants.TOKEN_NAME))
                    && redisTemplate.hasKey(redisKey = CommonRedisKey.USER_TOKEN.getRealKey(token))) { //还在登录的30分钟之内
                redisTemplate
                        .expire( //延长key的存活时间 30m
                                redisKey,
                                CommonRedisKey.USER_TOKEN.getExpireTime(),
                                CommonRedisKey.USER_TOKEN.getUnit());
            }
        }));
    }
}
