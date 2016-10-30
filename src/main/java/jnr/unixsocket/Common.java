/*
 * Copyright (C) 2016 Fritz Elfert
 * 
 * This file is part of the JNR project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package jnr.unixsocket;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import jnr.constants.platform.ProtocolFamily;

import jnr.ffi.Platform;
import jnr.ffi.Platform.OS;
import jnr.ffi.byref.IntByReference;

/**
 * Helper class, providing common methods.
 */
final class Common {

    private static OS currentOS = Platform.getNativePlatform().getOS();

    private Common() {
    }

    static UnixSocketAddress bind(int fd, UnixSocketAddress local) throws IOException {
        SockAddrUnix sa;
        if (null == local) {
            // Support autobind
            sa = SockAddrUnix.create();
            sa.setFamily(ProtocolFamily.PF_UNIX);
            if (currentOS == OS.LINUX) {
                // On Linux, we simply set an empty path
                sa.setPath("");
            } else {
                // Emulate something similar (bind to some random unique address),
                // but use regular namespace
                File f = Files.createTempFile("jnr-unixsocket-tmp", ".sock").toFile();
                f.deleteOnExit();
                f.delete();
                sa.setPath(f.getPath());
            }
        } else {
            sa = local.getStruct();
        }
        if (Native.bind(fd, sa, sa.length()) < 0) {
            throw new IOException(Native.getLastErrorString());
        }
        return getsockname(fd);
    }

    static UnixSocketAddress getsockname(int sockfd) {
        UnixSocketAddress local = new UnixSocketAddress();
        SockAddrUnix addr = local.getStruct();
        int maxLength = addr.getMaximumLength();
		IntByReference len = new IntByReference(addr.getMaximumLength());

		if (Native.libc().getsockname(sockfd, addr, len) < 0) {
			throw new Error(Native.getLastErrorString());
		}
        addr.updatePath(len.getValue());
        return local;
    }

	static UnixSocketAddress getpeername(int sockfd) {
		UnixSocketAddress remote = new UnixSocketAddress();
        SockAddrUnix addr = remote.getStruct();
        int maxLength = addr.getMaximumLength();
		IntByReference len = new IntByReference(maxLength);

		if (Native.libc().getpeername(sockfd, addr, len) < 0) {
			throw new Error(Native.getLastErrorString());
		}
        addr.updatePath(len.getValue());
		return remote;
	}

}