/*
 * Copyright 2017 Axway Software
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axway.ats.core.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class SeekInputStream extends InputStream {
    protected static final int READ_CHUNK      = 4096;
    protected static final int DEFAULT_BUFSIZE = 16 * READ_CHUNK;

    protected InputStream      stream;

    protected int              size            = 0;
    protected int              pos             = 0;
    protected int              mark            = 0;

    protected byte[]           buffer;
    protected int              bufsize;

    /**
     * Default constructor. Precaches the contents of the given stream.
     */
    public SeekInputStream( InputStream s ) throws IOException {

        super();

        //setup stream & initial buffer
        this.stream = new BufferedInputStream( s );
        this.bufsize = DEFAULT_BUFSIZE;
        this.buffer = new byte[this.bufsize];

        //mark source stream if possible
        if( this.stream.markSupported() )
            this.stream.mark( Integer.MAX_VALUE );

        //precache contents
        int nr = this.stream.read( this.buffer, this.size, READ_CHUNK );
        while( 0 < nr ) {
            this.size += nr;
            if( this.size >= this.bufsize - READ_CHUNK ) {
                int newsize = 2 * this.bufsize;
                byte[] newbuf = new byte[newsize];
                System.arraycopy( this.buffer, 0, newbuf, 0, this.size );
                this.bufsize = newsize;
                this.buffer = newbuf;
            }
            nr = this.stream.read( this.buffer, this.size, READ_CHUNK );
        }

        //reset source stream if possible
        if( this.stream.markSupported() )
            this.stream.reset();
    }

    @Override
    public int available() {

        return this.size - this.pos;
    }

    @Override
    public void close() throws IOException {

        this.stream.close();
        this.stream = null;
    }

    public int position() {

        return this.pos;
    }

    @Override
    public void mark(
                      int readlimit ) {

        this.mark = this.pos;
    }

    @Override
    public boolean markSupported() {

        return true;
    }

    @Override
    public int read() {

        int delta = this.size - this.pos;
        if( 0 >= delta )
            return -1;
        else return this.buffer[this.pos++] & 0xFF;
    }

    @Override
    public int read(
                     byte[] b,
                     int off,
                     int len ) {

        int rem = this.size - this.pos;
        if( 0 >= rem )
            return -1;
        if( len > rem )
            len = rem;
        System.arraycopy( this.buffer, this.pos, b, off, len );
        this.pos += len;

        return len;
    }

    @Override
    public void reset() {

        this.seek( this.mark );
    }

    public int seek(
                     int pos ) {

        //validate argument
        if( ( 0 > pos ) || ( this.size < pos ) )
            return -1;

        //seek there
        this.pos = pos;
        return this.pos;
    }

    @Override
    public long skip(
                      long n ) {

        //accept only 
        if( 0 >= n )
            return 0;

        int pos = this.pos;
        n += pos;
        if( Integer.MAX_VALUE <= n )
            n = Integer.MAX_VALUE;
        return this.seek( ( int ) n ) - pos;
    }

}
