package io.datawire.quark.netty;

import io.datawire.quark.runtime.AbstractDatawireRuntime;
import io.datawire.quark.runtime.Buffer;
import io.datawire.quark.runtime.BufferImpl;
import io.datawire.quark.runtime.Builtins;
import io.datawire.quark.runtime.Codec;
import io.datawire.quark.runtime.HTTPHandler;
import io.datawire.quark.runtime.HTTPRequest;
import io.datawire.quark.runtime.HTTPResponse;
import io.datawire.quark.runtime.HTTPServlet;
import io.datawire.quark.runtime.Runtime;
import io.datawire.quark.runtime.Task;
import io.datawire.quark.runtime.WSHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.CharsetUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

public class QuarkNettyRuntime extends AbstractDatawireRuntime implements Runtime {

    private final Object lock = new Object();
    private boolean locked = false;
    private final EventLoopGroup group = new NioEventLoopGroup();

    public void acquire() {
        synchronized(lock) {
            assert !locked;
            locked = true;
        }
    }

    public void release() {
        synchronized(lock) {
            assert locked;
            locked = false;
        }
    }

    public void wait(Double timeoutInSeconds) {
        synchronized(lock) {
            assert locked;
            if (!allowSync) {
                throw new IllegalStateException("Runtime is not configured for synchronous mode");
            }
            double timeoutInMs = timeoutInSeconds * 1000;
            long millis = (long) (timeoutInMs);
            int nanos = (int)((timeoutInMs - millis) * 1000000);
            try {
                lock.wait(millis, nanos);
            } catch (InterruptedException e) {
                // ignore, wait is best effort
            }
        }
    }

    boolean allowSync = false;
    boolean initialized = false;

    /**
     * Allow synchronous waiting on this instance of runtime
     * @param allowSync
     */
    void setAllowSync(boolean allowSync) {
        synchronized (lock) {
            if (initialized) {
                throw new IllegalStateException("setAllowSync can only be called once, before runtime is used");
            }
            this.allowSync = allowSync;
            initialize();
        }
    }

    @Override
	public boolean isAllowSync() {
		return allowSync;
	}

	public boolean isInitialized() {
		return initialized;
	}

	private Runnable notifier = new Runnable() {
        @Override
        public void run() {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    };
    @Override
	protected void wakeup() {
        group.submit(notifier );
    }

    @Override
	protected void initialize() {
        synchronized (lock) {
            initialized = true;
        }
    }

    public void open(String url, final WSHandler handler) {
        final WSHandler ws_handler = wrap(handler);
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            ws_handler.onWSError(null); // XXX websocket error handling
            return;
        }
        String scheme = uri.getScheme() == null? "ws" : uri.getScheme();
        final String host = uri.getHost() == null? "127.0.0.1" : uri.getHost();
        final int port;
        if (uri.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            System.err.println("Only WS(S) is supported.");
            ws_handler.onWSError(null); // XXX websocket error handling
            return;
        }

        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            try {
                sslCtx = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } catch (SSLException e) {
                ws_handler.onWSError(null); // XXX websocket error handling
                return;
            }
        } else {
            sslCtx = null;
        }
        final QuarkNettyWebsocket ws =
                new QuarkNettyWebsocket(
                        WebSocketClientHandshakerFactory.newHandshaker(
                                uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()),
                                ws_handler);

        ws_handler.onWSInit(ws);

        Bootstrap b = new Bootstrap();
        b.group(group)
        .channel(NioSocketChannel.class)
        .handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                if (sslCtx != null) {
                    p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                }
                p.addLast(
                        new HttpClientCodec(),
                        new HttpObjectAggregator(8192),
                        ws);
                ch.closeFuture().addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isDone()) {
                            if (future.isSuccess()) {
                                ws_handler.onWSClosed(ws);
                            } else {
                                ws_handler.onWSError(ws);
                            }
                            ws_handler.onWSFinal(ws);
                        }
                    }
                });
            }
        });
        ChannelFuture connecting = b.connect(uri.getHost(), port);
        connecting.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isDone()) {
                    if (future.isSuccess()) {
                        // fire onConnected only when websocket is fully connected
                    } else {
                        ws_handler.onWSError(ws);
                    }
                }
            }
        });
    }

    @Override
    public void schedule(Task handler, Double delayInSeconds) {
        final Task t_handler = wrap(handler);
        final Runtime self = this;
        group.schedule(new Runnable() {

            @Override
            public void run() {
                t_handler.onExecute(self);
            }
        }, Double.valueOf(delayInSeconds * 1000).intValue(), TimeUnit.MILLISECONDS);
    }

    public void launch() {
        try {
            group.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // empty
        }
    }

    @Override
    public void request(final HTTPRequest request, HTTPHandler handler) {
        final HTTPHandler ht_handler = wrap(handler);
        final URI uri;
        try {
            uri = new URI(request.getUrl());
        } catch (URISyntaxException e) {
            ht_handler.onHTTPError(request);
            return;
        }
        String scheme = uri.getScheme() == null? "http" : uri.getScheme();
        final String host = uri.getHost() == null? "127.0.0.1" : uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            }
        }

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            System.err.println("Only HTTP(S) is supported.");
            ht_handler.onHTTPError(request);
            return;
        }

        // Configure SSL context if necessary.
        final boolean ssl = "https".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            try {
                sslCtx = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } catch (SSLException e) {
                ht_handler.onHTTPError(null);
                return;
            }
        } else {
            sslCtx = null;
        }

        final ByteBuf content;
        if (request.getBody() != null) {
            content = io.netty.buffer.Unpooled.copiedBuffer(request.getBody(), CharsetUtil.UTF_8);
        } else {
            content = io.netty.buffer.Unpooled.buffer(0);
        }
        final FullHttpRequest request1;
        request1 = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.valueOf(request.getMethod().toUpperCase()), uri.getRawPath(), content);
        request1.headers().set("Host", host);
        request1.headers().set("Connection", "close");
        // request1.headers().set("Accept-Encoding", "gzip"); // XXX: extra runtime dependencies
        HttpHeaders.setContentLength(request1, content.readableBytes());

        for(String header : request.getHeaders()) {
            request1.headers().set(header, request.getHeader(header));
        }

        // Configure the client.
        Bootstrap b = new Bootstrap();
        b.group(group)
        .channel(NioSocketChannel.class)
        .handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch)
                    throws Exception {
                ChannelPipeline p = ch.pipeline();

                // Enable HTTPS if necessary.
                if (sslCtx != null) {
                    p.addLast(sslCtx.newHandler(ch.alloc()));
                }

                p.addLast(new HttpClientCodec());

                // Remove the following line if you don't want automatic content decompression.
                p.addLast(new HttpContentDecompressor());

                // Uncomment the following line if you don't want to handle HttpContents.
                p.addLast(new HttpObjectAggregator(1048576));

                p.addLast(new QuarkNettyHttpHandler(request, ht_handler));

                ch.closeFuture().addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        ht_handler.onHTTPFinal(request);
                    }
                });

            }
        });

        // Make the connection attempt.
        ChannelFuture connecting = b.connect(host, port);
        connecting.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isDone()) {
                    if (!future.isSuccess()) {
                        ht_handler.onHTTPError(request);
                    } else {
                        // Send the HTTP request.
                        future.channel().writeAndFlush(request1).addListener(new ChannelFutureListener() {

                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                if (future.isDone()) {
                                    if (future.isSuccess()) {
                                        // yay
                                    } else {
                                        ht_handler.onHTTPError(request);
                                    }
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    public Codec codec() {
        return Builtins.defaultCodec();
    }

    public static ByteBuf adaptBuffer(Buffer quarkBuffer) {
        // TODO: add netty implementation of runtime buffer and short-circuit here
        if (quarkBuffer instanceof BufferImpl) {
            return ((BufferImpl)quarkBuffer).data();
        }
        // Slow path
        int capacity = quarkBuffer.capacity();
        ByteBuf copy = ByteBufAllocator.DEFAULT.buffer(capacity);
        for(int i = 0; i < capacity; i++) { // XXX: really? :)
            copy.writeByte(quarkBuffer.getByte(i));
        }
        return copy;
    }

    @Override
    public void serveHTTP(final String url, HTTPServlet servlet) {
        final HTTPServlet servlet_w = wrap(servlet);
        final URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            servlet_w.onHTTPError(url);
            return;
        }
        String scheme = uri.getScheme() == null? "http" : uri.getScheme();
        final String host = uri.getHost() == null? "127.0.0.1" : uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            }
        }

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            System.err.println("Only HTTP(S) is supported.");
            servlet_w.onHTTPError(url);
            return;
        }

        final SslContext sslCtx;
        final boolean ssl = "https".equalsIgnoreCase(scheme);
        if (ssl) {
            try {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } catch (SSLException e) {
                servlet_w.onHTTPError(url);
                return;
            } catch (CertificateException e) {
                servlet_w.onHTTPError(url);
                return;
            }
        } else {
            sslCtx = null;
        }
        
        ServerBootstrap b = new ServerBootstrap();
        b.group(this.group)
         .channel(NioServerSocketChannel.class)
         .childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                if (sslCtx != null) {
                    p.addLast(sslCtx.newHandler(ch.alloc()));
                }
                p.addLast(new HttpRequestDecoder());
                p.addLast(new HttpResponseEncoder());
                p.addLast(new HttpObjectAggregator(1024*1024)); // XXX: nobody needs more than one megabytez
                p.addLast(new DatawireNettyHttpRequestHandler(QuarkNettyRuntime.this, servlet_w));
            }
        });
        b.bind(host,port).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isDone()) {
                    if (future.isSuccess()) {
                        servlet_w.onHTTPInit(url, QuarkNettyRuntime.this);
                    } else {
                        servlet_w.onHTTPError(url);
                    }
                }
            }
        });
    }

    @Override
    public void respond(HTTPRequest request_in, HTTPResponse response_in) {
        if (response_in instanceof DatawireNettyHttpRequestHandler.Response) {
            DatawireNettyHttpRequestHandler.Response response = (DatawireNettyHttpRequestHandler.Response) response_in; 
            if (response.request() == request_in) {
                response.respond();
                return;
            } else {
                response.fail(500, "Unmatched request and response");
            }
        } else {
            // XXX what to do with a response from a different runtime?
        }
    }

}