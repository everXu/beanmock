package com.everxu.beanmock;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 *  实现MockBean注入的功能
 *
 * @author ever.xu
 */
public class MockBeanInjectListener implements TestExecutionListener, Ordered {

    private Object testObject ;

    public MockBeanInjectListener(Object testObject){
        this.testObject = testObject;
    }

    private Map<String, List<Object>> mockServiceMap = new HashMap<>();

    private void injectMock(ApplicationContext applicationContext) throws Exception {
        Field[] fields = testObject.getClass().getDeclaredFields();
        for (Field field:fields) {
            MockBean mockBean = field.getAnnotation(MockBean.class);
            if (mockBean==null) {
                continue;
            }
            final String mockBeanInjectBeanName = getMockFieldBeanName(applicationContext,mockBean,field);
            final String[] dependentBeanNames = ((GenericApplicationContext)applicationContext).getBeanFactory().getDependentBeans(mockBeanInjectBeanName); //  获取所有在对象内使用该bean name注入属性的实现类的名称
            String targetBeanName = getTargetBeanName(mockBean, dependentBeanNames);
            if (targetBeanName==null) {
                throw new IllegalArgumentException(String.format("当前spring上下文找不到注入了属性 %s 的bean",mockBeanInjectBeanName));
            }

            Object originBeanValue = applicationContext.getBean(mockBeanInjectBeanName); // 原始的对象
            instanceMockObject(field, originBeanValue);

            for (String serviceName : dependentBeanNames) {
                if (!isMatch(serviceName,targetBeanName)) {
                    continue;
                }
                Object targetMockService = applicationContext.getBean(serviceName);
                String injectFiledName = field.getName();//  需要被注入的对象在目标中的属性名称
                if (StringUtils.isNotBlank(mockBean.fieldName())) {
                    injectFiledName = mockBean.fieldName();
                }

                Field mockField = getMockField(targetMockService.getClass(),field,targetBeanName,injectFiledName);
                if (mockField==null) {
                    mockField = getMockField(targetMockService.getClass().getSuperclass(), field, targetBeanName, injectFiledName);
                }
                if (mockBean==null) {
                    String errMsg = "在目标对象[%s]中找不到名称为[%s]的属性,请检查";
                    throw new IllegalArgumentException(String.format(errMsg, targetBeanName, injectFiledName));
                }
                injectFiledName = mockField.getName();
                BeanUtil2.setProperty(targetMockService,injectFiledName, field.get(testObject));
                mockServiceMap.put(serviceName, Arrays.asList(injectFiledName, originBeanValue));
            }
        }
    }

    private Field getMockField( Class clz,Field field,String targetBeanName, String injectFiledName){
        Field mockField = null;
        int typeMatchCount = 0;
        Field[] clzField = clz.getDeclaredFields();
        for (Field f: clzField) {
            if (f.getName().equalsIgnoreCase(injectFiledName)) {
                mockField = f;
                typeMatchCount = 0;
                break;
            }
            if (field.getType().isAssignableFrom(f.getType())) {
                mockField = f;
                typeMatchCount++;
            }
        }
        if (typeMatchCount>1) {
            String errMsg = "在目标对象 %s 中找不到名称为%s 的属性，并且类型为 %s 的属性超过1个，无法通过类型自动检测，请使用MockBean的fieldName属性指定需要mock的具体属性";
            throw new IllegalArgumentException(String.format(errMsg, targetBeanName, injectFiledName, field.getType()));
        }
        return mockField;
    }



    /**
     *  TODO 后续可以支持通配符的方式匹配
     * @param serviceName
     * @param targetBeanName
     * @return
     */
    private boolean isMatch(String serviceName,  String targetBeanName){
        if (serviceName.equalsIgnoreCase(targetBeanName)) {
            return true;
        }
        // 假如targetBeanName = abcService , 而serviceName=abcServiceImpl的场景，兼容这种场景
        String lowerCase = serviceName.toLowerCase().replace("impl","");
        return lowerCase.equalsIgnoreCase(targetBeanName);
    }

    /**
     *  获取需要进行mock的目标对象在spring中的beanName。
     *
     *      当整个spring中，只有一个bean注入了当前的mock对象，可以不注定目标对象的名称，自动检测。否则必须制定所希望mock对象的目标bean。
     *
     * @param mockBean
     * @param dependentBeanNames
     * @return
     */
    private String getTargetBeanName(MockBean mockBean, String[] dependentBeanNames) {
        if (dependentBeanNames.length==0) {
           return null;
        }
        String targetBeanName = mockBean.value();
        if (StringUtils.isBlank(targetBeanName)) {
            if (dependentBeanNames.length>1) {
                throw new UnsupportedOperationException("有多个bean注入了当前属性，必须指定所要mock的目标对象[注明MockBean的value属性]" +
                        " dependentBeanNames="+ arrayToString(dependentBeanNames,","));
            }
            targetBeanName = dependentBeanNames[0];
        }
        return targetBeanName;
    }

    private String arrayToString(String[] dependentBeanNames,String separator){
        StringBuilder sb = new StringBuilder();
        for (String s :dependentBeanNames){
            sb.append(s).append(separator);
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /**
     *   实例化测试类中的改mock对象
     * @param field
     * @param originBeanValue
     * @throws IllegalAccessException
     */
    private void instanceMockObject(Field field, Object originBeanValue ) throws IllegalAccessException {
        MockBean mockBean = field.getAnnotation(MockBean.class);
        field.setAccessible(true);
        if (mockBean.spy()) {
            Object spyObj = spy(originBeanValue);
            field.set(testObject,spyObj);
        }else{
            Object mockObj = mock(field.getType());
            field.set(testObject,mockObj);
        }
    }

    /**
     *  获取需要mock属性在spring中的beanName。策略如下：
     *      1、使用MockBean注解的beanName
     *      2、field的name
     *      3、1和2都找不到的情况下，假如在spring中只有一个实现类那么就去这个实现类的beanName
     *      4、都找不到则抛出异常
     * @param applicationContext
     * @param mockBean
     * @param field
     * @return
     */
    private String getMockFieldBeanName(ApplicationContext applicationContext, MockBean mockBean , Field field) {
        String targetBeanName = field.getName(); // mock的对象在目前类中所注入的bean name
        if (StringUtils.isNotBlank(mockBean.beanName())) {
            targetBeanName = mockBean.beanName();
        }
        String[] beanNames = applicationContext.getBeanNamesForType(field.getType());
        String result = null;
        StringBuilder beanNameStr = new StringBuilder();
        for (String name : beanNames) {
            beanNameStr.append(name).append(",");
            if (name.equalsIgnoreCase(targetBeanName)) {
                result = name;
                break;
            }
        }
        if (StringUtils.isBlank(result)) {
            if (beanNames.length > 1) {
                throw new IllegalArgumentException("cannot determine bean name, request beanName = " + targetBeanName + " and exist beanNames = " + beanNameStr.toString());
            }else {
                result = beanNames[0];
                System.out.println("cannot find match beanName[" + targetBeanName + "] use name[ " + result + "] by type ");
            }
        }
        return result;
    }

    private void resume(ApplicationContext applicationContext){
        for (Map.Entry<String, List<Object>> entry : mockServiceMap.entrySet()) {
            Object targetService = applicationContext.getBean(entry.getKey());
            try {
                BeanUtil2.setProperty(targetService,entry.getValue().get(0).toString(),entry.getValue().get(1));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {}

    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception { }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        ApplicationContext applicationContext = testContext.getApplicationContext();
        injectMock(applicationContext);
    }

    /**
     * The default implementation is <em>empty</em>. Can be overridden by
     * subclasses as necessary.
     */
    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        ApplicationContext applicationContext = testContext.getApplicationContext();
        resume(applicationContext);
    }

    @Override
    public void afterTestClass(TestContext testContext) throws Exception { }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }


    private void printLog(String msg , Object ...args){

    }
}
