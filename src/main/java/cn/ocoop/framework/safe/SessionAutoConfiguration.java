package cn.ocoop.framework.safe;

import cn.ocoop.framework.safe.ann.handler.advice.AuthorizationAttributeSourceAdvisor;
import cn.ocoop.framework.safe.auth.controller.CaptchaController;
import cn.ocoop.framework.safe.auth.controller.LogoutController;
import cn.ocoop.framework.safe.auth.service.AuthorizingService;
import cn.ocoop.framework.safe.ex.ExceptionAdviceHandler;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnWebApplication
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnBean(AuthorizingService.class)
@EnableConfigurationProperties(SafeProperties.class)
public class SessionAutoConfiguration {

    public SessionAutoConfiguration(SafeProperties safeProperties, StringRedisTemplate redisTemplate, AuthorizingService authorizingService) {
        SessionManager.redisTemplate = redisTemplate;
        SessionManager.safeProperties = safeProperties;
        SessionManager.authorizingService = authorizingService;
    }

    @Bean
    @ConditionalOnMissingBean(value = CaptchaController.class, search = SearchStrategy.CURRENT)
    public CaptchaController captchaController() {
        return new CaptchaController();
    }

    @Bean
    @ConditionalOnMissingBean(value = LogoutController.class, search = SearchStrategy.CURRENT)
    public LogoutController logoutController() {
        return new LogoutController();
    }

    @Bean
    @ConditionalOnMissingBean(AuthorizationAttributeSourceAdvisor.class)
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor() {
        return new AuthorizationAttributeSourceAdvisor();
    }

    //@Bean
    //@ConditionalOnMissingBean(DefaultAdvisorAutoProxyCreator.class)
    //public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
    //    DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator = new DefaultAdvisorAutoProxyCreator();
    //    defaultAdvisorAutoProxyCreator.setProxyTargetClass(true);
    //    return defaultAdvisorAutoProxyCreator;
    //}

    @Bean
    @ConditionalOnMissingBean(ExceptionAdviceHandler.class)
    public ExceptionAdviceHandler exceptionAdviceHandler() {
        return new ExceptionAdviceHandler();
    }

}
