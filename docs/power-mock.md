# 增强测试: 静态、私有方法处理

mockito 已经很强大，能帮我们完成大部分 mock 工作，但是对于一些特殊方法来说，还是无能为力。

例如，当我们使用系统获取当前时间戳的时候，可能会调用  System.currentTimeMillis()。对于这个方法来说，我们无法 mock。往往就会遇到一个有趣的现象，一些测试过了一段时间就通不过了，项目中可能有对时间进行检查的逻辑。

我们有一个项目是做财务报销单，当费用产生后的几个月后报销就会失败，我们的模拟数据是一个固定的时间，因此几个月后重新运行这个项目的单元测试就不通过了。

另外，项目中不可避免的需要 mock 系统静态方法、私有方法；以及对一些私有方法进行测试，虽然不推荐测试私有方法，但是如果遇到遗留系统，public 方法有时候巨大，测试的成本非常高。

配合 mockito 使用的另外一个框架是 powermock。它采用了字节码技术，可以增强测试，解决 mock 静态、私有方法，以及必要时测试静态、私有方法。

## powermock 入门

为了便于演示，我给上一部分的例子中的 User 对象增加了 createAt 字段，createAt 字段在 register 方法内被填充，然后进行持久化。

更新后的 User 对象如下：

```java
public class User {
    private String email;
    private String username;
    private String password;
    private Instant createAt;

    public User(String email, String username, String password, Instant createAt) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.createAt = createAt;
    }

    ...
}
```

并给 user 添加对应的值，Instant.now() 调用的系统方法。

```java
user.setCreateAt(Instant.now());
```

按照我们前面的测试，这样会给测试带来不变，因此需要想办法 mock 掉 Instant.now 这个方法。
```java 
assertEquals("", argument.getValue().getCreateAt());
```

首先，引入 powermock 的相关依赖。powermock 有两个模块，一个是封装 junit、另外一个是封装 mockito，间接的依赖了 junit 和 mockito。因此可以先把原来的测试依赖移除，添加下面的两个依赖即可。

```java
<dependency>
    <groupId>org.powermock</groupId>
    <artifactId>powermock-module-junit4</artifactId>
    <version>2.0.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.powermock</groupId>
    <artifactId>powermock-api-mockito2</artifactId>
    <version>2.0.2</version>
    <scope>test</scope>
</dependency>
```

然后，使用 PowerMockRunner 代替 mockito 的 runner。并使用 @PrepareForTest 对用到该静态方法的地方进行初始化。

```java
@RunWith(PowerMockRunner.class)
@PrepareForTest(UserService.class)
```

在测试过程中，我们就可以 mock Instant.class 中的静态方法，并且会影响 UserService 中使用它的地方。

```java
Instant moment = Instant.ofEpochSecond(1596494464);

PowerMockito.mockStatic(Instant.class);
PowerMockito.when(Instant.now()).thenReturn(moment);
```

Instant.now() 就会按照我们期望的值返回结果，当然也可以按照时间戳 1596494464 来断言了，完整的测试如下：

```java
@RunWith(PowerMockRunner.class)
// 比如使用 PrepareForTest 让 mock 在被测试代码中生效
@PrepareForTest({UserService.class})
public class UserServiceAnnotationTest {

    @Mock
    UserRepository mockedUserRepository;
    @Mock
    EmailService mockedEmailService;

    @Spy
    EncryptionService mockedEncryptionService = new EncryptionService();

    @InjectMocks
    UserService userService;

    @Test
    public void should_register() {
        // mock 前生成一个 Instant 实例
        Instant moment = Instant.ofEpochSecond(1596494464);
				
      	// mock 并设定期望返回值
        PowerMockito.mockStatic(Instant.class);
        PowerMockito.when(Instant.now()).thenReturn(moment);

        // given
        User user = new User("admin@test.com", "admin", "xxx", null);

        // when
        userService.register(user);

        // then
        verify(mockedEmailService).sendEmail(
                eq("admin@test.com"),
                eq("Register Notification"),
                eq("Register Account successful! your username is admin"));

        ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        verify(mockedUserRepository).saveUser(argument.capture());

        assertEquals("admin@test.com", argument.getValue().getEmail());
        assertEquals("admin", argument.getValue().getUsername());
        assertEquals("cd2eb0837c9b4c962c22d2ff8b5441b7b45805887f051d39bf133b583baf6860", argument.getValue().getPassword());
        assertEquals(moment, argument.getValue().getCreateAt());
    }
}
```

## Mock 静态方法

除了入门的基本使用外，powermock 还有一些需要特别注意的点，可以避免在实际项目中碰到的问题。

@PrepareForTest 中的参数，也就是一个需要被准备的类。这个类不是被 Mock 的类本身。例如上面一个例子中，我们需要 Mock 的是 Instant.now()，这个方法在 UserService 中被使用，我们需要 prepare UserService 而不是 Instant。这是 powermock 在使用中最常见的一个错误，就其原因是 静态方法是类级别的方法，需要在类被加载前准备完毕。

具体的实现是在 PowerMockRunner 中完成的，其中用了很多字节码级别的技术，可以不用关心具体实现。


### Verify 静态方法

上面的例子中，我们不需要验证 Instant.now 的调用情况。在有些情况下，对静态方法也需要验证。

可以使用 PowerMockito 的 verifyStatic 方法，然后直接调用被 verify 的静态方法即可。

```
PowerMockito.verifyStatic(Static.class);
Static.thirdStaticMethod(Mockito.anyInt());
```

需要注意的是 verifyStatic 需要每次验证都调用，这两句代码为成对出现的。



### Mock 构造方法

有时候被测试的代码中可能会直接使用 new 关键字创建一个对象，这种情况下就不太好隔离被创建的对象。如果不使用 powermock 甚至这段代码不能被测试，有两个途径来解决。一个是使用使用工厂方法或者依赖注入代替直接使用 new 关键字，进行解耦；另一种方式是，使用 powermock 对构造方法进行 mock。

在 powermock 中使用 whenNew 这个方法，可以拦截构造方法的调用，而直接返回其他对象或者异常。构造方法的 mock 是 powermock 中最常用的特性之一。

```java
@RunWith(PowerMockRunner.class)
@PrepareForTest(X.class)
public class XTest {
        @Test
        public void test() {
                whenNew(MyClass.class).withNoArguments().thenThrow(new IOException("error message"));

                X x = new X();
                x.y(); // y is the method doing "new MyClass()"

                ..
        }
}
```

## Mock 私有方法

和前面的问题类似，在做一些重构时，我们发现类中有一些特别长的私有方法，这些私有方法比较复杂带来的测试成本很高。

一种方式是，重构这些私有方法到另外一个类中，保持类的私有方法处于较少的状态。另外一种是通过 powermock 对私有方法进行 mock。使用 powermock mock 私有方法非常简单，只需要 PowerMockito.when() 方法即可，因为私有方法直接调用会有 java 语法报错，因此when 方法提供了通过方法名 mock 的重载。

假如 doSendEmockedEmailService 对象中有一个私有方法 doSendEmail，下面的示例代码演示了 powermock mock 私有方法的例子。

```java
 PowerMockito.when(mockedEmailService, "doSendEmail").thenReturn(true));
```



## 为私有属性赋值

在使用 Spring 等一些框架时，会使用 @Value  为对象属性注入值，但是往往这个属性是私有的。为了测试方便，mockito 提供了响应的工具。

早期的工具使用了 whitebox 类，提供统一的反射操作方法，因为 whitebox 过于开放，在后期的版本中不推荐使用了。

一般我们在项目中用的比较多的是  FieldSetter.setField 这个 API，可以比较优雅的解决这个问题。

```java
@Value
private String template;
```

```java
FieldSetter.setField(
        mockedEmailService,EmailService.class.getDeclaredField("template"),
        "test template"
);
```



## 测试私有方法

一般来说，单元测试会测试 public 方法，如果被测试代码不合理时，通常的做法是修改被测试代码。让代码具备可测试性非常重要，也能让代码变得更加整洁。

如果我们遇到私有方法，但是想要测试有两个方法。

一种比较好的方法是将私有方法修改为包级别私有，然后将测试代码放到同一个包下，但是处于 test 目录下 （src/main/java 和 src/test/java 的关系），这样测试代码就能访问到该方法。

另外的方法是，使用一些辅助工具，例如 mockito 的 Whitebox 类，可以提供对私有方法、属性的访问。使用 Whitebox 可以在必要时增强测试能力。

```java
Whitebox.invokeMethod(testObj, "method1", new Long(10L));
```

如果使用了 SpringTest 还可以使用 ReflectionTestUtils 类来完成：

```java
ReflectionTestUtils.invokeMethod(student, "saveOrUpdate", "From Unit test");
```

## 小结

实际工作中，被测试代码不一定能非常容易的被 mock 和测试。让代码具有很好的测试性，实际开发过程中非常重要的一件事。

当我们确实需要对私有方法做 mock 和 测试时，可以借助其他方法和工具：

1. 使用 powermock 对私有方法、属性进行 mock 和验证
2. 使用包级别可访问的策略对私有方法进行改造，使其可被测试
3. 使用反射工具，例如 Whitebox、ReflectionTestUtils、FieldSetter 访问私有属性和方法

