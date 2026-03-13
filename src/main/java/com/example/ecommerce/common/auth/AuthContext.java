package com.example.ecommerce.common.auth;

public final class AuthContext {

    private static final ThreadLocal<UserPrincipal> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(UserPrincipal userPrincipal) {
        HOLDER.set(userPrincipal);
    }

    public static UserPrincipal get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
