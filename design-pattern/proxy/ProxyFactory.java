package proxy;

public class ProxyFactory {
    public Subject getObject() {
        Subject real = new RealSubject();
        return new Proxy(real);
    }
}
