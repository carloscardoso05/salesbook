package io.github.carloscardoso05.api.shared;

public class NotFoundException extends RuntimeException {
    public NotFoundException(Class<?> clazz, Object id) {
        super("%s for id %s not found".formatted(clazz.getSimpleName(), id));
    }
}
