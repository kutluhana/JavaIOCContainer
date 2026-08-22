package location;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class Main {
    static void main() throws Exception {
        IOCContainer container = new IOCContainer();

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
    private final Map<Class<?>, Object> beans = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) throws Exception {
        if(beans.containsKey(type)) {
            return type.cast(beans.get(type));
        }

        Constructor<?>[] constructors = type.getConstructors();
        Constructor<?> constructor = constructors[0];

        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];

        for(int i = 0; i < parameterTypes.length; i++) {
            parameters[i] = getBean(parameterTypes[i]);
        }

        T instance = (T) constructor.newInstance(parameters);

        beans.put(type, instance);

        return instance;
    }
}
