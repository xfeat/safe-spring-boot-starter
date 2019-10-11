package cn.ocoop.framework.safe;

import cn.ocoop.framework.safe.ann.handler.advice.AuthorizationAttributeSourceAdvisor;
import cn.ocoop.framework.safe.auth.controller.CaptchaController;
import cn.ocoop.framework.safe.auth.service.AuthorizingService;
import cn.ocoop.framework.safe.filter.SafeFilter;
import cn.ocoop.framework.safe.response.FieldFilterAdvice;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@RefreshScope
@Configuration
@ConditionalOnWebApplication
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnBean(AuthorizingService.class)
@EnableConfigurationProperties({SafeProperties.class, CaptchaProperty.class})
public class SessionAutoConfiguration {

    public SessionAutoConfiguration(SafeProperties safeProperties, StringRedisTemplate redisTemplate, AuthorizingService authorizingService) {
        SessionManager.redisTemplate = redisTemplate;
        SessionManager.safeProperties = safeProperties;
        SessionManager.authorizingService = authorizingService;
    }

    @Bean
    @ConditionalOnMissingBean(AuthorizationAttributeSourceAdvisor.class)
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor() {
        return new AuthorizationAttributeSourceAdvisor();
    }

    @Bean
    @ConditionalOnMissingBean(DefaultAdvisorAutoProxyCreator.class)
    @ConditionalOnMissingClass("springfox.documentation.swagger2.annotations.EnableSwagger2")
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator = new DefaultAdvisorAutoProxyCreator();
        defaultAdvisorAutoProxyCreator.setProxyTargetClass(true);
        return defaultAdvisorAutoProxyCreator;
    }

    @Bean
    @ConditionalOnMissingBean(FieldFilterAdvice.class)
    public FieldFilterAdvice commonResponseBodyAdvice() {
        return new FieldFilterAdvice();
    }

    @Bean
    @ConditionalOnMissingBean(value = CaptchaController.class, search = SearchStrategy.CURRENT)
    public CaptchaController captchaController() {
        return new CaptchaController();
    }

    @Bean
    public static FilterRegistrationBean<SafeFilter> safeFilterRegistration() {
        FilterRegistrationBean<SafeFilter> registration = new FilterRegistrationBean<>(new SafeFilter());
        registration.setOrder(Integer.MIN_VALUE + 50);
        return registration;
    }
}
