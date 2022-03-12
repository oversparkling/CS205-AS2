import java.io.File; // Import the File class
import java.io.IOException; // Import the IOException class to handle errors
import java.io.FileWriter; // Import the FileWriter class

public class Assignment2 {

    static int slotsCommonPool, numberOfHotdogMakers, numberOfBurgerMakers, numberOfHotdogPackers,
            numberOfBurgerPackers;
    static volatile int hotdogOrders, burgerOrders;
    static volatile int hotdogId = 0;
    static volatile int burgerId = 0;
    static volatile int burgerPackerId = 0;
    static volatile int hotdogPackerId = 0;
    static volatile int burgerMakerId = 0;
    static volatile int hotdogMakerId = 0;
    static volatile int burgerCountPack = 0;
    static volatile int hotdogCountPack = 0;
    static volatile boolean oneHotDogWaiting = false;

    static void gowork(int n_seconds) {
        for (int i = 0; i < n_seconds; i++) {
            long n = 300000000;
            while (n > 0) {
                n--;
            }
        }
    }

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

    public static void main(String[] args) {

        hotdogOrders = Integer.parseInt(args[0]);
        burgerOrders = Integer.parseInt(args[1]);
        slotsCommonPool = Integer.parseInt(args[2]);
        numberOfHotdogMakers = Integer.parseInt(args[3]);
        numberOfBurgerMakers = Integer.parseInt(args[4]);
        numberOfHotdogPackers = Integer.parseInt(args[5]);
        numberOfBurgerPackers = Integer.parseInt(args[6]);
        log("hotdogs:" + hotdogOrders);
        log("burgers:" + burgerOrders);
        log("capacity:" + slotsCommonPool);
        log("burger makers:" + numberOfBurgerMakers);
        log("hotdog packers:" + numberOfBurgerMakers);

        Buffer buffer = new Buffer(slotsCommonPool);

        Runnable hotdogProducer = new Runnable() {
            @Override
            public void run() {
                Thread currentThread = Thread.currentThread();
                hotdogMakerId++;
                currentThread.setName("hm" + hotdogMakerId);
                // HotdogID denotes which id is being produced by the producer
                while (hotdogId < hotdogOrders) {
                    gowork(3);
                    Hotdog hotdog = new Hotdog(hotdogId, currentThread.getName());
                    hotdogId++;
                    buffer.put(hotdog);
                    log(currentThread.getName() + " puts hotdog id:" + hotdog.id);
                }

            }
        };
        Runnable burgerProducer = new Runnable() {
            @Override
            public void run() {
                Thread currentThread = Thread.currentThread();
                burgerMakerId++;
                currentThread.setName("bm" + burgerMakerId);
                // BurgerID denotes which id is being produced by the producer
                while (burgerId < burgerOrders) {
                    gowork(8);
                    Burger burger = new Burger(burgerId, currentThread.getName());
                    burgerId++;
                    buffer.put(burger);
                    log(currentThread.getName() + " puts burger id:" + burger.id);
                }

            }
        };

        Runnable hotdogPacker = new Runnable() {

            @Override
            public void run() {
                int currentHotdogs = 0;
                Thread currentThread = Thread.currentThread();
                hotdogPackerId++;
                currentThread.setName("hc" + hotdogPackerId);
                Food prevHotdog = null;
                while (hotdogCountPack < hotdogOrders) {
                    Food hotdog = buffer.getHotd(true, currentThread.getPriority());
                    while (hotdog == null) {
                        hotdog = buffer.getHotd(true, currentThread.getPriority());
                    }
                    currentHotdogs++;
                    if (currentHotdogs % 2 == 1) {
                        prevHotdog = hotdog;
                        currentThread.setPriority(10);
                    } else {
                        log(currentThread.getName() + " gets hotdogs id:" + prevHotdog.id + " from " + prevHotdog.origin
                                + " and id:" + hotdog.id + " from " + hotdog.origin);
                        currentThread.setPriority(5);
                    }
                    hotdogCountPack++;
                    gowork(2);
                }
                return;

            }
        };
        Runnable burgerPacker = new Runnable() {
            @Override
            public void run() {
                Thread currentThread = Thread.currentThread();
                burgerPackerId++;
                currentThread.setName("bc" + burgerPackerId);
                while (burgerCountPack < burgerOrders) {
                    Food burger = buffer.get(false);
                    while (burger == null) {
                        burger = buffer.get(false);
                    }
                    log(currentThread.getName() + " gets Burger id:" + burger);
                    burgerCountPack++;
                    gowork(2);
                }
                return;
            }
        };
        for (int i = 0; i < numberOfHotdogPackers; i++) {
            new Thread(hotdogPacker).start();
        }
        for (int i = 0; i < numberOfHotdogMakers; i++) {
            new Thread(hotdogProducer).start();
        }
        for (int i = 0; i < numberOfBurgerMakers; i++) {
            new Thread(burgerProducer).start();

        }
        for (int i = 0; i < numberOfBurgerPackers; i++) {
            new Thread(burgerPacker).start();
        }
        while (burgerCountPack != burgerOrders && hotdogCountPack != hotdogOrders) {

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

    @Override
    public String toString() {
        return "Burger [id=" + id + "]";
    }
}

class Hotdog extends Food {

    public Hotdog(int id, String origin) {
        this.id = id;
        this.origin = origin;
    }

    @Override
    public String toString() {
        return "Hotdog [id=" + id + "]";
    }

}

class Buffer {

    private Food[] buffer;
    private int front = 0, back = 0;
    public int item_count = 0;

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
        Assignment2.gowork(1);
        this.notifyAll();

    }

    public synchronized Food get(boolean isHotdog) {

        while (item_count == 0) {
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }

        Food food = buffer[front];
        if ((food instanceof Burger && !isHotdog)) {
            front = (front + 1) % buffer.length;
            item_count--;
            Assignment2.gowork(1);
            this.notifyAll();
            return food;
        } else {
            this.notifyAll();
            return null;
        }

    }

    public synchronized Food getHotd(boolean isHotdog, int priority) {

        while (item_count == 0) {
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }

        Food food = buffer[front];
        if ((food instanceof Hotdog && isHotdog)
                && ((priority == 10 && Assignment2.oneHotDogWaiting) || (!Assignment2.oneHotDogWaiting))) {
            front = (front + 1) % buffer.length;
            item_count--;
            Assignment2.gowork(1);
            Assignment2.oneHotDogWaiting = !Assignment2.oneHotDogWaiting;
            this.notifyAll();
            return food;
        } else {
            this.notifyAll();
            return null;
        }

    }

}
