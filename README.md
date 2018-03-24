# beanmock


mockito extension. it use to mock deep field.

# 使用场景
Mockito是java单元测试很常用的一个工具。但是在classA -> classB -> classC 的场景中，假如需要对classA进行单元测试，需要针对ClassC的方法返回不同结果进行验证，而又不希望对classB进行mock。
这个时候一般的解决方案有两种：

1、使用反射的层层调用（但是对于更深层的调用会显得很繁琐，并且需要手工恢复场景，否则会污染其他测试场景，因为spring上下文是全局的，除非使用DirtesContext）

2、改用powerMock


为了更好的接口上述的高频场景，对mockito和spring test进行了扩展，支持注解式的嵌套对象mock

# 使用方式

<pre>
@RunWith(SpringBeanMockTestClassRunner.class)  //  使用SpringJUnit4ClassRunner的扩展Runner
@ContextConfiguration(locations = "classpath:applicationContext.xml")
public class LoginServiceTest {

    @MockBean("userService") //标注当前属性需要输入的目标bean（也就是说userService中有configurationService这个属性，并且是通过spring注入的）
    private ConfigurationService configurationService;

    @Resource private LoginService loginService;

    @Test
    public void test(){
        Mockito.when(configurationService.userCache()).thenReturn(true);  
        User user = loginService.login();
        assertEquals("cache",user.getSource());
    }

    @Test
    public void testFromDb(){
        User user = loginService.login();
        assertEquals("db",user.getSource());
    }

}
</pre>

代码中有完整的例子提供运行。
由于相关代码比较少，所以并没有打包成jar包，而是直接提供源码，使用时直接引用源码

