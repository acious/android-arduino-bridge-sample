package io.protopie.protopiearduinobridge;


import android.content.Context;
import android.content.Intent;

public class ProtoPieUtils {
    public static final String PROTOPIE_RECEIVE_ACTION = "io.protopie.action.ONE_TIME_RESPONSE";
    public static final String PROTOPIE_SEND_ACTION = "io.protopie.action.ONE_TIME_TRIGGER";

    public static void sendToProtoPie(Context context, String messageId) {
        Intent intent = new Intent(PROTOPIE_SEND_ACTION);
        intent.putExtra("messageId", messageId);
        context.sendBroadcast(intent);
    }
}
