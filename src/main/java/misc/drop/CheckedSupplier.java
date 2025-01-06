package misc.drop;

public interface CheckedSupplier<T, E extends Exception> {
    T get() throws E;
}
