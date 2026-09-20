package com.swamibags.catalog.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public final class SharedFilePermissions {
    private static final Set<PosixFilePermission> PUBLIC_FILE = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.OTHERS_READ);

    private static final Set<PosixFilePermission> PUBLIC_DIRECTORY = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_EXECUTE);

    private SharedFilePermissions() {
    }

    public static void makeFilePublicReadable(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, PUBLIC_FILE);
        } catch (UnsupportedOperationException ignored) {
            // Windows and other non-POSIX development environments.
        }
    }

    public static void makeDirectoryPublicReadable(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, PUBLIC_DIRECTORY);
        } catch (UnsupportedOperationException ignored) {
            // Windows and other non-POSIX development environments.
        }
    }
}
