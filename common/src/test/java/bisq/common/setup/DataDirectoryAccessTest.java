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

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;

import java.io.IOException;

import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DataDirectoryAccessTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(DataDirectoryAccess.class);
    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    private Path tmp;
    private Path appDataDir;
    private Path networkDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        logAppender.start();
        logger.addAppender(logAppender);
        tmp = tempDir;
        appDataDir = createDirectory(tmp.resolve("Bisq"));
        networkDir = createDirectory(appDataDir.resolve("btc_mainnet"));
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
    }

    // A data directory of an earlier version, created with the default umask
    @Test
    void theDataDirectoryAndEveryDirectoryOfTheNetworkBecomeOwnerOnly() throws IOException {
        Path walletDir = createDirectory(networkDir.resolve("wallet"));
        Path walletBackupDir = createDirectory(createDirectory(walletDir.resolve("backup")).resolve("backups_wallet"));
        Path blocksDir = createDirectory(createDirectory(networkDir.resolve("db")).resolve("BsqBlocks"));
        Path wallet = createFile(walletDir.resolve("bisq_BTC.wallet"), "rw-r--r--");

        restrict();

        for (Path directory : List.of(appDataDir, networkDir, walletDir, walletBackupDir.getParent(), walletBackupDir,
                blocksDir.getParent(), blocksDir)) {
            assertMode("rwx------", directory);
        }
        assertMode("rw-r--r--", wallet);
        assertNoWarnings();
    }

    // For example --appDataDir=.local, or an empty --appName
    @Test
    void whatBisqDoesNotUseKeepsItsMode() throws IOException {
        Path shared = createDirectory(createDirectory(appDataDir.resolve("share")).resolve("containers"));
        Path readOnly = createDirectory(appDataDir.resolve("readOnly"));
        setMode(readOnly, "r-xr-xr-x");
        Path otherNetworkDir = createDirectory(appDataDir.resolve("btc_regtest"));
        Path otherFile = createFile(appDataDir.resolve("notes.log"), "rw-r--r--");

        restrict();

        for (Path directory : List.of(shared.getParent(), shared, otherNetworkDir)) {
            assertMode("rwxr-xr-x", directory);
        }
        assertMode("r-xr-xr-x", readOnly);
        assertMode("rw-r--r--", otherFile);
        assertMode("rwx------", appDataDir);
        assertMode("rwx------", networkDir);
    }

    @Test
    void theConfigFileAndTheLogsLoseThePermissionsOfOthers() throws IOException {
        Path configFile = createFile(appDataDir.resolve("bisq.properties"), "r--r--r--");
        Path log = createFile(appDataDir.resolve("bisq.log"), "rw-r--r--");
        Path olderLog = createFile(appDataDir.resolve("bisq_12.log"), "rw-rw-r--");

        restrict();

        assertMode("r--------", configFile);
        assertMode("rw-------", log);
        assertMode("rw-------", olderLog);
    }

    // For example a data directory which the user moved to another drive
    @Test
    void aDataDirectoryWhichIsALinkIsFollowed() throws IOException {
        Path link = tmp.resolve("link");
        Path walletDir = createDirectory(networkDir.resolve("wallet"));
        createSymbolicLinkOrSkip(link, appDataDir);

        DataDirectoryAccess.restrictToOwner(link.toFile(), link.resolve("btc_mainnet").toFile());

        for (Path directory : List.of(appDataDir, networkDir, walletDir)) {
            assertMode("rwx------", directory);
        }
    }

    @Test
    void aLinkToTheWalletIsNotFollowedAndAnOpenTargetIsReported() throws IOException {
        Path target = createDirectory(tmp.resolve("drive"));
        Path inner = createDirectory(target.resolve("backup"));
        createSymbolicLinkOrSkip(networkDir.resolve("wallet"), target);

        restrict();

        assertMode("rwxr-xr-x", target);
        assertMode("rwxr-xr-x", inner);
        assertWarningContains("chmod go= '" + target.toRealPath() + "'");
    }

    @Test
    void aNetworkDirectoryWhichIsALinkIsNotFollowedAndAnOpenTargetIsReported() throws IOException {
        Path target = createDirectory(tmp.resolve("drive"));
        Path inner = createDirectory(target.resolve("wallet"));
        Files.delete(networkDir);
        createSymbolicLinkOrSkip(networkDir, target);

        restrict();

        assertMode("rwx------", appDataDir);
        assertMode("rwxr-xr-x", target);
        assertMode("rwxr-xr-x", inner);
        assertWarningContains("chmod go= '" + target.toRealPath() + "'");
    }

    @Test
    void aConfigFileWhichIsALinkIsNotFollowedAndAnOpenTargetIsReported() throws IOException {
        Path target = createFile(tmp.resolve("shared.properties"), "rw-r--r--");
        createSymbolicLinkOrSkip(appDataDir.resolve("bisq.properties"), target);

        restrict();

        assertMode("rw-r--r--", target);
        assertWarningContains("chmod go= '" + target.toRealPath() + "'");
    }

    @Test
    void aLinkToAPrivateTargetIsNotReported() throws IOException {
        Path target = createDirectory(tmp.resolve("drive"));
        setMode(target, "rwx------");
        createSymbolicLinkOrSkip(networkDir.resolve("wallet"), target);

        restrict();

        assertNoWarnings();
    }

    // For example the JSON files for a web server
    @Test
    void aLinkFurtherDownIsNotReported() throws IOException {
        Path target = createDirectory(tmp.resolve("www"));
        createSymbolicLinkOrSkip(createDirectory(networkDir.resolve("db")).resolve("json"), target);

        restrict();

        assertMode("rwxr-xr-x", target);
        assertNoWarnings();
    }

    // A directory which its owner cannot read, but others can enter
    @Test
    void aDirectoryWhichCannotBeReadBecomesOwnerOnlyWithItsSubtree() throws IOException {
        Path unreadable = createDirectory(networkDir.resolve("db"));
        Path inner = createDirectory(unreadable.resolve("backup"));
        setMode(unreadable, "-wx--x--x");
        // Root can read it
        assumeTrue(unreadable.toFile().listFiles() == null);

        restrict();

        assertMode("rwx------", unreadable);
        assertMode("rwx------", inner);
    }

    @Test
    void aDataDirectoryWhichCannotBeReadBecomesOwnerOnlyWithTheNetworkDirectory() throws IOException {
        Path walletDir = createDirectory(networkDir.resolve("wallet"));
        setMode(appDataDir, "-wx--x--x");
        assumeTrue(appDataDir.toFile().listFiles() == null);

        restrict();

        for (Path directory : List.of(appDataDir, networkDir, walletDir)) {
            assertMode("rwx------", directory);
        }
    }

    // A test cannot create files of another user, so root stands for the owner of the data directory
    @Test
    void whatBelongsToAnotherUserThanTheDataDirectoryKeepsItsMode() throws IOException {
        UserPrincipal otherUser = tmp.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName("root");
        assumeFalse(Files.getOwner(tmp).equals(otherUser));
        Path walletDir = createDirectory(networkDir.resolve("wallet"));
        Path configFile = createFile(appDataDir.resolve("bisq.properties"), "rw-r--r--");
        DataDirectoryAccess dataDirectoryAccess = new DataDirectoryAccess();

        dataDirectoryAccess.restrictNetworkDir(networkDir, otherUser, true);
        dataDirectoryAccess.restrictFile(configFile, otherUser);

        assertMode("rwxr-xr-x", networkDir);
        assertMode("rwxr-xr-x", walletDir);
        assertMode("rw-r--r--", configFile);
    }

    // The data directory belongs to another user, who can replace the directory of the network with a link at any time
    @Test
    void onlyTheNetworkDirectoryItselfBecomesOwnerOnlyIfOthersCanReplaceIt() throws IOException {
        Path walletDir = createDirectory(networkDir.resolve("wallet"));

        new DataDirectoryAccess().restrictNetworkDir(networkDir, null, false);

        assertMode("rwx------", networkDir);
        assertMode("rwxr-xr-x", walletDir);
    }

    @Test
    void aMissingDataDirectoryOrNetworkDirectoryIsOnlyLogged() throws IOException {
        Files.delete(networkDir);
        assertDoesNotThrow(this::restrict);
        assertMode("rwx------", appDataDir);

        Files.delete(appDataDir);
        assertDoesNotThrow(this::restrict);
    }

    private void restrict() {
        DataDirectoryAccess.restrictToOwner(appDataDir.toFile(), networkDir.toFile());
    }

    private void assertWarningContains(String text) {
        assertTrue(logAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains(text)),
                logAppender.list.toString());
    }

    private void assertNoWarnings() {
        assertEquals(List.of(), logAppender.list);
    }

    private static Path createDirectory(Path path) throws IOException {
        Files.createDirectory(path);
        setMode(path, "rwxr-xr-x");
        return path;
    }

    private static Path createFile(Path path, String mode) throws IOException {
        Files.createFile(path);
        setMode(path, mode);
        return path;
    }

    private static void setMode(Path path, String mode) throws IOException {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(mode));
    }

    private static void assertMode(String mode, Path path) throws IOException {
        assertEquals(PosixFilePermissions.fromString(mode), Files.getPosixFilePermissions(path), path.toString());
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "Symbolic links are not supported: " + e);
        }
    }
}
