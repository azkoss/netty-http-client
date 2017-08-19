/*
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.netty.http.client;

import com.mastfrog.url.Protocols;
import com.mastfrog.url.URL;
import com.mastfrog.util.Exceptions;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Tim Boudreau
 */
final class RequestInfo {

    final URL url;
    final HttpRequest req;
    final AtomicBoolean cancelled;
    final ResponseFuture handle;
    final ResponseHandler<?> r;
    final AtomicInteger redirectCount = new AtomicInteger();
    final Duration timeout;
    final ZonedDateTime startTime;
    volatile boolean listenerAdded;
    TimerTask timer;
    final boolean dontAggregate;
    final ChunkedContent chunkedBody;
    WebSocketClientHandshaker websocketHandshaker;
    final WebSocketVersion websocketVersion;

    RequestInfo(URL url, HttpRequest req, AtomicBoolean cancelled, ResponseFuture handle, ResponseHandler<?> r,
            Duration timeout, ZonedDateTime startTime, TimerTask timer, boolean noAggregate, ChunkedContent chunkedBody,
            WebSocketVersion websocketVersion) {
        this.url = url;
        this.req = req;
        this.cancelled = cancelled;
        this.handle = handle;
        this.r = r;
        this.timeout = timeout;
        this.startTime = startTime;
        this.timer = timer;
        this.dontAggregate = noAggregate;
        this.chunkedBody = chunkedBody;
        this.websocketVersion = websocketVersion;
    }

    RequestInfo(URL url, HttpRequest req, AtomicBoolean cancelled, ResponseFuture handle, ResponseHandler<?> r, Duration timeout, TimerTask timer, boolean noAggregate, ChunkedContent chunkedBody, WebSocketVersion websocketVersion) {
        this(url, req, cancelled, handle, r, timeout, ZonedDateTime.now(), timer, noAggregate, chunkedBody, websocketVersion);
    }

    Duration age() {
        return Duration.between(startTime, ZonedDateTime.now());
    }

    Duration remaining() {
        return timeout == null ? null : timeout.minus(age());
    }

    boolean isExpired() {
        if (timeout != null) {
            return ZonedDateTime.now().isAfter(startTime.plus(timeout));
        }
        return false;
    }

    void cancelTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    WebSocketClientHandshaker handshaker() {
        if (websocketHandshaker != null) {
            return websocketHandshaker;
        }
        URL websocketUrl = url.withProtocol(url.getProtocol().isSecure() ? Protocols.WSS : Protocols.WS);
        try {
            return websocketHandshaker = WebSocketClientHandshakerFactory.newHandshaker(
                    websocketUrl.toURI(), websocketVersion, null, true, new DefaultHttpHeaders());
        } catch (URISyntaxException ex) {
            return Exceptions.chuck(ex);
        }
    }

    boolean hasHandshaker() {
        return websocketHandshaker != null;
    }

    @Override
    public String toString() {
        return "RequestInfo{" + "url=" + url + ", req=" + req + ", cancelled="
                + cancelled + ", handle=" + handle + ", r=" + r + ", timeout=" + timeout + '}';
    }
}
