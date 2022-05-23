package cn.wolfcode.common.web.interceptor;

import cn.wolfcode.common.constants.CommonConstants;
import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.common.web.anno.RequireLogin;
import cn.wolfcode.redis.CommonRedisKey;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class RequireLoginInterceptor implements HandlerInterceptor {
    private StringRedisTemplate redisTemplate;

    public RequireLoginInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //非option方式的请求进入 排除跨域请求
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            //FEIGN_REQUEST_KEY = false
            String feignRequest = request.getHeader(CommonConstants.FEIGN_REQUEST_KEY);
            if (!StringUtils.isEmpty(feignRequest) //feignRequest不能为空
                    && CommonConstants.FEIGN_REQUEST_FALSE.equals(feignRequest) //请求是从前端传递过来的（false）
                    && handlerMethod.getMethodAnnotation(RequireLogin.class) != null) { //方法上要有RequireLogin注解
                response.setContentType("application/json;charset=utf-8");
                String token = request.getHeader(CommonConstants.TOKEN_NAME); //token != null
                //如果token没有数据，就错误返回
                if (StringUtils.isEmpty(token)) {
                    response.getWriter().write(JSON.toJSONString(Result.error(CommonCodeMsg.TOKEN_INVALID)));
                    return false;
                }
                //依据token，从Redis中获取userInfo对象
                UserInfo userInfo = JSON.parseObject(redisTemplate.opsForValue().get(CommonRedisKey.USER_TOKEN.getRealKey(token)), UserInfo.class);
                //登录时间超过7天，异常返回
                if (userInfo == null) {
                    response.getWriter().write(JSON.toJSONString(Result.error(CommonCodeMsg.TOKEN_INVALID)));
                    return false;
                }
                String ip = request.getHeader(CommonConstants.REAL_IP); //本次请求访问的ip地址
                //登录ip地址与本次访问的ip地址是否相同，如果不同，错误返回
                if (!userInfo.getLoginIp().equals(ip)) {
                    response.getWriter().write(JSON.toJSONString(Result.error(CommonCodeMsg.LOGIN_IP_CHANGE)));
                    return false;
                }
            }
        }
        return true;
    }
}

