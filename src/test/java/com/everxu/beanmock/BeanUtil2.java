package com.everxu.beanmock;

import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * @author  ever
 *
 */
public class BeanUtil2 {

    public static void setProperty(Object bean,String name ,Object value) throws  Exception{
        String[] propertyNames = name.split("\\.");
        int maxDepth = propertyNames.length;
        int depth = 0;
        Object targetBean = bean;
        for (String property : propertyNames) {
            if(depth==maxDepth-1){
                setFieldValue(targetBean, property, value);
            }else{
                targetBean = getTarget(targetBean);
                targetBean = getFieldValue(property,targetBean);
                depth++;
            }
        }
    }

    private static boolean isMockProxy(Object object){
        return Proxy.isProxyClass(object.getClass()) || ClassUtils.isCglibProxyClass(object.getClass());
    }

    public static Object getTarget( Object bean) throws Exception {
        if (isMockProxy(bean)) {
            if(AopUtils.isAopProxy(bean)) {
                return getAopTarget(bean);
            }else{
                return getDeepObject("CGLIB$CALLBACK_0.mockSettings.spiedInstance",bean);
            }
        }else{
           return bean;
        }
    }

    /**
     * 获取 目标对象
     * @param proxy 代理对象
     * @return
     * @throws Exception
     */
    public static Object getAopTarget(Object proxy) throws Exception {

        if(!AopUtils.isAopProxy(proxy)) {
            return proxy;//不是代理对象
        }

        if(AopUtils.isJdkDynamicProxy(proxy)) {
            return getJdkDynamicProxyTargetObject(proxy);
        } else { //cglib
            return getCglibProxyTargetObject(proxy);
        }
    }

    private static Object getCglibProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);

        Object advisedTarget = null;
        try {
            Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");//只被spring修饰
            advised.setAccessible(true);
            advisedTarget = advised.get(dynamicAdvisedInterceptor);
        } catch (NoSuchFieldException e) {
            advisedTarget = getDeepObject("mockSettings.spiedInstance.CGLIB$CALLBACK_0.advised",dynamicAdvisedInterceptor);  //如果被mockito修饰的话，就需要这样做。
        } catch (SecurityException e) {
            throw e;
        }


        return ((AdvisedSupport)advisedTarget).getTargetSource().getTarget();
    }

    private static Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        AopProxy aopProxy = (AopProxy) h.get(proxy);

        Field advised = aopProxy.getClass().getDeclaredField("advised");
        advised.setAccessible(true);

        return ((AdvisedSupport)advised.get(aopProxy)).getTargetSource().getTarget();
    }

    private static Object getDeepObject(String expression,Object object) throws Exception {
        Object targetObject = getTarget(object);
        if (!expression.contains("\\.")){
            return getFieldValue(expression, targetObject);
        }
        int index = expression.indexOf("\\.");
        String field = expression.substring(0,index);
        String subExpression = expression.substring(index+1);
        Object result = getFieldValue(field,targetObject);
        return getDeepObject(subExpression,result);
    }

    private static void setFieldValue(Object bean, String name , Object value) throws Exception{
        Field field;
        try {
            field = bean.getClass().getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            field = bean.getClass().getSuperclass().getDeclaredField(name);
        }
        field.setAccessible(true);
        field.set(bean, value);
    }

    private static Object getFieldValue(String field , Object object) throws NoSuchFieldException, IllegalAccessException {
        Field f ;
        try {
            f = object.getClass().getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            f = object.getClass().getSuperclass().getDeclaredField(field);
        }
        f.setAccessible(true);
        return f.get(object);
    }

    public static Object getProperty(Object bean,String name) throws  Exception{
        return  getDeepObject(name,bean);
    }

}
