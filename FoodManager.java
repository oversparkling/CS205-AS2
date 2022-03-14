import java.io.IOException; // Import the IOException class to handle errors
import java.io.FileWriter; // Import the FileWriter class

public class FoodManager {

    static int slotsCommonPool, numberOfHotdogMakers, numberOfBurgerMakers, numberOfHotdogPackers,
            numberOfBurgerPackers;
    static volatile int hotdogOrders, burgerOrders;
    static volatile int hotdogId = 0;
    static volatile int burgerId = 0;
    // To set the thread names
    static volatile int burgerPackerId = 0;
    static volatile int hotdogPackerId = 0;
    static volatile int burgerMakerId = 0;
    static volatile int hotdogMakerId = 0;
    // To help determine if order has been fully fulfilled
    static volatile int burgerCountPack = 0;
    static volatile int hotdogCountPack = 0;
    // Boolean to track if there is a packer waiting for a hotdog
    static volatile boolean oneHotDogWaiting = false;
    static volatile int[] burgerMakerArray;
    static volatile int hotdogMakerArray[];
    static volatile int burgerPackerArray[];
    static volatile int hotdogPackerArray[];

    // Helper function for logging
    public static void log(String message) {
        try {
            FileWriter myWriter = new FileWriter("log.txt", true);
            myWriter.write(message + "\n");
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return;
    }

    // Helper function for logging
    public static int sum(int[] array) {
        int sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }

    public static void main(String[] args) throws InterruptedException {

        hotdogOrders = Integer.parseInt(args[0]);
        burgerOrders = Integer.parseInt(args[1]);
        slotsCommonPool = Integer.parseInt(args[2]);
        numberOfHotdogMakers = Integer.parseInt(args[3]);
        numberOfBurgerMakers = Integer.parseInt(args[4]);
        numberOfHotdogPackers = Integer.parseInt(args[5]);
        numberOfBurgerPackers = Integer.parseInt(args[6]);

        burgerMakerArray = new int[numberOfBurgerMakers];
        hotdogMakerArray = new int[numberOfHotdogMakers];
        burgerPackerArray = new int[numberOfBurgerPackers];
        hotdogPackerArray = new int[numberOfHotdogPackers];

        log("hotdogs:" + hotdogOrders);
        log("burgers:" + burgerOrders);
        log("capacity:" + slotsCommonPool);
        log("hotdog makers:" + numberOfHotdogMakers);
        log("burger makers:" + numberOfBurgerMakers);
        log("hotdog packers:" + numberOfHotdogPackers);
        log("burger packers:" + numberOfBurgerPackers);

        Buffer buffer = new Buffer(slotsCommonPool);

        Runnable hotdogProducer = new Runnable() {
            @Override
            public void run() {
                Thread currentThread = Thread.currentThread();
                int localId = ++hotdogMakerId;
                currentThread.setName("hm" + hotdogMakerId);
                // HotdogID denotes which id is being produced by the producer
                while (hotdogId < hotdogOrders) {
                    Hotdog hotdog = new Hotdog(++hotdogId, currentThread.getName());

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    buffer.put(hotdog);
                    log(currentThread.getName() + " puts hotdog id:" + hotdog.id);
                    hotdogMakerArray[localId - 1] += 1;
                }

            }
        };
        Runnable burgerProducer = new Runnable() {
            @Override
            public void run() {
                Thread currentThread = Thread.currentThread();
                int localId = ++burgerMakerId;
                currentThread.setName("bm" + burgerMakerId);
                // BurgerID denotes which id is being produced by the producer
                while (burgerId < burgerOrders) {
                    Burger burger = new Burger(++burgerId, currentThread.getName());
                    try {
                        Thread.sleep(8000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    buffer.put(burger);
                    log(currentThread.getName() + " puts burger id:" + burger.id);
                    burgerMakerArray[localId - 1] += 1;
                }

            }
        };

        Runnable hotdogPacker = new Runnable() {

            @Override
            public void run() {
                int currentHotdogs = 0;
                Thread currentThread = Thread.currentThread();
                int localId = ++hotdogPackerId;
                currentThread.setName("hc" + hotdogPackerId);

                // To store the previous hotdog for logging purposes
                Food prevHotdog = null;
                while (hotdogCountPack < hotdogOrders) {

                    Food hotdog = buffer.getHotd(currentThread.getPriority());
                    while (hotdog == null) {

                        // Get out of the infinite loop when done
                        if (FoodManager.hotdogCountPack == FoodManager.hotdogOrders) {
                            return;
                        }

                        // Checks if the head is a hotdog
                        hotdog = buffer.getHotd(currentThread.getPriority());
                    }
                    // Successfully got a hotdog
                    currentHotdogs++;

                    // No previous hotdog
                    if (currentHotdogs % 2 == 1) {
                        prevHotdog = hotdog;
                        // Priority is used to distinguish which packer can enter
                        currentThread.setPriority(10);
                    } else {
                        log(currentThread.getName() + " gets hotdogs id:" + prevHotdog.id + " from " + prevHotdog.origin
                                + " and id:" + hotdog.id + " from " + hotdog.origin);
                        currentThread.setPriority(5);
                    }
                    hotdogCountPack++;
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    hotdogPackerArray[localId - 1] += 1;
                }
                return;

            }
        };
        Runnable burgerPacker = new Runnable() {
            @Override
            public void run() {
                Thread currentThread = Thread.currentThread();
                int localId = ++burgerPackerId;
                currentThread.setName("bc" + burgerPackerId);
                while (burgerCountPack < burgerOrders) {

                    // Retrieve from the buffer
                    Food burger = buffer.getBurg();
                    while (burger == null) {

                        // Exit from the infinite loop
                        if (FoodManager.burgerCountPack == FoodManager.burgerOrders) {
                            return;
                        }
                        burger = buffer.getBurg();
                    }
                    log(currentThread.getName() + " gets burger id:" + burger.id);
                    burgerCountPack++;
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    burgerPackerArray[localId - 1] += 1;

                }
                return;
            }
        };
        for (int i = 0; i < numberOfHotdogPackers; i++) {
            Thread temp = new Thread(hotdogPacker);
            temp.start();
        }
        for (int i = 0; i < numberOfHotdogMakers; i++) {
            Thread temp = new Thread(hotdogProducer);
            temp.start();
        }
        for (int i = 0; i < numberOfBurgerMakers; i++) {
            Thread temp = new Thread(burgerProducer);
            temp.start();
        }
        for (int i = 0; i < numberOfBurgerPackers; i++) {
            Thread temp = new Thread(burgerPacker);
            temp.start();
        }

        // Only start summary when all orders fulfilled
        while (sum(hotdogMakerArray) != hotdogOrders || sum(hotdogPackerArray) != hotdogOrders
                || sum(burgerMakerArray) != burgerOrders || sum(burgerPackerArray) != burgerOrders) {
        }

        log("summary:");
        for (int i = 0; i < hotdogMakerArray.length; i++) {
            log("hm" + (i + 1) + " makes " + hotdogMakerArray[i]);
        }
        for (int i = 0; i < burgerMakerArray.length; i++) {
            log("bm" + (i + 1) + " makes " + burgerMakerArray[i]);
        }
        for (int i = 0; i < hotdogPackerArray.length; i++) {
            log("hc" + (i + 1) + " packs " + hotdogPackerArray[i]);
        }
        for (int i = 0; i < burgerPackerArray.length; i++) {
            log("bc" + (i + 1) + " packs " + burgerPackerArray[i]);
        }
        return;
    }
}

class Food {
    public int id;
    String origin;
}

class Burger extends Food {

    public Burger(int id, String origin) {
        this.id = id;
        this.origin = origin;
    }
}

class Hotdog extends Food {

    public Hotdog(int id, String origin) {
        this.id = id;
        this.origin = origin;
    }
}

class Buffer {

    private Food[] buffer;
    private int front = 0, back = 0;
    public int item_count = 0;
    public int hotdogPacked = 0;
    public int burgerPacked = 0;

    Buffer(int size) {
        buffer = new Food[size];
    }

    public synchronized void put(Food food) {
        while (item_count == buffer.length) {
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
        buffer[back] = food;
        back = (back + 1) % buffer.length;
        item_count++;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.notifyAll();
    }

    public synchronized Food getBurg() {

        while (item_count == 0 && (burgerPacked < FoodManager.burgerOrders)) {
            try {
                this.wait();
            } catch (InterruptedException e) {

            }
        }
        if (burgerPacked == FoodManager.burgerOrders) {
            this.notifyAll();
            return null;
        }
        Food food = buffer[front];
        if ((food instanceof Burger)) {
            front = (front + 1) % buffer.length;
            item_count--;
            burgerPacked++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.notifyAll();
            return food;
        } else {
            this.notifyAll();
            return null;
        }

    }

    public synchronized Food getHotd(int priority) {
        // Only wait if the order is not fully fulfilled and there is no item
        while (item_count == 0 && hotdogPacked < FoodManager.hotdogOrders) {
            try {
                this.wait();
            } catch (InterruptedException e) {

            }
        }
        // Exit if fulfilled
        if (hotdogPacked == FoodManager.hotdogOrders) {
            this.notifyAll();
            return null;
        }

        Food food = buffer[front];
        // If there is a packer waiting for one hotdog, check the priority. Only give if
        // the priority matches
        if ((food instanceof Hotdog)
                && ((priority == 10 && FoodManager.oneHotDogWaiting) || (!FoodManager.oneHotDogWaiting))) {
            front = (front + 1) % buffer.length;
            item_count--;
            hotdogPacked++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            FoodManager.oneHotDogWaiting = !FoodManager.oneHotDogWaiting;
            this.notifyAll();
            return food;
        } else {
            this.notifyAll();
            return null;
        }

    }

}
