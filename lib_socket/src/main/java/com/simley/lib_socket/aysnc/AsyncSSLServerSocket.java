package com.simley.lib_socket.aysnc;

import java.security.PrivateKey;
import java.security.cert.Certificate;

public interface AsyncSSLServerSocket extends AsyncServerSocket {
    PrivateKey getPrivateKey();
    Certificate getCertificate();
}
