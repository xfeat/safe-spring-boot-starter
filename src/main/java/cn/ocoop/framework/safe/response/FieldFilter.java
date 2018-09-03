package cn.ocoop.framework.safe.response;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(FieldFilters.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldFilter {

    /**
     * 属性
     *
     * @return
     */
    String[] value() default {};

    /**
     * 是否过滤该属性，若该属性为true则始终过滤，否则根据{@link #requireAuthentication()}和{@link #requirePermission()}来判断是否需要过滤
     *
     * @return
     */
    boolean always() default true;

    /**
     * 是否需要登录
     *
     * @return
     */
    boolean requireAuthentication() default false;

    /**
     * 是否需要权限
     *
     * @return
     */
    String[] requirePermission() default {};

    /**
     * 是否需要角色
     *
     * @return
     */
    String[] requireRole() default {};
}
