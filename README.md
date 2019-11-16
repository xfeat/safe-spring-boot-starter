# 集群会话权限管理<br>

## 简介

这是一个spring-boot-starter,如果你还不了解spring-boot以及它的starter如何使用，请先学习spring-boot。

该项目提供了一个简单的会话、权限管理功能，目前支持的功能：

- 基于redis分布式会话(需要spring-data-redis支持)
- 基于注解的权限检查(@RequireXXX etc.)

## 使用

1. 添加依赖
    ```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        
        <dependency>
            <groupId>cn.ocoop.framework</groupId>
            <artifactId>safe-spring-boot-starter</artifactId>
            <version>1.2.6</version>
        </dependency>
    ```

2. 实现接口AuthorizingService，并交由spring管理
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

3. 配置yml
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
        timeout: 1800                               #会话超时时间 
      captcha:                                      #验证码配置
        enabled: true                               #是否开启验证码校验  
        type: com.wf.captcha.ArithmeticCaptcha      #验证码样式
        length: 2                                   #验证码长度   
    ```

4. 编写登录代码
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

5. 使用注解
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
             public void demo() {}
         }
    ```

## 状态码

首先要确定的是，目前该项目旨在为完全的前后端分离做权限校验，如果你的项目还没有这样做，那这个项目可能并不适合你。

项目使用HTTP状态码作为处理权限异常的方式，具体的对应关系如下：

|状态码|含义|
|---|---|
|401|未登录|
|444|无权限|
|455|验证码错误|
|466|会话状态不正确|

## 验证码

### 验证码获取
    
默认提供了获取验证码的Controller

|含义|请求地址|
|---|---|
|获取图形验证码(base64格式)|/secure/captcha-stream|
|获取图形验证码(stream格式)|/secure/captcha-stream|

### 验证码校验

图形验证码要做到和业务无关，做统一的拦截那么就需要参数不和业务数据耦合，目前使用请求头来携带图形验证码参数，
在发送请求时请将用户输入的图形验证码放置于请求头`X-Captcha`中

## 异常处理
提供了[ExceptionAdviceHandler](src/main/java/cn/ocoop/framework/safe/ex/ExceptionAdviceHandler.java),
你的异常可以继承该类来扩展自己的异常处理

```java
@RestControllerAdvice
public class ControllerAdviceExceptionHandler extends ExceptionAdviceHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> example(WebRequest request, Exception ex) {
        return handleExceptionInternal(ex, errorWrap(HttpStatus.BAD_REQUEST.value(), "请求参数不正确", ex.getLocalizedMessage()), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler
    public ResponseEntity<Object> example1(WebRequest request, Exception ex) {
        return handleExceptionInternal(ex, errorWrap(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统异常", ex.getLocalizedMessage()), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }
}
```

## 对Controller返回值的字段进行过滤

很多时候，我们会写公共的service去完成相同逻辑，但不同的接口可能需要返回不同的字段值，例如：拥有`查看工资`权限的人可以查看员工的工资属性，
那么这时候对于这个不包含`工资`属性返回值就需要构建不同service方法或者在controller重写该字段的值。

通常我们会通过两种方式来覆盖该属性：

    1.直接删除该字段
    2.覆盖该字段的值
    
 对于第一种情况你可能会想到类似于GraphQL的实现，可以根据调用方来决定响应字段。然而，对于web开发而言，我们很多时候更倾向于返回一个包含默认值的字段。
 
为此，提供了[@FieldFilter](https://github.com/xfeat/safe-spring-boot-starter/blob/master/src/main/java/cn/ocoop/framework/safe/response/FieldFilter.java)注解,使用该注解可以轻松的为不想返回真实值的字段设置零值。

用法：
```java
@Data
class Paging<T> {
     private Collection<T> data;
}

@Data
class Job {
    private long id;
    private String title;
    private String name;
    private boolean shelfState;
}

@RestController
@RequestMapping("/jobs")
class TestController {
      @Autowired
      private JobService jobService;
     
      @FieldFilter({"data.title","data.firstName"})
      @FieldFilter({"data.id","data.shelfState"})
      @RequestMapping("/list/{pageNum}/{pageSize}")
      public Paging<Job> list(  @PathVariable int pageNum, @PathVariable int pageSize ) {
          PageHelper.startPage(pageNum, pageSize);
          return Paging.build(pageNum, pageSize, jobService.list());
      }
      
      
      @FieldFilter(value="title", requireAuthentication = true)
      @RequestMapping("/get/{id}")
      public Job list( @PathVariable long id) {
          return jobService.get(id);
      }
}

```   

更多用法请参看该注解注释








