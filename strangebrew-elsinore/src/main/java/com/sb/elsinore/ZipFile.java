package com.sb.elsinore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFile {

    private ZipOutputStream zos = null;
    private FileOutputStream fos = null;
    private Logger logger = LoggerFactory.getLogger(ZipFile.class);

    /**
     * Protected constructor, use {@link #ZipFile(String) String Constructor}.
     */
    protected ZipFile() {
    }

    /**
     * Create a new Zip file with the specified name.
     *
     * @param filename The zip filename.
     * @throws FileNotFoundException If the file could not be created.
     */
    public ZipFile(final String filename) throws FileNotFoundException {
        this.fos = new FileOutputStream(filename);
        this.zos = new ZipOutputStream(this.fos);
    }

    /**
     * Add a file to the zipfile specified by this ZipFile object.
     *
     * @param fileName The file to add to the archive.
     * @throws FileNotFoundException If the input file could not be found.
     * @throws IOException           If the Zipfile hasn't been opened.
     */
    public final void addToZipFile(final String fileName)
            throws IOException {
        if (this.zos == null) {
            throw new IOException("Zip file has not been opened");
        }

        this.logger.info("Writing '{}' to zip file", fileName);

        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(file.getName());
        this.zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            this.zos.write(bytes, 0, length);
        }

        this.zos.closeEntry();
        fis.close();
    }

    /**
     * Close the archive.
     *
     * @throws IOException If the archive couldn't be closed.
     */
    public final void closeZip() throws IOException {
        this.zos.close();
        this.fos.close();
    }
}
