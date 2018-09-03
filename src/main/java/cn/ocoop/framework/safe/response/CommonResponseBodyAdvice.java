package cn.ocoop.framework.safe.response;

import cn.ocoop.framework.safe.SessionManager;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@ControllerAdvice
public class CommonResponseBodyAdvice implements ResponseBodyAdvice {
    public static void setDefaultValue(Object o, String[] fieldName) throws InvocationTargetException, IllegalAccessException {
        if (fieldName.length == 1) {
            if (o instanceof Collection) {
                for (Object o1 : ((Collection) o)) {
                    setDefaultValue(o1, fieldName);
                }
            } else if (o instanceof Map) {
                for (Object o1 : ((Map) o).values()) {
                    setDefaultValue(o1, fieldName);
                }
            } else {
                org.apache.commons.beanutils.BeanUtils.setProperty(o, fieldName[0], null);
            }
            return;
        }

        if (o instanceof Collection) {
            for (Object n : ((Collection) o)) {
                String currFileName = fieldName[0];
                setPDDefaultValue(fieldName, n, currFileName);
            }
        } else if (o instanceof Map) {
            setDefaultValue(((Map) o).values(), fieldName);
        } else {
            String currFileName = fieldName[0];
            setPDDefaultValue(fieldName, o, currFileName);
        }
    }

    private static void setPDDefaultValue(String[] fieldName, Object n, String currFileName) {
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(n.getClass(), currFileName);
        if (pd != null) {
            try {
                Object fieldValue = pd.getReadMethod().invoke(n);
                setDefaultValue(fieldValue, ArrayUtils.remove(fieldName, 0));
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("无法获取属性{}的值", fieldName, e);
            }
        }
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Class aClass) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        if (Objects.isNull(o)) return null;

        for (FieldFilter filter : executionFilters(methodParameter)) {
            if (filter.always()) {
                setDefaultPropertyValue(o, filter);
                continue;
            }

            if (lacksAuthentication(filter) || lacksPermission(filter) || lacksRole(filter)) {
                setDefaultPropertyValue(o, filter);
            }
        }

        return o;
    }

    private Set<FieldFilter> executionFilters(MethodParameter methodParameter) {
        Set<FieldFilter> filters = Sets.newHashSet();

        FieldFilter[] classFilters = methodParameter.getDeclaringClass().getAnnotationsByType(FieldFilter.class);
        if (ArrayUtils.isNotEmpty(classFilters)) {
            CollectionUtils.addAll(filters, classFilters);
        }

        Method method = methodParameter.getMethod();
        if (method != null) {
            FieldFilter[] methodFilters = method.getAnnotationsByType(FieldFilter.class);
            if (ArrayUtils.isNotEmpty(methodFilters)) {
                CollectionUtils.addAll(filters, methodFilters);
            }
        }
        return filters;
    }

    private void setDefaultPropertyValue(Object o, FieldFilter filter) {
        try {

            for (String field : filter.value()) {
                setDefaultValue(o, field.split("\\."));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("无法清空{}的值", filter.value(), e);
        }
    }

    private boolean lacksAuthentication(FieldFilter fieldFilter) {
        return fieldFilter.requireAuthentication() && !SessionManager.isLogin();
    }

    private boolean lacksPermission(FieldFilter fieldFilter) {
        return ArrayUtils.isNotEmpty(fieldFilter.requirePermission()) && !SessionManager.hasPermission(fieldFilter.requirePermission());
    }

    private boolean lacksRole(FieldFilter fieldFilter) {
        return ArrayUtils.isNotEmpty(fieldFilter.requireRole()) && !SessionManager.hasRole(fieldFilter.requireRole());
    }
}
