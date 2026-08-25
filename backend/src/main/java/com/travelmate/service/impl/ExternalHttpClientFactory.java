package com.travelmate.service.impl;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

/** Creates outbound HTTP clients that honor the host's standard proxy environment. */
final class ExternalHttpClientFactory {

    private ExternalHttpClientFactory() {
    }

    static HttpClient create(URI target, Duration connectTimeout) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL);
        URI proxy = resolveProxy(target);
        if (proxy != null && proxy.getHost() != null && proxy.getPort() > 0) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())));
        }
        return builder.build();
    }

    private static URI resolveProxy(URI target) {
        if (target == null || isLocalTarget(target.getHost()) || matchesNoProxy(target.getHost())) {
            return null;
        }
        String configured = firstNonBlank(
                System.getenv("HTTPS_PROXY"), System.getenv("https_proxy"),
                System.getenv("ALL_PROXY"), System.getenv("all_proxy"));
        if (configured == null) {
            return null;
        }
        try {
            URI proxy = URI.create(configured.trim());
            return proxy.getHost() == null ? null : proxy;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isLocalTarget(String host) {
        return host == null || "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    private static boolean matchesNoProxy(String host) {
        String noProxy = firstNonBlank(System.getenv("NO_PROXY"), System.getenv("no_proxy"));
        if (host == null || noProxy == null) {
            return false;
        }
        for (String entry : noProxy.split(",")) {
            String candidate = entry.trim();
            if (candidate.isEmpty()) {
                continue;
            }
            int colon = candidate.lastIndexOf(':');
            if (colon > 0 && candidate.indexOf(':') == colon) {
                candidate = candidate.substring(0, colon);
            }
            if ("*".equals(candidate) || host.equalsIgnoreCase(candidate)
                    || (candidate.startsWith(".") && host.toLowerCase().endsWith(candidate.toLowerCase()))) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
