package com.bugfunbug.linearreader.linear;

import com.bugfunbug.linearreader.LinearRuntime;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class ZstdSupport {

    private static final String EMBEDDED_JAR_RESOURCE = "/META-INF/linearreader-libs/zstd-jni.jar";
    private static volatile Bridge bridge;
    private static volatile ZstdUnavailableException unavailableException;
    private static volatile Throwable testFailure;

    private ZstdSupport() {
    }

    public static void ensureAvailable() {
        bridge();
    }

    static long compressBound(long srcSize) {
        return bridge().compressBound(srcSize);
    }

    static long compress(byte[] dst, byte[] src, int level) {
        return bridge().compress(dst, src, level);
    }

    static long compress(byte[] dst, int dstOff, int dstLen, byte[] src, int srcOff, int srcLen, int level) {
        return bridge().compress(dst, dstOff, dstLen, src, srcOff, srcLen, level);
    }

    static long decompressedSize(byte[] src) {
        return bridge().decompressedSize(src);
    }

    static long decompressedSize(byte[] src, int srcOff, int srcLen) {
        return bridge().decompressedSize(src, srcOff, srcLen);
    }

    static long decompress(byte[] dst, byte[] src) {
        return bridge().decompress(dst, src);
    }

    static long decompress(byte[] dst, int dstOff, int dstLen, byte[] src, int srcOff, int srcLen) {
        return bridge().decompress(dst, dstOff, dstLen, src, srcOff, srcLen);
    }

    static boolean isError(long code) {
        return bridge().isError(code);
    }

    static String getErrorName(long code) {
        return bridge().getErrorName(code);
    }

    private static Bridge bridge() {
        Throwable injectedFailure = testFailure;
        if (injectedFailure != null) {
            throw unavailable(injectedFailure);
        }

        Bridge current = bridge;
        if (current != null) {
            return current;
        }

        ZstdUnavailableException cachedFailure = unavailableException;
        if (cachedFailure != null) {
            throw cachedFailure;
        }

        synchronized (ZstdSupport.class) {
            injectedFailure = testFailure;
            if (injectedFailure != null) {
                throw unavailable(injectedFailure);
            }

            current = bridge;
            if (current == null) {
                ZstdUnavailableException priorFailure = unavailableException;
                if (priorFailure != null) {
                    throw priorFailure;
                }
                try {
                    current = loadBridge();
                    bridge = current;
                } catch (Throwable t) {
                    throw unavailable(t);
                }
            }
            return current;
        }
    }

    static void setTestFailure(Throwable failure) {
        testFailure = failure;
    }

    static void clearTestFailure() {
        testFailure = null;
        unavailableException = null;
    }

    private static ZstdUnavailableException unavailable(Throwable cause) {
        if (cause instanceof ZstdUnavailableException unavailable) {
            unavailableException = unavailable;
            return unavailable;
        }

        ZstdUnavailableException cached = unavailableException;
        if (cached != null && sameFailure(cached.getCause(), cause)) {
            return cached;
        }

        StringBuilder message = new StringBuilder(
                "[LinearReader] Fatal startup incompatibility: zstd-jni could not be initialized. "
                        + "LinearReader cannot safely load worlds because .linear region data would not be writable."
        );
        if (looksLikeAndroidFcl(cause)) {
            message.append(" Android/FCL runtime detected; the embedded zstd-jni library is not supported there.");
        }

        ZstdUnavailableException failure = new ZstdUnavailableException(message.toString(), cause);
        unavailableException = failure;
        return failure;
    }

    private static boolean sameFailure(Throwable left, Throwable right) {
        return left == right || (left != null && right != null
                && left.getClass() == right.getClass()
                && String.valueOf(left.getMessage()).equals(String.valueOf(right.getMessage())));
    }

    private static boolean looksLikeAndroidFcl(Throwable cause) {
        StringBuilder haystack = new StringBuilder();
        appendEnvHint(haystack, System.getProperty("java.vendor"));
        appendEnvHint(haystack, System.getProperty("java.vm.vendor"));
        appendEnvHint(haystack, System.getProperty("java.runtime.name"));
        appendEnvHint(haystack, System.getProperty("java.library.path"));
        appendEnvHint(haystack, System.getProperty("java.class.path"));

        Throwable current = cause;
        while (current != null) {
            appendEnvHint(haystack, current.getClass().getName());
            appendEnvHint(haystack, current.getMessage());
            current = current.getCause();
        }

        String normalized = haystack.toString().toLowerCase(Locale.ROOT);
        return normalized.contains("android")
                || normalized.contains("com.tungsten.fcl")
                || normalized.contains("fclauncher")
                || normalized.contains("pojav");
    }

    private static void appendEnvHint(StringBuilder haystack, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!haystack.isEmpty()) {
            haystack.append(' ');
        }
        haystack.append(value);
    }

    private static Bridge loadBridge() {
        try {
            Path extractedJar = extractEmbeddedJar();
            URLClassLoader loader = new URLClassLoader(
                    new URL[]{extractedJar.toUri().toURL()},
                    ZstdSupport.class.getClassLoader()
            );
            Class<?> zstdClass = Class.forName("com.github.luben.zstd.Zstd", true, loader);
            Class<?> compressCtxClass = Class.forName("com.github.luben.zstd.ZstdCompressCtx", true, loader);
            Class<?> decompressCtxClass = Class.forName("com.github.luben.zstd.ZstdDecompressCtx", true, loader);
            LinearRuntime.LOGGER.debug("[LinearReader] Loaded embedded zstd-jni from {}.", extractedJar);
            return new Bridge(
                    loader,
                    zstdClass.getMethod("compressBound", long.class),
                    zstdClass.getMethod("decompressedSize", byte[].class),
                    zstdClass.getMethod("decompressedSize", byte[].class, int.class, int.class),
                    zstdClass.getMethod("isError", long.class),
                    zstdClass.getMethod("getErrorName", long.class),
                    compressCtxClass.getConstructor(),
                    compressCtxClass.getMethod("setLevel", int.class),
                    compressCtxClass.getMethod("compressByteArray",
                            byte[].class, int.class, int.class,
                            byte[].class, int.class, int.class),
                    decompressCtxClass.getConstructor(),
                    decompressCtxClass.getMethod("decompressByteArray",
                            byte[].class, int.class, int.class,
                            byte[].class, int.class, int.class)
            );
        } catch (ReflectiveOperationException | IOException e) {
            throw new IllegalStateException("[LinearReader] Failed to initialize embedded zstd-jni runtime.", e);
        }
    }

    private static Path extractEmbeddedJar() throws IOException {
        try (InputStream in = ZstdSupport.class.getResourceAsStream(EMBEDDED_JAR_RESOURCE)) {
            if (in == null) {
                throw new IOException("[LinearReader] Missing embedded zstd-jni resource: " + EMBEDDED_JAR_RESOURCE);
            }
            Path tempDir = Files.createTempDirectory("linearreader-zstd");
            Path jarPath = tempDir.resolve("zstd-jni.jar");
            Files.copy(in, jarPath, StandardCopyOption.REPLACE_EXISTING);
            tempDir.toFile().deleteOnExit();
            jarPath.toFile().deleteOnExit();
            return jarPath;
        }
    }

    public static final class ZstdUnavailableException extends IllegalStateException {

        private ZstdUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class Bridge {
        @SuppressWarnings("unused")
        private final URLClassLoader loader;
        private final Method compressBound;
        private final Method decompressedSize;
        private final Method decompressedSizeSlice;
        private final Method isError;
        private final Method getErrorName;
        private final Constructor<?> compressCtxCtor;
        private final Method compressCtxSetLevel;
        private final Method compressCtxCompressByteArray;
        private final Constructor<?> decompressCtxCtor;
        private final Method decompressCtxDecompressByteArray;
        private final ThreadLocal<CompressContextState> compressContexts;
        private final ThreadLocal<Object> decompressContexts;

        private Bridge(URLClassLoader loader,
                       Method compressBound,
                       Method decompressedSize,
                       Method decompressedSizeSlice,
                       Method isError,
                       Method getErrorName,
                       Constructor<?> compressCtxCtor,
                       Method compressCtxSetLevel,
                       Method compressCtxCompressByteArray,
                       Constructor<?> decompressCtxCtor,
                       Method decompressCtxDecompressByteArray) {
            this.loader = loader;
            this.compressBound = compressBound;
            this.decompressedSize = decompressedSize;
            this.decompressedSizeSlice = decompressedSizeSlice;
            this.isError = isError;
            this.getErrorName = getErrorName;
            this.compressCtxCtor = compressCtxCtor;
            this.compressCtxSetLevel = compressCtxSetLevel;
            this.compressCtxCompressByteArray = compressCtxCompressByteArray;
            this.decompressCtxCtor = decompressCtxCtor;
            this.decompressCtxDecompressByteArray = decompressCtxDecompressByteArray;
            this.compressContexts = ThreadLocal.withInitial(this::newCompressContextState);
            this.decompressContexts = ThreadLocal.withInitial(this::newDecompressContext);
        }

        private long compressBound(long srcSize) {
            return invokeStaticLong(compressBound, srcSize);
        }

        private long compress(byte[] dst, byte[] src, int level) {
            return compress(dst, 0, dst.length, src, 0, src.length, level);
        }

        private long compress(byte[] dst, int dstOff, int dstLen, byte[] src, int srcOff, int srcLen, int level) {
            CompressContextState state = compressContexts.get();
            state.applyLevel(level);
            return invokeInstanceLong(state.context, compressCtxCompressByteArray,
                    dst, dstOff, dstLen, src, srcOff, srcLen);
        }

        private long decompressedSize(byte[] src) {
            return invokeStaticLong(decompressedSize, src);
        }

        private long decompressedSize(byte[] src, int srcOff, int srcLen) {
            return invokeStaticLong(decompressedSizeSlice, src, srcOff, srcLen);
        }

        private long decompress(byte[] dst, byte[] src) {
            return decompress(dst, 0, dst.length, src, 0, src.length);
        }

        private long decompress(byte[] dst, int dstOff, int dstLen, byte[] src, int srcOff, int srcLen) {
            return invokeInstanceLong(decompressContexts.get(), decompressCtxDecompressByteArray,
                    dst, dstOff, dstLen, src, srcOff, srcLen);
        }

        private boolean isError(long code) {
            return invokeStaticBoolean(isError, code);
        }

        private String getErrorName(long code) {
            return invokeStaticString(getErrorName, code);
        }

        private CompressContextState newCompressContextState() {
            return new CompressContextState(newInstance(compressCtxCtor));
        }

        private Object newDecompressContext() {
            return newInstance(decompressCtxCtor);
        }

        private static long invokeStaticLong(Method method, Object... args) {
            return ((Number) invoke(null, method, args)).longValue();
        }

        private static long invokeInstanceLong(Object target, Method method, Object... args) {
            return ((Number) invoke(target, method, args)).longValue();
        }

        private static boolean invokeStaticBoolean(Method method, Object... args) {
            return (Boolean) invoke(null, method, args);
        }

        private static String invokeStaticString(Method method, Object... args) {
            return (String) invoke(null, method, args);
        }

        private static Object newInstance(Constructor<?> constructor) {
            try {
                return constructor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("[LinearReader] Could not create embedded zstd-jni context " + constructor.getDeclaringClass().getSimpleName() + ".", e);
            }
        }

        private static Object invoke(Object target, Method method, Object... args) {
            try {
                return method.invoke(target, args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("[LinearReader] Could not access embedded zstd-jni method " + method.getName() + ".", e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new IllegalStateException("[LinearReader] Embedded zstd-jni call failed: " + method.getName() + ".", cause);
            }
        }

        private final class CompressContextState {
            private final Object context;
            private int level = Integer.MIN_VALUE;

            private CompressContextState(Object context) {
                this.context = context;
            }

            private void applyLevel(int nextLevel) {
                if (level == nextLevel) return;
                invoke(context, compressCtxSetLevel, nextLevel);
                level = nextLevel;
            }
        }
    }
}
