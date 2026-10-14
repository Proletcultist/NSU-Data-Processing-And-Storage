package ru.nsu.zenin.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.AccessLevel;
import java.util.concurrent.Callable;
import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Result<T> {
    private final Exception exception;
    private final T val;
    @Getter private final boolean ok;

    public static <U> Result<U> ofValue(U val) {
        return new Result<U>(null, val, true);
    }

    public static <U> Result<U> ofException(Exception exception) {
        return new Result<U>(exception, null, false);
    }

    public static <U> Result<U> ofTry(Callable<U> cal) {
        try {
            return Result.ofValue(cal.call());
        } catch (Exception e) {
            return Result.ofException(e);
        }
    }

    public <U> Result<U> map(Function<T, U> fun) {
        if (ok) {
            return Result.ofValue(fun.apply(val));
        } else {
            return Result.ofException(exception);
        }
    }

    public T or(T altVal) {
        if (ok) {
            return val;
        } else {
            return altVal;
        }
    }

    public T or(Function<Exception, T> fun) {
        if (ok) {
            return val;
        } else {
            return fun.apply(exception);
        }
    }

    public T getValue() {
        if (!ok) {
            throw new IllegalStateException("No value in fail Result");
        }

        return val;
    }
    
    public Exception getException() {
        if (ok) {
            throw new IllegalStateException("No exception in successful CrawlingResult");
        }

        return exception;
    }
}
