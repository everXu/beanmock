package com.everxu.beanmock.test;

import com.everxu.beanmock.MockBean;
import com.everxu.beanmock.SpringBeanMockTestClassRunner;
import com.everxu.beanmock.demo.ConfigurationService;
import com.everxu.beanmock.demo.LoginService;
import com.everxu.beanmock.demo.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;

@RunWith(SpringBeanMockTestClassRunner.class)  //  使用SpringJUnit4ClassRunner的扩展Runner
@ContextConfiguration(locations = "classpath:applicationContext.xml")
public class LoginServiceTest2 {

    //标注当前属性需要输入的目标bean（也就是说userService中有configurationService这个属性，并且是通过spring注入的）
    @MockBean
    private ConfigurationService config;

    @Resource private LoginService loginService;

    @Test
    public void test(){
        Mockito.when(config.userCache()).thenReturn(true);
        User user = loginService.login();
        assertEquals("cache",user.getSource());
    }

    @Test
    public void testFromDb(){
        User user = loginService.login();
        assertEquals("db",user.getSource());
    }

}
