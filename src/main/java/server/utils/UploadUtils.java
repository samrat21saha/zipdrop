package server.utils;

import java.util.Random;

public class UploadUtils {
    public  static int generateCode(){ // generates invite code recieve file which is actually an unreserved/available port

        int DYNAMIC_STARTING_PORT = 49152;
        int DYNAMIC_ENDING_PORT = 65535;

        Random random = new Random();
        return random.nextInt((DYNAMIC_ENDING_PORT-DYNAMIC_STARTING_PORT)+DYNAMIC_STARTING_PORT); // to limit the random port number within starting and ending range
    }
}
