/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package functionalTests.filetransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.proactive.api.PAFileTransfer;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.filetransfer.RemoteFile;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.utils.OperatingSystem;

import functionalTests.GCMFunctionalTest;
import functionalTests.TestDisabler;


/**
 * Tests the two main methods of the File Transfer API
 */

public class TestAPI extends GCMFunctionalTest {

    private static int FILE_SIZE = 16; //MB

    Node testnode;

    Node testnodePush;

    File dirTest = new File(System.getProperty("java.io.tmpdir"), "ProActive-TestAPI");

    @BeforeClass
    public static void beforeClass() {
        // The assertion Assert.assertTrue(rdirPushed.delete()) fails on windows because
        // the FileTransferAPI keeps a lock on the previously transferred file and the parent directory cannot be deleted
        TestDisabler.unsupportedOs(OperatingSystem.windows);
    }

    public TestAPI() throws ProActiveException {
        super(2, 1);
        super.startDeployment();
    }

    @Before
    public void initTest() throws Exception {
        cleanIfNecessary();
        Assert.assertFalse(dirTest.exists());

        testnode = super.getANode();
        testnodePush = super.getANode();

        Assert.assertTrue(dirTest.mkdirs());
        Assert.assertTrue(dirTest.exists());
        Assert.assertTrue(dirTest.isDirectory());
        Assert.assertTrue(dirTest.canWrite());
    }

    @Test
    public void theTest() throws Exception {
        //testPushPullFile();
        testPushPullDir();
    }

    public void testPushPullFile() throws Exception {
        File fileTest = new File(dirTest, "ProActiveTestFile.dat");
        File filePushed = new File(new File(dirTest, "b"), "ProActiveTestPushed.dat");
        File filePulled = new File(new File(dirTest, "c"), "ProActiveTestPulled.dat");
        File fileFuturePushed = new File(new File(dirTest, "d"), "ProActiveTestFuturePushed.dat");

        if (logger.isDebugEnabled()) {
            logger.debug("Creating " + FILE_SIZE + "MB random test file: " + fileTest);
        }

        //creates a new test file
        //Assert.assertTrue(SkeletonSystemImpl.checkWritableDirectory(dirTest));
        Assert.assertTrue(dirTest.exists());
        createRandomContentFile(fileTest, FILE_SIZE);

        RemoteFile rfilePushed = PAFileTransfer.push(fileTest, testnode, filePushed);
        rfilePushed.waitFor();
        Assert.assertTrue(rfilePushed.getRemoteFilePath().equals(filePushed)); //wait-by-necessity

        RemoteFile rfilePulled = PAFileTransfer.pull(testnode, filePushed, filePulled);

        RemoteFile pushedWhilePulling = rfilePulled.push(testnodePush, fileFuturePushed);

        Assert.assertTrue(rfilePulled.getRemoteFilePath().equals(filePulled)); //wait-by-necessity
        Assert.assertTrue(pushedWhilePulling.getRemoteFilePath().equals(fileFuturePushed)); //wait-by-necessity

        rfilePulled.waitFor();
        pushedWhilePulling.waitFor();

        long fileTestSum = checkSum(fileTest);
        long filePulledSum = checkSum(filePulled);
        long filePushedSum = checkSum(filePushed);
        long fileFuturePushedSum = checkSum(fileFuturePushed);

        if (logger.isDebugEnabled()) {
            logger.debug("CheckSum TestFile              =" + fileTestSum);
            logger.debug("CheckSum PushedFile            =" + filePushedSum);
            logger.debug("CheckSum PulledFile            =" + filePulledSum);
            logger.debug("CheckSum PushedFileWhilePulling=" + fileFuturePushedSum);
        }

        Assert.assertTrue(fileTestSum == filePushedSum);
        Assert.assertTrue(fileTestSum == filePulledSum);
        Assert.assertTrue(fileTestSum == fileFuturePushedSum);

        //Check remote file delete
        Assert.assertTrue(rfilePushed.exists());
        Assert.assertTrue(rfilePushed.isFile());
        Assert.assertFalse(rfilePushed.isDirectory());
        Assert.assertTrue(rfilePushed.delete());
        Assert.assertFalse(rfilePushed.exists());
    }

    public void testPushPullDir() throws Exception {
        String theFile = "file.dat";
        String empty = "empty";

        File dirTestSrc = new File(dirTest, "src");
        File dirTestSrcFile = new File(dirTestSrc, theFile);
        File dirTestSrcEmpty = new File(dirTestSrc, empty);

        File dirTestPushed = new File(dirTest, "pushed");
        File dirTestPushedFile = new File(dirTestPushed, theFile);
        File dirTestPushedEmpty = new File(dirTestPushed, empty);

        File dirTestPulled = new File(dirTest, "pulled");
        File dirTestPulledFile = new File(dirTestPulled, theFile);
        File dirTestPulledEmpty = new File(dirTestPulled, empty);

        Assert.assertTrue(dirTestSrc.mkdir());
        Assert.assertTrue(dirTestSrc.exists());
        Assert.assertTrue(dirTestSrc.isDirectory());
        Assert.assertTrue(dirTestSrc.canWrite());
        Assert.assertTrue(dirTestSrcEmpty.mkdir());
        Assert.assertTrue(dirTestSrcEmpty.exists());

        Assert.assertFalse(dirTestPushed.exists());
        Assert.assertFalse(dirTestPulled.exists());

        createRandomContentFile(dirTestSrcFile, FILE_SIZE);

        RemoteFile rdirPushed = PAFileTransfer.push(dirTestSrc, testnode, dirTestPushed);
        RemoteFile rdirPulled = PAFileTransfer.push(dirTestSrc, testnode, dirTestPulled);

        rdirPushed.waitFor();
        rdirPulled.waitFor();

        long fileTestSum = checkSum(dirTestSrcFile);

        //Check correct push
        Assert.assertTrue(dirTestPushed.exists());
        Assert.assertTrue(dirTestPushed.isDirectory());
        long filePushedSum = checkSum(dirTestPushedFile);

        Assert.assertTrue(fileTestSum == filePushedSum);
        Assert.assertTrue(dirTestPushedEmpty.exists());
        Assert.assertTrue(dirTestPushedEmpty.isDirectory());
        Assert.assertTrue(dirTestPushedEmpty.listFiles().length == 0);

        //Check correct pull
        Assert.assertTrue(dirTestPulled.exists());
        Assert.assertTrue(dirTestPulled.isDirectory());
        long filePulledSum = checkSum(dirTestPulledFile);

        Assert.assertTrue(fileTestSum == filePulledSum);
        Assert.assertTrue(dirTestPulledEmpty.exists());
        Assert.assertTrue(dirTestPulledEmpty.isDirectory());
        Assert.assertTrue(dirTestPulledEmpty.listFiles().length == 0);

        //Check delete directory
        Assert.assertTrue(rdirPushed.exists());
        Assert.assertTrue(rdirPushed.isDirectory());
        Assert.assertFalse(rdirPushed.isFile());
        Assert.assertTrue(rdirPushed.delete()); // fails on windows because of file lock
        Assert.assertFalse(rdirPushed.exists());
    }

    /**
     * Gets a checksum on the specified file
     * 
     * @param file
     *            The file to be check-summed.
     * @return
     * @throws IOException
     */
    static long checkSum(File file) throws IOException {
        // Compute Adler-32 checksum
        CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file.getAbsoluteFile()), new Adler32());
        byte[] tempBuf = new byte[1024 * 1024]; //1MB loops
        while (cis.read(tempBuf) >= 0)
            ;

        return cis.getChecksum().getValue();
    }

    /**
     * Creates a File with random content of specified MB size.
     * 
     * @param file
     *            The path of the File.
     * @param size
     *            The desired size of the file in MB.
     * @return
     * @throws IOException
     */
    static void createRandomContentFile(File file, int size) throws IOException {
        SecureRandom psrg = new SecureRandom();
        byte[] b = new byte[1024 * 1024]; //1 * MB
        psrg.nextBytes(b);

        FileOutputStream fos = new FileOutputStream(file);
        for (int i = 0; i < size; i++) { //size times
            fos.write(b, 0, b.length); //not really random, but good enough for this test
        }
        fos.flush();
        fos.close();
    }

    /**
     * Cleans test files
     * 
     */
    private void cleanIfNecessary() throws IOException {
        FileUtils.deleteDirectory(this.dirTest);
    }
}
