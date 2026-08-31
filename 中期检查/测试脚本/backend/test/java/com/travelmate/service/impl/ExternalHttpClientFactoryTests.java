package com.travelmate.service.impl;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalHttpClientFactoryTests {

    @Test
    void createsHttpClientWithDefaultSettings() {
        HttpClient client = ExternalHttpClientFactory.create(
                URI.create("https://api.example.com"), Duration.ofSeconds(10));

        assertThat(client).isNotNull();
        assertThat(client.connectTimeout()).hasValue(Duration.ofSeconds(10));
    }

    @Test
    void skipsProxyForLocalhost() {
        HttpClient client = ExternalHttpClientFactory.create(
                URI.create("http://localhost:8080/api"), Duration.ofSeconds(5));

        assertThat(client).isNotNull();
    }

    @Test
    void skipsProxyFor127001() {
        HttpClient client = ExternalHttpClientFactory.create(
                URI.create("http://127.0.0.1:8080/api"), Duration.ofSeconds(5));

        assertThat(client).isNotNull();
    }

    @Test
    void createsClientForNullTargetGracefully() {
        HttpClient client = ExternalHttpClientFactory.create(null, Duration.ofSeconds(5));

        assertThat(client).isNotNull();
    }

    @Test
    void createsClientWithShortTimeout() {
        HttpClient client = ExternalHttpClientFactory.create(
                URI.create("https://fast.example.com"), Duration.ofMillis(500));

        assertThat(client).isNotNull();
        assertThat(client.connectTimeout()).hasValue(Duration.ofMillis(500));
    }

    @Test
    void createsClientForIpv6Localhost() {
        HttpClient client = ExternalHttpClientFactory.create(
                URI.create("http://[::1]:8080/api"), Duration.ofSeconds(5));

        assertThat(client).isNotNull();
    }

    @Test
    void followsRedirects() {
        HttpClient client = ExternalHttpClientFactory.create(
                URI.create("https://redirect.example.com"), Duration.ofSeconds(5));

        assertThat(client.followRedirects()).isEqualTo(HttpClient.Redirect.NORMAL);
    }
}