import java.util.List;

/**
 * Class for the messages data itself
 * Messages are split into smaller parts that can be easily
 * constructed using this class.
 */
class Messages{
    /**
     * Tell EV3 to wait for motor to finish
     */
    static byte[] wait = new byte[]{(byte)0xAA, 0x00, 0x0F};
    /**
     * Message header
     */
    static byte[] header = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00};

    /**
     * Start moving motor at port @param index
     * @param index port to start rotating
     * @return message part for port @param index
     */
    static byte[] start(int index){
        return new byte[]{(byte)0xA6, 0x00, (byte)Math.pow(2, index - 1)};
    }

    /**
     * Request data from sensor at port @param portNum in mode @param mode
     * @param portNum port number to read data from
     * @param mode sensor mode that data relates to
     * @return message part for the EV3 brick
     */
    static byte[] sensorData(int portNum, int mode){
        return new byte[]
                {0x00, 0x00, 0x00, 0x04, 0x00, (byte)0x99, 0x1D, 0x00, (byte)(portNum-1), 0x00, (byte)mode, 0x01, 0x60};
    }

    /**
     * Convert sensor reply to Float
     * @param reply received message in bytes
     * @return Float if reply has data, else null
     */
    static Float convertSensorReply(byte[] reply){
        if (reply == null || reply.length != 7 || reply[0] != 0x00 || reply[1] != 0x00 || reply[2] != 0x02){
            return null;
        }
        return Float.intBitsToFloat(
                (reply[6] & 0xff) << 24 | (reply[5] & 0xff) << 16 | (reply[4] & 0xff) << 8 | reply[3] & 0xff
        );
    }

    /**
     * Create message bytes for moving
     * all motors simultaneously by given parameters
     *
     * @param motor1 motor angle at port 1
     * @param motor2 motor angle at port 2
     * @param motor3 motor angle at port 3
     * @param motor4 motor angle at port 4
     * @param speed motor movement speed
     * @return byte array that the EV3 brick can understand.
     */
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

    /**
     * Create message bytes for motor by given parameters
     *
     * @param index motor port
     * @param angle motor angle to move
     * @param maxAngle if all motors are moved at once, this is the biggest angle. 0 otherwise.
     * @param speed motor movement speed
     * @return byte array that the EV3 brick can understand.
     */
    static byte[] motorDataToBytes(int index, int angle, int maxAngle, int speed) {

        int relativeSpeed = maxAngle == 0 ? speed : Math.max(1, Math.round(Math.abs(speed * angle / maxAngle)));

        byte[] movement = motorMovement(index, angle, relativeSpeed);

        return concatArrays( new byte[][]{polarity(motorByte(index), angle), movement});
    }

    /**
     * Spin motor at given speed continuously
     * @param index motor port
     * @param speed rotation speed
     * @return byte array for the EV3 brick
     */
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

    /**
     * Make the EV3 brick play a tone
     * @param frequency tone frequency
     * @param volume tone volume
     * @param duration to duration in ms
     * @return byte array for the EV3 brick
     */
    static byte[] toneData(int frequency, int volume, int duration) {

        return concatArrays(new byte[][]{
                {0x00, 0x00, 0x00, 0x00, 0x00, (byte)0x94, 0x01},
                pack2b(volume),
                pack3b(frequency),
                pack3b(duration),
                new byte[]{(byte)150, }
        });
    }

    /**
     * Make a single byte array from a 2d array.
     * Notice the 2d array is NOT necessarily a matrix!
     * @param arrays to concat
     * @return a single byte array
     */
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

    /**
     * Get Message part that pertains to polarity of movement (forward/backwards)
     * @param motor port number
     * @param angle movement angle
     * @return message part
     */
    private static byte[] polarity(int motor, int angle){
        return new byte[]{(byte) 0xA7, 0x0, motorByte(motor), directionByte(angle),};
    }

    /**
     * Translate motor port @param index to byte
     * @param index to translate
     * @return motor port byte
     */
    private static byte motorByte(int index) {
        return (byte) Math.pow(2, index - 1);}

    /**
     * Translate motor @param angle to direction byte
     * @param angle to translate
     * @return motor direction byte
     */
    private static byte directionByte(int angle) {
        return (byte) (angle < 0 ? 0x63 : 0x01);}

    /**
     * Construct a message for moving a motor.
     * @param index motor port index to rotate
     * @param angle of motor movement
     * @param relativeSpeed speed of motor
     * @return message for EV3 brick
     */
    private static byte[] motorMovement(int index, int angle, int relativeSpeed){
        return concatArrays(new byte[][]{
                {(byte)0xAE, 0x00, motorByte(index)},
                pack2b(relativeSpeed),
                pack5b(0),
                pack5b(angle),
                pack5b(0),
                {0x01}
        });
    }

    /**
     * Pack @param value (1 byte) into 2 bytes.
     * @param value 1 byte to pack
     * @return packed bytes
     */
    private static byte[] pack2b(int value){
        return new byte[] {(byte)0x81, (byte)(value & 255)};
    }

    /**
     * Pack @param value (2 bytes) into 3 bytes
     * @param value 2 bytes to pack
     * @return packed bytes
     */
    private static byte[] pack3b(int value){
        return new byte[] {(byte)0x82, (byte)(value & 0xFF), (byte) ((value >> 8) & 0xFF)};
    }

    /**
     * Pack @param value (4 bytes) into 5 bytes
     * @param value 4 bytes to pack
     * @return packed bytes
     */
    private static byte[] pack5b(int value){
        return new byte[]
                {       (byte)0x83,
                        (byte)(value & 0xFF),
                        (byte)((value >> 8) & 0xFF),
                        (byte)((value >> 16) & 0XFF),
                        (byte)((value >> 24) & 0xFF)};
    }
}