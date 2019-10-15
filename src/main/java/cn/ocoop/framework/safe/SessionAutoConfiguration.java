package cn.ocoop.framework.safe;

import cn.ocoop.framework.safe.ann.handler.advice.AuthorizationAttributeSourceAdvisor;
import cn.ocoop.framework.safe.auth.controller.CaptchaController;
import cn.ocoop.framework.safe.auth.service.AuthorizingService;
import cn.ocoop.framework.safe.response.FieldFilterAdvice;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
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
@EnableConfigurationProperties({SafeProperties.class, CaptchaProperties.class})
public class SessionAutoConfiguration {

    public SessionAutoConfiguration(SafeProperties safeProperties, CaptchaProperties captchaProperties, StringRedisTemplate redisTemplate, AuthorizingService authorizingService) {
        SessionManager.redisTemplate = redisTemplate;
        SessionManager.safeProperties = safeProperties;
        SessionManager.captchaProperties = captchaProperties;
        SessionManager.authorizingService = authorizingService;
    }

    @Bean
    @ConditionalOnMissingBean(AuthorizationAttributeSourceAdvisor.class)
    public static AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor() {
        return new AuthorizationAttributeSourceAdvisor();
    }

    @Bean
    public static FilterRegistrationBean<SafeFilter> safeFilterRegistration(SafeFilter safeFilter) {
        FilterRegistrationBean<SafeFilter> registration = new FilterRegistrationBean<>(safeFilter);
        registration.setOrder(SafeFilter.DEFAULT_ORDER);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(DefaultAdvisorAutoProxyCreator.class)
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
    @ConditionalOnMissingBean(SafeFilter.class)
    public SafeFilter safeFilter() {
        return new SafeFilter();
    }
}
