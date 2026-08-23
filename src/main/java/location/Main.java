package location;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Main {
    static void main() throws Exception {
        IOCContainer container = new IOCContainer();

        container.findBeans();

        OrderService orderService = container.getBean(OrderService.class);

        orderService.checkout();
    }
}

class PaymentService {

    public PaymentService() {}

    public void pay() {
        System.out.println("Paid!");
    }
}

class OrderService {
    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    public void checkout() {
        paymentService.pay();
    }
}

class IOCContainer {
    private final Collection<Class<?>> beanCandidates = new ArrayList<>();
    private final Map<Class<?>, Object> beans = new HashMap<>();

    public IOCContainer() throws Exception {
        findBeans();
        for(Class<?> beanCandidate : beanCandidates) {
            initializeBean(beanCandidate);
        }
    }

    public <T> T findBeans() {

        //discover annotated classes

        return null;
    }

    @SuppressWarnings("unchecked")
    public void initializeBean(Class<?> beanCandidate) throws Exception {

        Constructor<?>[] constructors = beanCandidate.getConstructors();
        Constructor<?> constructor = constructors[0];

        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];

        for(int i = 0; i < parameterTypes.length; i++) {
            parameters[i] = getBean(parameterTypes[i]);
        }

        Object instance = constructor.newInstance(parameters);

        beans.put(beanCandidate, instance);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        if(beans.containsKey(type)) {
            return type.cast(beans.get(type));
        } else {
            throw new RuntimeException();
        }
    }
}
