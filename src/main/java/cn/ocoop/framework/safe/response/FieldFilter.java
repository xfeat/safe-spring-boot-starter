package cn.ocoop.framework.safe.response;

import java.lang.annotation.*;

/**
 * 对返回值的属性进行过滤，待过滤的值可以是以下类型：
 * <p>{@link java.util.Collection}:</p> 如：a.b.c, b为Collection，则会对b进行遍历，并为每个值设置零值
 * <p>{@link java.util.Map}:</p> 如：a.b.c, b为Map，则会对b的value进行遍历，并为每个值设置零值
 * <p>普通的bean:</p> 如：a.b.c,则会对a对象内嵌套的b对象的属性c设置零值
 */

@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(FieldFilters.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldFilter {

    /**
     * 属性,可以使用.来表示嵌套对象
     *
     */
    String[] value() default {};


    /**
     * 是否需要登录
     *
     */
    boolean requireAuthentication() default false;

    /**
     * 是否需要权限
     *
     */
    String[] requirePermission() default {};

    /**
     * 是否需要角色
     *
     */
    String[] requireRole() default {};
}
