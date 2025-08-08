package example.micronaut;

import io.micronaut.context.BeanContext;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.mcp.resources.Resource;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
class ResourcesTest {

    @Inject
    BeanContext beanContext;
    @Test
    void loadResources() {
        Collection<Resource> resources = beanContext.getBeansOfType(Resource.class);
        assertTrue(CollectionUtils.isNotEmpty(resources));
        resources.forEach(resource -> {
            System.out.println("----");
            System.out.println(resource.title());
            System.out.println(resource.description());
        });
    }
}
