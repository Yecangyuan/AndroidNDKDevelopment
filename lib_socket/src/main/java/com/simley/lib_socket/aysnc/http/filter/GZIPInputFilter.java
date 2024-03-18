package com.simley.lib_socket.aysnc.http.filter;


import com.simley.lib_socket.aysnc.ByteBufferList;
import com.simley.lib_socket.aysnc.DataEmitter;
import com.simley.lib_socket.aysnc.PushParser;
import com.simley.lib_socket.aysnc.callback.DataCallback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

public class GZIPInputFilter extends InflaterInputFilter {
    static short peekShort(byte[] src, int offset, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            return (short) ((src[offset] << 8) | (src[offset + 1] & 0xff));
        } else {
            return (short) ((src[offset + 1] << 8) | (src[offset] & 0xff));
        }
    }

    private static final int FCOMMENT = 16;

    private static final int FEXTRA = 4;

    private static final int FHCRC = 2;

    private static final int FNAME = 8;


    public GZIPInputFilter() {
        super(new Inflater(true));
    }

    boolean mNeedsHeader = true;
    protected CRC32 crc = new CRC32();

    public static int unsignedToBytes(byte b) {
        return b & 0xFF;
    }

    @Override
    @SuppressWarnings("unused")
    public void onDataAvailable(final DataEmitter emitter, ByteBufferList bb) {
        if (mNeedsHeader) {
            final PushParser parser = new PushParser(emitter);
            parser.readByteArray(10, new PushParser.ParseCallback<byte[]>() {
                int flags;
                boolean hcrc;

                public void parsed(byte[] header) {
                    short magic = peekShort(header, 0, ByteOrder.LITTLE_ENDIAN);
                    if (magic != (short) GZIPInputStream.GZIP_MAGIC) {
                        report(new IOException(String.format(Locale.ENGLISH, "unknown format (magic number %x)", magic)));
                        emitter.setDataCallback(new DataCallback.NullDataCallback());
                        return;
                    }
                    flags = header[3];
                    hcrc = (flags & FHCRC) != 0;
                    if (hcrc) {
                        crc.update(header, 0, header.length);
                    }
                    if ((flags & FEXTRA) != 0) {

                        parser.readByteArray(2, header1 -> {
                            if (hcrc) {
                                crc.update(header1, 0, 2);
                            }
                            int length = peekShort(header1, 0, ByteOrder.LITTLE_ENDIAN) & 0xffff;

                            parser.readByteArray(length, buf -> {
                                if (hcrc) {
                                    crc.update(buf, 0, buf.length);
                                }
                                next();
                            });
                        });
                    } else {
                        next();
                    }
                }

                private void next() {
                    PushParser parser = new PushParser(emitter);
                    DataCallback summer = (emitter1, bb1) -> {
                        if (hcrc) {
                            while (bb1.size() > 0) {
                                ByteBuffer b = bb1.remove();
                                crc.update(b.array(), b.arrayOffset() + b.position(), b.remaining());
                                ByteBufferList.reclaim(b);
                            }
                        }
                        bb1.recycle();
                        done();
                    };
                    if ((flags & FNAME) != 0) {
                        parser.until((byte) 0, summer);
                        return;
                    }
                    if ((flags & FCOMMENT) != 0) {
                        parser.until((byte) 0, summer);
                        return;
                    }

                    done();
                }

                private void done() {
                    if (hcrc) {
                        parser.readByteArray(2, header -> {
                            short crc16 = peekShort(header, 0, ByteOrder.LITTLE_ENDIAN);
                            if ((short) crc.getValue() != crc16) {
                                report(new IOException("CRC mismatch"));
                                return;
                            }
                            crc.reset();
                            mNeedsHeader = false;
                            setDataEmitter(emitter);
//                            emitter.setDataCallback(GZIPInputFilter.this);
                        });
                    } else {
                        mNeedsHeader = false;
                        setDataEmitter(emitter);
                    }
                }
            });
        } else {
            super.onDataAvailable(emitter, bb);
        }
    }
}
