package biocode.fims.rest;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientProperties;
//import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import java.util.List;


/**
 * @author rongjihuang@gmail.com
 */
public class WebTargetFactoryBean implements FactoryBean<WebTarget>, InitializingBean, DisposableBean, ApplicationContextAware {
    private final static Logger logger = LoggerFactory.getLogger(WebTargetFactoryBean.class);
    private List<String> componentPackages;     // auto register packages
    private List<Class<?>> componentClasses;    // register components

    private ApplicationContext context;
    private JerseyTest jerseyTest;
    private WebTarget target;

    static {
        // bridge java.util.logging to slf4j
        java.util.logging.Logger rootLogger = java.util.logging.LogManager.getLogManager().getLogger("");
        java.util.logging.Handler[] handlers = rootLogger.getHandlers();
        for (java.util.logging.Handler handler : handlers) rootLogger.removeHandler(handler);
//        org.slf4j.bridge.SLF4JBridgeHandler.install();
    }

    @Override
    public WebTarget getObject() throws Exception {
        return target;
    }

    @Override
    public Class<?> getObjectType() {
        return WebTarget.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // idea from http://stackoverflow.com/questions/24509754/force-jersey-to-read-mocks-from-jerseytest
        jerseyTest = new JerseyTest() {
            @Override
            protected Application configure() {
                return getResourceConfig();
            }
        };

        logger.info("setUp jerseyTest instance");
        jerseyTest.setUp();

        // init WebTarget instance
        target = jerseyTest.target();
        target.property(ClientProperties.FOLLOW_REDIRECTS, false);
    }

    @Override
    public void destroy() throws Exception {
        logger.info("destroy jerseyTest instance");
        jerseyTest.tearDown();
    }

    private ResourceConfig getResourceConfig() {
        ResourceConfig rc = new ResourceConfig();

        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        rc.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(request).to(HttpServletRequest.class);
            }
        });

        // inject spring context, avoid JerseyTest create a new one.
        // see org.glassfish.jersey.server.spring.SpringComponentProvider.createSpringContext()
        rc.property("contextConfig", WebTargetFactoryBean.this.context);

        // register components
        if (getComponentClasses() != null) for (Class<?> resource : componentClasses) rc.register(resource);

        // auto register package's components
        if (getComponentPackages() != null) for (String p : componentPackages) rc.packages(p);

        // only log out header, URL
        //rc.register(LoggingFilter.class);// header,URL

        // log out header, URL, body
//        rc.register(new LoggingFilter(java.util.logging.Logger.getLogger(LoggingFilter.class.getName()), true));

        return rc;
    }

    public List<String> getComponentPackages() {
        return componentPackages;
    }

    public void setComponentPackages(List<String> componentPackages) {
        this.componentPackages = componentPackages;
    }

    public List<Class<?>> getComponentClasses() {
        return componentClasses;
    }

    public void setComponentClasses(List<Class<?>> componentClasses) {
        this.componentClasses = componentClasses;
    }

}
