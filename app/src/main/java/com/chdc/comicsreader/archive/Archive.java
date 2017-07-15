package com.chdc.comicsreader.archive;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by Wen on 2017/7/15.
 */

public interface Archive extends Closeable {

    boolean extractFile(FileHeader fileHeader, OutputStream outputStream);

    List<FileHeader> listFiles();

    void close() throws IOException;
}
