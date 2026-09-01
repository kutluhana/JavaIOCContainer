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
        new IOCContainer();
    }
}

class IOCContainer {
    private static final Path ROOT_PATH = Paths.get("/Users/kutluhanpalalioglu/Desktop/JavaIOCContainer/target/classes").toAbsolutePath();
    private final Set<Class<?>> beanCandidates = ConcurrentHashMap.newKeySet();
    private final Map<Class<?>, Object> beans = new HashMap<>();

    public IOCContainer() throws Exception {
        fillBeanCandidates();
        for(Class<?> beanCandidate : beanCandidates) {
            if(!beans.containsKey(beanCandidate)) {
                initializeBean(beanCandidate);
            }
        }
    }

    public void fillBeanCandidates() {
        ClassLoader classLoader = Main.class.getClassLoader();

        try (Stream<Path> paths = Files.walk(ROOT_PATH)) {
            beanCandidates.addAll(
                    paths.parallel()
                            .filter(p -> p.toString().endsWith(".class"))
                            .map(path -> ROOT_PATH.relativize(path).toString()
                                    .replace(File.separatorChar, '.')
                                    .replaceAll("\\.class$", ""))
                            .map(name -> loadClass(name, classLoader))
                            .filter(Objects::nonNull)
                            .filter(c -> c.isAnnotationPresent(IGuessThisIsABean.class))
                            .collect(Collectors.toSet()));
            // if you are adding to a hashset either use concurrent data structures or thread safe operations on normal data structures.
            // I changed beanCandidates.add() to Collectors.toSet() because .add() is not a thread-safe operation. On the other hand Collectors.toSet()
            // creates different sets for each parallel thread, then merges them. So there is no concurrency problem.
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
        beans.put(beanCandidate, instance);

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
class PaymentService {

    public PaymentService() {
        System.out.println("PaymentService is created");
    }

    public void pay() {
        System.out.println("Paid!");
    }
}

@IGuessThisIsABean
class OrderService {
    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
        System.out.println("OrderService is created");
    }
    public void checkout() {
        paymentService.pay();
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface IGuessThisIsABean {}
