package com.interzero.TestServer.configuration;

/**
 * The roles a user of the API can have.
 */
public enum Role {

    /**
     * Can only read data.
     */
    READER,

    /**
     * Can only write data (create, update, delete).
     */
    WRITER,

    /**
     * Can read and write data.
     */
    ADMIN
}
