package com.takin.rpc.remoting.nio.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.takin.emmet.util.SystemClock;

/**
 * 
 */
public class IoFuture implements Future {

    private static final Logger LOGGER = LoggerFactory.getLogger(IoFuture.class);
    private static final long DEAD_LOCK_CHECK_INTERVAL = 5000L;
    private boolean success = false;
    private Throwable cause;
    private String msg;
    private List<IoFutureListener> listeners;
    private boolean done = false;
    /**
     * A lock used by the wait() method
     */
    private final Object lock = this;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Throwable cause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean isDone() {
        return done;
    }

    public void addListener(IoFutureListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (lock) {
            if (listeners == null) {
                listeners = new ArrayList<IoFutureListener>();
            }
            listeners.add(listener);
            if (isDone()) {
                complete(listener);
            }
        }
    }

    public void addListener(List<IoFutureListener> listeners) {
        if (listeners == null) {
            return;
        }

        synchronized (lock) {
            if (this.listeners == null) {
                this.listeners = new ArrayList<IoFutureListener>();
            }
            this.listeners.addAll(listeners);

            if (isDone()) {
                for (IoFutureListener listener : listeners) {
                    complete(listener);
                }
            }
        }
    }

    public void removeListener(IoFutureListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (lock) {
            if (this.listeners != null) {
                this.listeners.remove(listener);
            }
        }
    }

    public void notifyListeners() {
        synchronized (lock) {
            done = true;
            if (this.listeners != null) {
                for (IoFutureListener ioFutureListener : listeners) {
                    complete(ioFutureListener);
                }
            }
        }
    }

    private void complete(IoFutureListener listener) {
        try {
            listener.operationComplete(this);
        } catch (Exception e) {
            LOGGER.error("notify listener {} error ", listener, e);
        }
    }

    public boolean awaitUninterruptibly(long timeoutMillis) {
        try {
            return await0(timeoutMillis, false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    private boolean await0(long timeoutMillis, boolean interruptable) throws InterruptedException {
        if (isDone()) {
            return true;
        }

        if (timeoutMillis <= 0) {
            return isDone();
        }

        if (interruptable && Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        long endTime = SystemClock.now() + timeoutMillis;

        boolean interrupted = false;

        synchronized (lock) {
            if (isDone()) {
                return true;
            }

            if (timeoutMillis <= 0) {
                return isDone();
            }

            try {
                for (;;) {

                    long timeOut = Math.min(timeoutMillis, DEAD_LOCK_CHECK_INTERVAL);
                    try {
                        lock.wait(timeOut);
                    } catch (InterruptedException e) {
                        if (interruptable) {
                            throw e;
                        } else {
                            interrupted = true;
                        }

                    }

                    if (isDone()) {
                        return true;
                    }

                    if (endTime < SystemClock.now()) {
                        return isDone();
                    }
                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

}
