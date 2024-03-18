package com.simley.lib_socket.aysnc.http.server;

public interface RouteMatcher {
    AsyncHttpServerRouter.RouteMatch route(String method, String path);
}