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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@IGuessThisIsABean
public class Main {
    static void main() throws Exception {
        IOCContainer container = new IOCContainer();
    }
}

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

class IOCContainer {
    private static final Path ROOT_PATH = Paths.get("/Users/kutluhanpalalioglu/Desktop/JavaIOCContainer/target/classes").toAbsolutePath();
    private final Set<Class<?>> beanCandidates = new HashSet<>();
    private final Map<Class<?>, Object> beans = new HashMap<>();

    public IOCContainer() throws Exception {
        fillBeanCandidates();//How class? check it!!
        for(Class<?> beanCandidate : beanCandidates) {
            initializeBean(beanCandidate);
        }
    }

    public void fillBeanCandidates() {
        ClassLoader classLoader = Main.class.getClassLoader();

        try(Stream<Path> paths = Files.walk(ROOT_PATH)) {
            paths.parallel()
                    .filter(p -> p.toString().endsWith(".class"))
                    .forEach(path -> {
                        String fileName = ROOT_PATH.relativize(path).toString()
                                .replace(File.separatorChar, '.')
                                .replaceAll("\\.class$", "");

                        try {
                            Class<?> justAClass = Class.forName(fileName, false, classLoader);

                            if(justAClass.isAnnotationPresent(IGuessThisIsABean.class)) {
                                beanCandidates.add(justAClass);
                            }
                        } catch (ClassNotFoundException e) {
                            System.out.println("Where are my beans!!!");
                        }
                    });
        } catch (IOException exception) {
            System.out.println("Couldn't walk and fell...");
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

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface IGuessThisIsABean {}
