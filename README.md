# 集群会话权限管理<br> <small>*`使用safe-spring-boot-starter保护你的spring-boot应用`*</small>

## 简介

这是一个spring-boot-starter,如果你还不了解spring-boot以及它的starter如何使用，请先学习spring-boot。

该项目提供了一个简单的会话、权限管理功能，目前支持的功能：

- 基于redis分布式会话(需要spring-data-redis支持)
- 基于注解的权限检查(@RequireXXX etc.)

## 使用

- step1：添加依赖
```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <dependency>
        <groupId>cn.ocoop.framework</groupId>
        <artifactId>safe-spring-boot-starter</artifactId>
        <version>1.1.0</version>
    </dependency>
```

- step2：配置filter
```java
    @Bean
    public FilterRegistrationBean safeFilter() {
        FilterRegistrationBean filter = new FilterRegistrationBean<>(new SafeFilter());
        filter.addUrlPatterns("/*");
        return filter;
    }
```

- step3：实现接口AuthorizingService，并交由spring管理
```java
    @Service
    public class AuthenticatingService implements AuthorizingService {
        public List<String> listRole(long accountId){
            //返回accuntId拥有的角色
            return null;
        }
        public List<String> listPermission(long accountId){
            //返回accuntId拥有的权限
            return null;
        }
    }
```

- step4：配置yml
```yml
spring:
  redis:
    url: redis://localhost:6379                 #redis配置
safe:
  session:                                      #会话配置 
    session-id-cookie-name: boss-sessionid      #cookie中存储会话id的name
    session-key-prefix: "boss:session:"         #redis中存储会话id的key前缀
    session-map-key-prefix: "boss:session_map:" #redis中存储accountId和会话id映射关系的key前缀
    permission-key: "boss:pms"                  #redis中存储权限信息的key前缀
  captcha:                                      #图形验证码配置
    width: 100                                  #宽度（px）
    height: 40                                  #高度（px）
    length: 4                                   #验证码位数
```

- step5：编写登录代码
```java
public void login(HttpServletResponse response, String username, String password) throws UnknownAccountException, AccountLockedException, IncorrectCredentialsException {
    User user = getUser(username);
    HashCode inputPasswordEncrypt = Md5.encrypt(password, user.getId());
    if (HashCode.fromString(user.getPassword()).equals(inputPasswordEncrypt)) {
        BoundHashOperations<String, String, String> hash = SessionManager.createAuthenticatedSession(response, user.getId());
        hash.put("username", user.getUsername());
        hash.put("state", user.getState());
        return;
    }

    throw new IncorrectCredentialsException("密码错误");
}
```

- step6：使用注解
```java
     @RestController
     @RequiresPermissions("权限编码")//需要权限编码才能访问
     //@RequiresPermissions(value = {"权限编码","权限编码1"},logical=Logical.AND)//需要多个权限编码才能访问
     //@RequiresPermissions(value = {"权限编码","权限编码1"},logical=Logical.OR)//需要任意权限编码才能访问
     //@RequiresRoles(value = {"角色编码","角色编码1"},logical=Logical.AND)//需要多个角色才能访问
     //@RequiresRoles(value = {"角色编码","角色编码1"},logical=Logical.OR)//需要任意角色才能访问
     //@RequiresCaptcha//需要验证码才能访问,验证码访问地址:/captcha
     //@RequiresAuthentication//需要登录才能访问
     public class DemoController {
     
         @RequiresPermissions("权限编码")//需要权限编码才能访问
         //@RequiresPermissions(value = {"权限编码","权限编码1"},logical=Logical.AND)//需要多个权限编码才能访问
         //@RequiresPermissions(value = {"权限编码","权限编码1"},logical=Logical.OR)//需要任意权限编码才能访问
         //@RequiresRoles(value = {"角色编码","角色编码1"},logical=Logical.AND)//需要多个角色才能访问
         //@RequiresRoles(value = {"角色编码","角色编码1"},logical=Logical.OR)//需要任意角色才能访问
         //@RequiresCaptcha//需要验证码才能访问,验证码访问地址:/captcha
         //@RequiresAuthentication//需要登录才能访问
         @RequestMapping("/")
         public void demo() {
             
         }
     }

```

## 说明

- 状态码

首先要确定的是，目前该项目旨在为完全的前后端分离做权限校验，如果你的项目还没有这样做，那这个项目可能并不适合你。

项目使用HTTP状态码作为处理权限异常的方式，具体的对应关系如下：


<table>
    <thead>
        <tr><th>状态码</th><th>含义</th></tr>
    </thead>   
    <tbody>
        <tr><td>401</td><td>未登录</td></tr>
        <tr><td>444</td><td>无权限</td></tr>
        <tr><td>455</td><td>验证码错误</td></tr>
    </tbody> 
</table>

- 内置endpoint

默认提供了两个endpoint
<table>
    <thead>
        <tr><th>请求地址</th><th>含义</th></tr>
    </thead>   
    <tbody>
        <tr><td>/logout</td><td>退出登录</td></tr>
        <tr><td>/captcha</td><td>获取图形验证码</td></tr>
    </tbody> 
</table>

- 如何校验图形验证码

图形验证码要做到和业务无关，做统一的拦截那么就需要参数不和业务数据耦合，目前使用请求头来携带图形验证码参数，
在发送请求时请将用户输入的图形验证码放置于请求头`X-Captcha`中

- 会话超时

目前尚未提供超时配置，默认给定的超时时间为2天










