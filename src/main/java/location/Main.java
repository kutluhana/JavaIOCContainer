package location;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    static void main() throws Exception {
        IOCContainer iocContainer = new IOCContainer();

        OrderService orderService = iocContainer.getBean(OrderService.class);
        OrderService orderService2 = iocContainer.getBean(OrderService.class);
        OrderService orderService3 = iocContainer.getBean(OrderService.class);
        OrderService orderService4 = iocContainer.getBean(OrderService.class);

    }
}

class IOCContainer {
    private static final Path ROOT_PATH = Paths.get("/Users/kutluhanpalalioglu/Desktop/JavaIOCContainer/target/classes").toAbsolutePath();
    private static final String SINGLETON = "singleton";
    private final Map<Class<?>, Object> beans = new HashMap<>();

    private final Set<Class<?>> beanCandidates = ConcurrentHashMap.newKeySet();
    private final Map<Class<?>, String> scopeNames = new ConcurrentHashMap<>();

    public IOCContainer() throws Exception {
        fillBeanCandidates();
        for(Class<?> beanCandidate : beanCandidates) {
            if(!beans.containsKey(beanCandidate) && SINGLETON.equals(scopeNames.get(beanCandidate))) {
                initializeBean(beanCandidate);
            }
        }
        System.out.println("All the singleton beans are created!");
    }

    public void fillBeanCandidates() {
        try (Stream<Path> paths = Files.walk(ROOT_PATH)) {
            Set<Class<?>> setOfBeanCandidates = findBeanCandidates(paths);

            beanCandidates.addAll(setOfBeanCandidates);
            setOfBeanCandidates.forEach(item -> scopeNames.put(item, item.getAnnotation(AndThisIsTheScope.class).value().name()));

        } catch (IOException exception) {
            System.out.println("Couldn't walk and fell...");
        }
    }

    private Class<?> loadClass(String name, ClassLoader classLoader) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException e) {
            System.out.println("Where are my beans!!! " + name);
            return null;
        }
    }

    private Set<Class<?>> findBeanCandidates(Stream<Path> paths) {
        ClassLoader classLoader = Main.class.getClassLoader();

        return paths.parallel()
                .filter(p -> p.toString().endsWith(".class"))
                .map(path -> ROOT_PATH.relativize(path).toString()
                        .replace(File.separatorChar, '.')
                        .replaceAll("\\.class$", ""))
                .map(name -> loadClass(name, classLoader))
                .filter(Objects::nonNull)
                .filter(c ->
                        c.isAnnotationPresent(IGuessThisIsABean.class) && c.isAnnotationPresent(AndThisIsTheScope.class))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    public <T> T initializeBean(Class<?> beanCandidate) throws Exception {
        Constructor<?>[] constructors = beanCandidate.getConstructors();
        Constructor<?> constructor = constructors[0];

        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];

        for(int i = 0; i < parameterTypes.length; i++) {
            parameters[i] = getBean(parameterTypes[i]);
        }

        T instance = (T) constructor.newInstance(parameters);

        if(SINGLETON.equals(scopeNames.get(beanCandidate))) {
            beans.put(beanCandidate, instance);
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) throws Exception {
        if(beans.containsKey(type)) {
            return type.cast(beans.get(type));
        } else {
            return initializeBean(type);
        }
    }
}

@IGuessThisIsABean
@AndThisIsTheScope(TheScopes.SINGLETON)
class PaymentService {

    public PaymentService() {
        System.out.println("PaymentService is created");
    }

    public void pay() {
        System.out.println("Paid!");
    }
}

@IGuessThisIsABean
@AndThisIsTheScope(TheScopes.PROTOTYPE)
class OrderService {
    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
        System.out.println("OrderService is created. And it is definitely a prototype!");
    }
    public void checkout() {
        paymentService.pay();
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface IGuessThisIsABean {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface AndThisIsTheScope {
    TheScopes value();
}

enum TheScopes {
    SINGLETON, PROTOTYPE
}
