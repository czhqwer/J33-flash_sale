package cn.wolfcode.web.config;

import cn.wolfcode.common.web.interceptor.FeignRequestInterceptor;
import cn.wolfcode.common.web.interceptor.RequireLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public RequireLoginInterceptor requireLoginInterceptor(StringRedisTemplate redisTemplate){
        return new RequireLoginInterceptor(redisTemplate);
    }
    //feign 只需要放入到IOC容器中
    @Bean
    public FeignRequestInterceptor feignRequestInterceptor(){
        return new FeignRequestInterceptor();
    }
    //从ioc中获取拦截器
    @Autowired
    private RequireLoginInterceptor requireLoginInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //springMVC需要这样加
        registry.addInterceptor(requireLoginInterceptor)
                .addPathPatterns("/**");
    }
}
