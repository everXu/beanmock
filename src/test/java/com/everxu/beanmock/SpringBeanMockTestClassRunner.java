/**
 *
 */
package com.everxu.beanmock;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author  ever
 */
public class SpringBeanMockTestClassRunner extends SpringJUnit4ClassRunner {

    /**
     * @param clazz
     * @throws InitializationError
     */
    public SpringBeanMockTestClassRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    protected Object createTest() throws Exception {
        Object test = super.createTest();
        // Note that JUnit4 will call this createTest() multiple times for each
        // test method, so we need to ensure to call "beforeClassSetup" only once.
        MockBeanInjectListener listener = new MockBeanInjectListener(test);
        getTestContextManager().registerTestExecutionListeners(listener);
        return test;
    }

    @Override
    public void run(RunNotifier notifier) {
        super.run(notifier);
    }
}
