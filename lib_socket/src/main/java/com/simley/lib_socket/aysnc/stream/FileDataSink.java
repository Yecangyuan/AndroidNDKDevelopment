package com.simley.lib_socket.aysnc.stream;


import com.simley.lib_socket.aysnc.AsyncServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Created by koush on 2/2/14.
 */
public class FileDataSink extends OutputStreamDataSink {
    File file;

    public FileDataSink(AsyncServer server, File file) {
        super(server);
        this.file = file;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        OutputStream ret = super.getOutputStream();
        if (ret == null) {
            file.getParentFile().mkdirs();
            ret = Files.newOutputStream(file.toPath());
            setOutputStream(ret);
        }
        return ret;
    }
}
