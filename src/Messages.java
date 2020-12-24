import java.util.List;

class Messages{
    static byte[] wait = new byte[]{(byte)0xAA, 0x00, 0x0F};

    static byte[] header = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00};

    static byte[] start(int index){
        return new byte[]{(byte)0xA6, 0x00, (byte)Math.pow(2, index - 1)};
    }

    static byte[] sensorData(int portNum, int mode){
        return new byte[]
                {0x00, 0x00, 0x00, 0x04, 0x00, (byte)0x99, 0x1D, 0x00, (byte)(portNum-1), 0x00, (byte)mode, 0x01, 0x60};
    }

    static Float convertSensorReply(byte[] reply){
        if (reply == null || reply.length != 7 || reply[0] != 0x00 || reply[1] != 0x00 || reply[2] != 0x02){
            return null;
        }
        return Float.intBitsToFloat(
                (reply[6] & 0xff) << 24 | (reply[5] & 0xff) << 16 | (reply[4] & 0xff) << 8 | reply[3] & 0xff
        );
    }

    static byte[] allMotorsDataToBytes(int motor1, int motor2, int motor3, int motor4, int speed){
        int maxAngle = List.of(motor1, motor2, motor3, motor4)
                .stream()
                .map(Math::abs)
                .reduce(-360, Math::max);

        if (maxAngle == 0){
            return null;
        }

        return concatArrays(new byte[][]{
                motorDataToBytes(1, motor1, maxAngle, speed),
                motorDataToBytes(2, motor2, maxAngle, speed),
                motorDataToBytes(3, motor3, maxAngle, speed),
                motorDataToBytes(4, motor4, maxAngle, speed),
        });
    }

    static byte[] motorDataToBytes(int index, int angle, int maxAngle, int speed) {

        int relativeSpeed = maxAngle == 0 ? speed : Math.max(1, Math.round(Math.abs(speed * angle / maxAngle)));

        byte[] movement = motorMovement(motorByte(index), angle, relativeSpeed);

        return concatArrays( new byte[][]{polarity(motorByte(index), angle), movement});
    }

    static byte[] spinMotor(int index, int speed){
        if (speed == 0){ // Stop motor logic
            return new byte[] {(byte)0xA3, 0, motorByte(index), 0x01};
        } else {
            return concatArrays(new byte[][]{
                    polarity(index, speed),
                    new byte[]{(byte)0xA5, 0x00, motorByte(index)},
                    pack2b(Math.abs(speed)),
                    new byte[]{(byte)0xA6, 0x00, motorByte(index)},
            });
        }
    }

    static byte[] toneData(int frequency, int volume, int duration) {

        return concatArrays(new byte[][]{
                {0x00, 0x00, 0x00, 0x00, 0x00, (byte)0x94, 0x01},
                pack2b(volume),
                pack3b(frequency),
                pack3b(duration),
                new byte[]{(byte)150, }
        });
    }

    static byte[] concatArrays(byte[][] arrays){
        int size = 0;
        for (byte[] byteArray: arrays) {
            size += byteArray.length;
        }

        byte[] newArray = new byte[size];

        int fillIndex = 0;
        for (byte[] byteArray: arrays) {
            System.arraycopy(byteArray, 0, newArray, fillIndex, byteArray.length);
            fillIndex += byteArray.length;

        }
        return newArray;
    }

    private static byte[] polarity(int motor, int angle){
        return new byte[]{(byte) 0xA7, 0x0, motorByte(motor), directionByte(angle),};
    }

    private static byte motorByte(int index) {
        return (byte) Math.pow(2, index - 1);}

    private static byte directionByte(int angle) {
        return (byte) (angle < 0 ? 0x63 : 0x01);}

    private static byte[] motorMovement(byte motorByte, int angle, int relativeSpeed){
        return concatArrays(new byte[][]{
                {(byte)0xAE, 0x00, motorByte},
                pack2b(relativeSpeed),
                pack5b(0),
                pack5b(angle),
                pack5b(0),
                {0x01}
        });
    }

    private static byte[] pack2b(int value){
        return new byte[] {(byte)0x81, (byte)(value & 255)};
    }

    //    Three-byte constant (LC2)
    private static byte[] pack3b(int value){
        return new byte[] {(byte)0x82, (byte)(value & 0xFF), (byte) ((value >> 8) & 0xFF)};
    }

    private static byte[] pack5b(int value){
        return new byte[]
                {       (byte)0x83,
                        (byte)(value & 0xFF),
                        (byte)((value >> 8) & 0xFF),
                        (byte)((value >> 16) & 0XFF),
                        (byte)((value >> 24) & 0xFF)};
    }
}