package com.example.todolist.update;

/** A failure of the update flow with a category the UI can translate into a message. */
public class UpdateException extends Exception {

    public enum Kind {
        /** No connection, timeout, broken stream. */
        NETWORK,
        /** GitHub refused because of the anonymous rate limit. */
        RATE_LIMIT,
        /** The server answered, but not with something usable. */
        BAD_RESPONSE,
        /** The release has no APK, or its URL is not an asset of this repository. */
        NO_APK,
        /** Not enough free storage. */
        NO_SPACE,
        /** The downloaded file does not match the checksum GitHub published. */
        CHECKSUM,
        /** The file is not a readable APK of this app. */
        INVALID_APK,
        /** The APK is not newer than the installed version. */
        NOT_NEWER,
        /** The APK is signed with a different key; Android would refuse to install it over this app. */
        SIGNATURE
    }

    public final Kind kind;

    public UpdateException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public UpdateException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }
}
