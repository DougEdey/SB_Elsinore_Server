package com.sb.elsinore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFile {

    private ZipOutputStream zos = null;
    private FileOutputStream fos = null;

    /**
     * Protected constructor, use {@link #ZipFile(String) String Constructor}.
     */
    protected ZipFile() {
    }

    /**
     * Create a new Zip file with the specified name.
     * @param filename The zip filename.
     * @throws FileNotFoundException If the file could not be created.
     */
    public ZipFile(final String filename) throws FileNotFoundException {
        fos = new FileOutputStream(filename);
        zos = new ZipOutputStream(fos);
    }

    /**
     * Add a file to the zipfile specified by this ZipFile object.
     * @param fileName The file to add to the archive.
     * @throws FileNotFoundException If the input file could not be found.
     * @throws IOException If the Zipfile hasn't been opened.
     */
    public final void addToZipFile(final String fileName)
            throws IOException {
        if (zos == null) {
            throw new IOException("Zip file has not been opened");
        }

        BrewServer.LOG.info("Writing '" + fileName + "' to zip file");

        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(file.getName());
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }

    /**
     * Close the archive.
     * @throws IOException If the archive couldn't be closed.
     */
    public final void closeZip() throws IOException {
        zos.close();
        fos.close();
    }
}
