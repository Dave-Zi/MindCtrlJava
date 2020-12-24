import com.fazecast.jSerialComm.SerialPort;
import java.time.LocalDateTime;
import java.util.logging.Logger;

enum Colors {
    NONE, BLACK, BLUE, GREEN, YELLOW, RED, WHITE, BROWN
}

class EV3 {

    private Logger logger = Logger.getLogger(EV3.class.getName());
    private SerialPort port;
    private int delay = 0;

    /**
     * getter for delay (ms)
     * @return delay value
     */
    public int getDelay(){return delay;}

    /**
     * Setter for delay value
     * @param delay new value
     */
    public void setDelay(int delay){this.delay = delay;}

    /**
     * Construct EV3 object and connect it to given port.
     * Throw Exception if connecting fails.
     * @param portName to connect to
     */
    EV3(String portName){
        logger.info("EV3 object initiated at - " + LocalDateTime.now().toString() );
        logger.config("Trying to connect to EV3 Brick...");

        for (SerialPort sp: SerialPort.getCommPorts()) {
            if (sp.getSystemPortName().equals(portName)){
                port = sp;
                logger.config("Connecting to: " + sp.getSystemPortName());
                break;
            }
        }
        if (port != null) {
            port.openPort();
            logger.info("Port open!");
        } else {
            logger.severe("No EV3 Brick Found!");
            throw new NullPointerException("Brick was not found!\n");}
    }

    /**
     * Close port
     */
    void disconnect(){
        stop();
        port.closePort();
        logger.info("Port closed!");
    }

    /**
     * Rotate all motors simultaneously.
     * Constructs a message to the EV3 brick that contains:
     *      - header
     *      - body
     *          - motor 1 body
     *          - motor 2 body
     *          - motor 3 body
     *          - motor 4 body
     *      - wait for finish
     *
     * Each motor body is a concatenation of polarity and movement.
     *
     * When finished, call delay.
     *
     * @param motor1 Angle of motor 1
     * @param motor2 Angle of motor 2
     * @param motor3 Angle of motor 3
     * @param motor4 Angle of motor 4
     * @param speed Movement speed
     */
    void rotate(int motor1, int motor2,int motor3,int motor4, int speed){

        byte[] body = Messages.allMotorsDataToBytes(motor1, motor2, motor3, motor4, speed);

        if (body == null){
            return;
        }

        send(Messages.concatArrays(new byte[][]{
                Messages.header,
                body,
                Messages.wait
        }));
        delay();
    }

    /**
     * Rotate a single motor.
     * Constructs a message to the EV3 brick that contains:
     *      - header
     *      - body
     *      - start
     *      - wait for finish
     *
     * Motor's body is a concatenation of polarity and movement.
     *
     * When finished, call delay.
     *
     * @param index Port index of motor to be moved
     * @param angle Movement angle
     * @param speed Movement speed
     */
    void rotate(int index, int angle, int speed){
        if (angle == 0){
            return;
        }

        byte[] body = Messages.motorDataToBytes(index, angle, 0, speed);

        send(Messages.concatArrays(new byte[][]{
                Messages.header,
                body,
                Messages.start(index),
                Messages.wait
        }));
        delay();
    }

    /**
     * Rotate motors with given speed continuously
     *
     * @param motor1 speed for port A
     * @param motor2 speed for port B
     * @param motor3 speed for port C
     * @param motor4 speed for port D
     */
    void spin(int motor1, int motor2, int motor3, int motor4){
        send(Messages.concatArrays(new byte[][]{
                Messages.header,
                Messages.spinMotor(1, motor1),
                Messages.spinMotor(2, motor2),
                Messages.spinMotor(3, motor3),
                Messages.spinMotor(4, motor4)}));
    }

    /**
     * Stop all motors at once.
     */
    void stop(){
        spin(0,0,0,0);
    }

    /**
     * Get value from sensor connected at port @param portNum
     * @param portNum port number
     * @return value if received any, if not return null
     */
    Float sensor(int portNum){

        byte[] reply = send(Messages.sensorData(portNum, 0));
        Float value = Messages.convertSensorReply(reply);
        logger.info("Port " + portNum + " - " + value);
        return value;
    }

    /**
     * Get value from sensor connected at port @param portNum.
     * Data will relate to the specified mode @param mode
     * @param portNum port number
     * @param mode sensor mode
     * @return value if received any, if not return null
     */
    Float sensor(int portNum, int mode){

        byte[] reply = send(Messages.sensorData(portNum, mode));
        Float value = Messages.convertSensorReply(reply);
        String logMessage = "Port " + portNum + " - " + value;
        if (value != null && mode == 2){
            value = (float)Math.round(value);
            logMessage = "Port " + portNum + " - " + Colors.values()[value.intValue()];
        }
        logger.info(logMessage);
        return value;
    }
    /**
     * Make EV3 brick beep
     * @param frequency sound frequency
     * @param volume sound volume
     * @param duration sound duration in ms
     */
    void tone(int frequency, int volume, int duration) {
        send(Messages.toneData(frequency, volume, duration));
    }

    /**
     * Send message to the EV3 brick
     * Message format is:
     *      - message data size (2 bytes)
     *      - message data
     *
     * The message format is little endian.
     * Therefore when constructing and prepending the message data size,
     * this has to be addressed.

     * @param message byte array to be sent.
     */
    private byte[] send(byte[] message){

        byte[] messageLength = {(byte)(message.length % 256), (byte)(message.length / 256)};  // LSB first
        byte[] fullMessage = Messages.concatArrays(new byte[][]{
                messageLength,
                message
        });

        byte[] fullReply = null;

        if (port.isOpen()){
            // send message
            port.writeBytes(fullMessage, fullMessage.length);

            // Get reply
            byte[] replyLengthBytes = new byte[2];
            port.readBytes(replyLengthBytes, 2);
            int replyLengthInt = replyLengthBytes[0] + replyLengthBytes[1] * 256;
            fullReply = new byte[replyLengthInt];
            port.readBytes(fullReply, replyLengthInt);
        }
        return fullReply;
    }

    /**
     * sleep for @delay milliseconds
     */
    private void delay(){
        if (delay > 0){
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}


