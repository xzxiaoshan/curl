package org.toilelibre.libe.curl;

import org.apache.commons.cli.CommandLine;
import org.toilelibre.libe.curl.http.Request;
import org.toilelibre.libe.curl.http.Response;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

/**
 * 拦截器处理器
 *
 * @author shanhy
 * @date 2023-07-31 17:51
 */
public class InterceptorsHandler {

    private static final BiFunction<Request, Supplier<Response>, Response> EXAMPLE
            = ((request, responseSupplier) -> responseSupplier.get());

    private static final Type EXAMPLE_TYPE;

    static {
        try {
            EXAMPLE_TYPE = InterceptorsHandler.class.getDeclaredField("EXAMPLE").getGenericType();
        } catch (NoSuchFieldException e) {
            throw new CurlException(new IllegalArgumentException(e));
        }
    }

    public Response handleInterceptors(Request request, Supplier<Response> realCall,
                                       List<BiFunction<Request, Supplier<Response>, Response>> remainingInterceptors) {
        if (remainingInterceptors.size() > 0) {
            BiFunction<Request, Supplier<Response>, Response> nextInterceptor =
                    remainingInterceptors.get(0);
            return nextInterceptor.apply(request, () -> this.handleInterceptors(request, realCall,
                    remainingInterceptors.subList(1, remainingInterceptors.size())));
        } else return realCall.get();
    }

    @SuppressWarnings("unchecked")
    public List<BiFunction<Request, Supplier<Response>, Response>> getInterceptors(
            final CommandLine commandLine, List<BiFunction<Request, Supplier<Response>, Response>> additionalInterceptors) {
        return
                concat(stream(Optional.ofNullable(commandLine.getOptionValues(Arguments.INTERCEPTOR.getOpt())).orElse(new String[0]))
                        .map(methodName -> {
                            final Class<?> targetClass;
                            try {
                                targetClass = Class.forName(methodName.split("::")[0]);
                            } catch (ClassNotFoundException e) {
                                return null;
                            }
                            Object newInstance;
                            try {
                                newInstance = targetClass.getDeclaredConstructor().newInstance();
                            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                                     InvocationTargetException e) {
                                newInstance = null;
                            }
                            final Object finalNewInstance = newInstance;
                            try {
                                final BiFunction<Request, Supplier<Response>, Response> candidate =
                                        stream(targetClass.getDeclaredFields()).filter(f ->
                                                        EXAMPLE_TYPE.equals(f.getGenericType()))
                                                .findFirst()
                                                .map(f -> {
                                                    try {
                                                        f.setAccessible(true);
                                                        return (BiFunction<Request, Supplier<Response>, Response>) f.get(finalNewInstance);
                                                    } catch (IllegalAccessException e) {
                                                        return null;
                                                    }
                                                }).orElse(null);
                                if (candidate != null)
                                    return candidate;
                                final Method targetMethod = stream(targetClass.getDeclaredMethods()).filter(m ->
                                        methodName.split("::")[1].equals(m.getName())).findFirst().orElse(null);
                                if (targetMethod == null) return null;
                                return (BiFunction<Request, Supplier<Response>, Response>)
                                        (request, subsequentCall) -> {
                                            try {
                                                return (Response) targetMethod.invoke(finalNewInstance,
                                                        request,
                                                        subsequentCall);
                                            } catch (IllegalAccessException | InvocationTargetException e) {
                                                throw new CurlException(e);
                                            }
                                        };
                            } catch (ClassCastException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull), additionalInterceptors.stream())
                        .collect(toList());
    }

    public static <T> T convert(Object obj, Class<T> clazz) {
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        } else {
            return null;
        }
    }

}
