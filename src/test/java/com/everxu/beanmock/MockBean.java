package com.everxu.beanmock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ever.xu
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface MockBean {

    /**
     * 需要替换为mock对象 的目标实现类。需要进行mock的目标对象在spring中的beanName。
     *
     * example:
     *
     *      假如标注了MockBean注解的属性是Configer类， value的名字是：abcService
     *      那么所表达的含义为：
     *          希望对beanName=abcService 的这个bean中的Configer属性，替换为一个mock对象，用于进行测试。
     *
     *  当整个spring中，只有一个bean注入了当前的mock对象，可以不注定目标对象的名称，自动检测。否则必须制定所希望mock对象的目标bean。
     *
     */
    String value() default "" ;

    /** 属性在spring的beanName*/
    String beanName() default  "";

    /** 被注入的实体在 目标 类中的属性名称。默认就是变量名称*/
    String fieldName() default "";

    boolean spy() default false;

    /** mock的属性是使用type注入的，假如设置为true 则忽略beanName*/
    boolean injectByType() default  false;

}
