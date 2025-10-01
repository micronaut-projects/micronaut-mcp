Please, write a test to verify introspection for ${className}

The following tests shows how to test if a class is introspected. The following test verifies if the `CreateGame` class is annotated with `@Introspected`.

```java
@Test
void isAnnotatedWithIntrospected() {
    assertDoesNotThrow(() -> BeanIntrospection.getIntrospection(CreateGame.class));
}
```
