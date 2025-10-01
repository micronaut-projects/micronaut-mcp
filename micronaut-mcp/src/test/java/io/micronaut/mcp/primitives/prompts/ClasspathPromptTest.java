package io.micronaut.mcp.primitives.prompts;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.classpath-prompts.introspection-testing.name", value = "introspection-testing")
@Property(name = "micronaut.mcp.classpath-prompts.introspection-testing.title", value = "Introspection-Testing")
@Property(name = "micronaut.mcp.classpath-prompts.introspection-testing.description", value = "Test whether a class is introspected in a Micronaut application")
@Property(name = "micronaut.mcp.classpath-prompts.introspection-testing.path", value = "prompts/introspection-testing.md")
@Property(name = "micronaut.mcp.classpath-prompts.introspection-testing.arguments[0].name", value = "className")
@Property(name = "micronaut.mcp.classpath-prompts.introspection-testing.arguments[0].description", value = "The class for which you want to test introspection")
@Property(name = "micronaut.mcp.classpath-prompts.static-resources-testing.name", value = "static-resources-testing")
@Property(name = "micronaut.mcp.classpath-prompts.static-resources-testing.title", value = "Static-Resources-Testing")
@Property(name = "micronaut.mcp.classpath-prompts.static-resources-testing.description", value = "Test whether a static resource is publicly accessible at a given path in a Micronaut application")
@Property(name = "micronaut.mcp.classpath-prompts.static-resources-testing.path", value = "prompts/static-resources-testing.md")
@Property(name = "micronaut.mcp.classpath-prompts.static-resources-testing.arguments[0].name", value = "path")
@Property(name = "micronaut.mcp.classpath-prompts.static-resources-testing.arguments[0].description", value = "The static resource path. For example /assets/stylesheets/screen.css")
@MicronautTest(startApplication = false)
class ClasspathPromptTest {

    @Inject
    BeanContext beanContext;

    @Test
    void classpathPrompts() {
        Collection<ClasspathPrompt> prompts = beanContext.getBeansOfType(ClasspathPrompt.class);
        assertEquals(2, prompts.size());
        ClasspathPrompt prompt = prompts.stream().filter(p -> p.getName().equals("introspection-testing")).findFirst().get();
        assertEquals("introspection-testing", prompt.getName());
        assertEquals("introspection-testing", prompt.getNameQualifier());
        assertEquals("Introspection-Testing", prompt.getTitle());
        assertEquals("Test whether a class is introspected in a Micronaut application", prompt.getDescription());
        assertEquals("prompts/introspection-testing.md", prompt.getPath());
        assertEquals(1, prompt.getArguments().size());
        assertEquals("className", prompt.getArguments().get(0).getName());
        assertTrue(prompt.getArguments().get(0).isRequired());
        assertEquals("The class for which you want to test introspection", prompt.getArguments().get(0).getDescription());
    }
}
