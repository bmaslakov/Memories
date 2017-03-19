package io.uuddlrlrba.memories.core;

public interface CommandCallback<T> {
    void onResult(T result);

    void onError(Exception e);
}
