public class Main {

    public static void main(String[] args) {
	// write your code here

        EV3 ev3 = new EV3(args[0]);


//        ev3.tone(440, 50, 200);
//        ev3.tone(400, 45, 100);
//        ev3.rotateAll(0,90,180,270,100);
//        ev3.rotateAll(90,180,270, 360, 50);

//        ev3.spin(100, 0, 0, 0);
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        ev3.rotate(2, -180, 50);
        while (true){
            try {
                ev3.sensor(1, 2);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }

        ev3.disconnect();
    }
}
