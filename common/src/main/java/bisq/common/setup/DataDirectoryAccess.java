/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.common.setup;

import com.google.common.annotations.VisibleForTesting;

import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;

import java.io.File;
import java.io.IOException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

/**
 * Keeps other users out of the data directory, as specified in
 * docs/specifications/application/data-directory-access.md.
 * Only what Bisq uses is changed: the data directory itself, the config file and the logs in it, and the directory of
 * the network. Links below the data directory are not followed. A failure is only logged.
 * <p>
 * The mode of a directory is set before its entries are read, so that other users cannot replace an entry with a link
 * while its path is used. Other processes of the same user could, so this must run before Tor starts.
 */
@Slf4j
class DataDirectoryAccess {
    private static final Set<PosixFilePermission> OWNER_ONLY =
            Collections.unmodifiableSet(PosixFilePermissions.fromString("rwx------"));
    private static final Pattern CONFIG_OR_LOG_FILE = Pattern.compile("bisq\\.properties|bisq(_\\d+)?\\.log");

    private final List<Path> entriesOfOtherUsers = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();

    static void restrictToOwner(File appDataDir, File networkDir) {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return;
        }
        try {
            new DataDirectoryAccess().restrict(appDataDir.toPath().toRealPath(), networkDir.getName());
        } catch (IOException | UnsupportedOperationException e) {
            log.warn("Could not make {} accessible to its owner only: {}", appDataDir, e.toString());
        }
    }

    private void restrict(Path root, String networkDirName) throws IOException {
        PosixFileAttributes attributes = readAttributes(root);
        boolean isRestricted = restrictRoot(root, attributes);
        // Root could change what belongs to root in the data directory of a user, and lock out the user who works with
        // it. A process of another user can only change what it owns anyway.
        UserPrincipal requiredOwner = isRestricted ? attributes.owner() : null;
        restrictConfigAndLogFiles(root, requiredOwner);
        restrictNetworkDir(root.resolve(networkDirName), requiredOwner,
                isRestricted || isWritableByRootOnly(root, attributes));
        if (!entriesOfOtherUsers.isEmpty()) {
            log.warn("These belong to another user than the directory above them. They keep their modes and may be " +
                    "open to other users. Change their owner, for example with chown -R as root: {}",
                    entriesOfOtherUsers);
        }
        if (!failures.isEmpty()) {
            log.warn("Could not make these accessible to their owner only: {}", failures);
        }
    }

    private static boolean restrictRoot(Path root, PosixFileAttributes attributes) {
        try {
            Files.setPosixFilePermissions(root, OWNER_ONLY);
            return true;
        } catch (IOException e) {
            log.warn("Could not make the data directory {} accessible to its owner {} only: {}. Its owner, and " +
                            "everyone who may write in it, can replace the files and directories in it. It should " +
                            "belong to the user who runs Bisq.",
                    root, attributes.owner(), e.toString());
            return false;
        }
    }

    // Nobody but root, which can do so anyway, can replace an entry of such a directory
    private static boolean isWritableByRootOnly(Path directory, PosixFileAttributes attributes) throws IOException {
        Set<PosixFilePermission> permissions = attributes.permissions();
        return Files.getAttribute(directory, "unix:uid", LinkOption.NOFOLLOW_LINKS).equals(0) &&
                !permissions.contains(PosixFilePermission.GROUP_WRITE) &&
                !permissions.contains(PosixFilePermission.OTHERS_WRITE);
    }

    private void restrictConfigAndLogFiles(Path root, @Nullable UserPrincipal requiredOwner) {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(root,
                entry -> CONFIG_OR_LOG_FILE.matcher(entry.getFileName().toString()).matches())) {
            for (Path entry : entries) {
                restrictFile(entry, requiredOwner);
            }
        } catch (IOException | DirectoryIteratorException e) {
            log.warn("Could not make the config file and the logs in {} accessible to their owner only: {}", root,
                    e.toString());
        }
    }

    // Takes the permissions of others only, so that a file which its owner made read-only stays read-only
    @VisibleForTesting
    void restrictFile(Path file, @Nullable UserPrincipal requiredOwner) {
        try {
            PosixFileAttributes attributes = readAttributes(file);
            if (attributes.isSymbolicLink()) {
                warnIfLinkTargetIsOpen(file);
            } else if (attributes.isRegularFile() && isOpen(attributes) && isOfOwner(file, attributes, requiredOwner)) {
                Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
                permissions.addAll(attributes.permissions());
                permissions.retainAll(OWNER_ONLY);
                setPermissionsWithoutFollowingALink(file, permissions);
            }
        } catch (IOException e) {
            addFailure(file, e);
        }
    }

    /**
     * @param requiredOwner the owner which the directory must have, or null for any
     * @param isPathStable  whether nobody else can replace the directory. If not, only the directory itself gets the
     *                      mode, because a path below it could lead elsewhere.
     */
    @VisibleForTesting
    void restrictNetworkDir(Path networkDir, @Nullable UserPrincipal requiredOwner, boolean isPathStable) {
        try {
            PosixFileAttributes attributes = readAttributes(networkDir);
            if (attributes.isSymbolicLink()) {
                warnIfLinkTargetIsOpen(networkDir);
            } else if (attributes.isDirectory() && isOfOwner(networkDir, attributes, requiredOwner)) {
                if (isPathStable) {
                    restrictTree(networkDir, attributes.owner());
                } else {
                    setPermissionsWithoutFollowingALink(networkDir, OWNER_ONLY);
                }
            }
        } catch (IOException e) {
            addFailure(networkDir, e);
        }
    }

    private void restrictTree(Path root, UserPrincipal owner) {
        Deque<Path> directories = new ArrayDeque<>(List.of(root));
        while (!directories.isEmpty()) {
            Path directory = directories.pop();
            try {
                Files.setPosixFilePermissions(directory, OWNER_ONLY);
            } catch (IOException e) {
                // Others may be able to replace its entries, so it is not entered
                addFailure(directory, e);
                continue;
            }
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
                for (Path entry : entries) {
                    if (isDirectoryToEnter(entry, owner, directory.equals(root))) {
                        directories.push(entry);
                    }
                }
            } catch (IOException | DirectoryIteratorException e) {
                log.warn("Could not make all directories below {} accessible to their owner only: {}", directory,
                        e.toString());
            }
        }
    }

    // A link to the wallet, for example, is worth a warning. Links further down, such as the one of a JSON export, may
    // be open on purpose.
    private boolean isDirectoryToEnter(Path path, UserPrincipal owner, boolean isEntryOfNetworkDir) {
        try {
            PosixFileAttributes attributes = readAttributes(path);
            if (attributes.isSymbolicLink() && isEntryOfNetworkDir) {
                warnIfLinkTargetIsOpen(path);
            }
            return attributes.isDirectory() && isOfOwner(path, attributes, owner);
        } catch (IOException e) {
            addFailure(path, e);
            return false;
        }
    }

    private boolean isOfOwner(Path path, PosixFileAttributes attributes, @Nullable UserPrincipal owner) {
        boolean isOfOwner = owner == null || attributes.owner().equals(owner);
        if (!isOfOwner) {
            entriesOfOtherUsers.add(path);
        }
        return isOfOwner;
    }

    private static void warnIfLinkTargetIsOpen(Path link) throws IOException {
        Path target = link.toRealPath();
        PosixFileAttributes attributes = readAttributes(target);
        if ((attributes.isDirectory() || attributes.isRegularFile()) && isOpen(attributes)) {
            log.warn("{} is a link to {}, whose mode Bisq does not change. It may be open to other users. To keep " +
                    "them out, run: chmod go= '{}'", link, target, target);
        }
    }

    private static boolean isOpen(PosixFileAttributes attributes) {
        return !OWNER_ONLY.containsAll(attributes.permissions());
    }

    private static PosixFileAttributes readAttributes(Path path) throws IOException {
        return Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    }

    private static void setPermissionsWithoutFollowingALink(Path path, Set<PosixFilePermission> permissions)
            throws IOException {
        Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS)
                .setPermissions(permissions);
    }

    private void addFailure(Path path, IOException e) {
        failures.add(path + " (" + e + ")");
    }
}
