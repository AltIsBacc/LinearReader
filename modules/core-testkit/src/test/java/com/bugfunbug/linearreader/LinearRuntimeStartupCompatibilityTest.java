package com.bugfunbug.linearreader;

import com.bugfunbug.linearreader.linear.LinearCoreTestHooks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinearRuntimeStartupCompatibilityTest {

    @AfterEach
    void tearDown() {
        LinearCoreTestHooks.clearZstdStartupFailure();
    }

    @Test
    void installFailsFastWhenZstdIsUnavailable() {
        UnsatisfiedLinkError cause = new UnsatisfiedLinkError(
                "/data/data/com.tungsten.fcl/cache/fclauncher/libzstd-jni.so: dlopen failed"
        );
        LinearCoreTestHooks.setZstdStartupFailure(cause);

        IllegalStateException error = assertThrows(IllegalStateException.class, LinearReader::installForTests);

        assertTrue(error.getMessage().contains("cannot safely load worlds"));
        assertTrue(error.getMessage().contains("Android/FCL"));
        assertSame(cause, error.getCause());
    }
}
