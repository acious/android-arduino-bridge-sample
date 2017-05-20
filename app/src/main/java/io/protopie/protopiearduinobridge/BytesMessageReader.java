package io.protopie.protopiearduinobridge;


import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read messages from bytes read.
 * In this example app, a message is defined as string that starts with '<' and ends with '>'
 * This is used for bytes read from the USB serial.
 */
public class BytesMessageReader {
    private State state = State.WAITING_COMMAND;
    private final StringBuffer readingCommands = new StringBuffer();

    /**
     * Called on data received and returns ProtoPie messages read.
     * @param data the byte array read from the USB serial
     * @param length the length of the bytes read
     * @return A list of messages. If no messages empty list. So you do not have to check null.
     */
    @NonNull
    public List<String> onData(byte[] data, int length) {
        List<String> rv = null;

        for (int i = 0; i < length; i++) {
            byte b = data[i];

            if (state == State.WAITING_COMMAND) {
                if (b == '<') {
                    state = State.READING_COMMAND;
                    readingCommands.delete(0, readingCommands.length());
                }
            } else if (state == State.READING_COMMAND) {
                if (b == '>') {
                    if (rv == null) {
                        rv = new ArrayList<>();
                    }
                    rv.add(readingCommands.toString());
                    readingCommands.delete(0, readingCommands.length());
                } else if (b == '<') {
                    readingCommands.delete(0, readingCommands.length());
                } else {
                    readingCommands.append(Character.valueOf((char) (b & 0xFF)));
                }
            }
        }

        if (rv == null) {
            return Collections.emptyList();
        } else {
            return rv;
        }
    }

    private enum State {
        WAITING_COMMAND, READING_COMMAND
    }
}
