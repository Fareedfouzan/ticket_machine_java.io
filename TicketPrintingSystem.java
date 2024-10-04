import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

interface ServiceTicketMachine {
    void printTicket();

    void refillPaper();

    void replaceToner();
}

class Ticket {
    // Implementation for Ticket class
    private UUID ticketId;
    private double price;

    public Ticket(double price) {
        this.ticketId = UUID.randomUUID();
        this.price = price;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return "Ticket ID: " + ticketId +
                "\nPrice: " + price;
    }
}

class TicketMachine implements ServiceTicketMachine {
    private Ticket ticket;
    private int paperLevel;
    private int tonerLevel;
    private Object monitor = new Object();
    Random random = new Random();

    private Lock lock = new ReentrantLock();

    public TicketMachine(int startingPaperLevel, int startingTonerLevel) {
        this.paperLevel = startingPaperLevel;
        this.tonerLevel = startingTonerLevel;
    }

    @Override
    public void printTicket() {
        synchronized (monitor) {
            try {
                while (paperLevel == 0 || tonerLevel == 0) {
                    monitor.wait();
                }

                ticket = new Ticket(random.nextDouble(5.0, 10.0));
                System.out.println(Thread.currentThread().getName());
                System.out.println("Ticket printed with the id: " + ticket.getTicketId());
                System.out.println("Ticket price: £" + ticket.getPrice());
                paperLevel--;
                tonerLevel--;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                monitor.notifyAll();
            }
        }
    }

    @Override
    public synchronized void refillPaper() {
        System.out.println("Paper refilled.");
        paperLevel = 10;
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    @Override
    public synchronized void replaceToner() {
        System.out.println("Toner replaced.");
        tonerLevel = 10;
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    public Lock getLock(){
        return lock;
    }

    public synchronized void printStatus() {
        System.out.println("Paper Level: " + paperLevel);
        System.out.println("Toner Level: " + tonerLevel);
    }
}

class Passenger implements Runnable {
    private TicketMachine ticketMachine;
    private int numberOfTicketsToPrint;

    public Passenger(TicketMachine _ticketMachine, int numberOfTicketsToPrint) {
        this.ticketMachine = _ticketMachine;
        this.numberOfTicketsToPrint = numberOfTicketsToPrint;
    }

    @Override
    public void run() {
        Random random = new Random();
        Lock lock = ticketMachine.getLock();
        lock.lock();
        try {
            for (int i = 0; i < numberOfTicketsToPrint; i++) {
                Thread.sleep(random.nextInt(3000));
                ticketMachine.printTicket();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}


class TicketPaperTechnician implements Runnable {

    private TicketMachine ticketMachine;

    public TicketPaperTechnician(TicketMachine _ticketMachine) {
        this.ticketMachine = _ticketMachine;
    }


    @Override
    public void run() {
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(random.nextInt(6000));
                ticketMachine.refillPaper();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class TicketTonerTechnician implements Runnable {
    private TicketMachine ticketMachine;

    public TicketTonerTechnician(TicketMachine _ticketMachine) {
        this.ticketMachine = _ticketMachine;
    }

    @Override
    public void run() {
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(random.nextInt(6000)); // Random sleep interval between 0 and 3000 milliseconds
                ticketMachine.replaceToner();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

public class TicketPrintingSystem {
    public static void main(String[] args) {
        TicketMachine ticketMachine = new TicketMachine(10, 10);

        Passenger passenger1 = new Passenger(ticketMachine, 3);
        Passenger passenger2 = new Passenger(ticketMachine, 4);
        Passenger passenger3 = new Passenger(ticketMachine, 2);
        Passenger passenger4 = new Passenger(ticketMachine, 1);

        TicketPaperTechnician paperTechnician = new TicketPaperTechnician(ticketMachine);
        TicketTonerTechnician tonerTechnician = new TicketTonerTechnician(ticketMachine);

        ThreadGroup passengerGroup = new ThreadGroup("PassengerGroup");
        ThreadGroup technicianGroup = new ThreadGroup("TechnicianGroup");

        Thread passengerThread1 = new Thread(passengerGroup, passenger1, "Passenger 1");
        Thread passengerThread2 = new Thread(passengerGroup, passenger2, "Passenger 2");
        Thread passengerThread3 = new Thread(passengerGroup, passenger3, "Passenger 3");
        Thread passengerThread4 = new Thread(passengerGroup, passenger4, "Passenger 4");
        Thread paperTechnicianThread = new Thread(technicianGroup, paperTechnician, "Paper Technician");
        Thread tonerTechnicianThread = new Thread(technicianGroup, tonerTechnician, "Toner Technician");

        passengerThread1.start();
        passengerThread2.start();
        passengerThread3.start();
        passengerThread4.start();
        paperTechnicianThread.start();
        tonerTechnicianThread.start();

        try {
            passengerThread1.join();
            passengerThread2.join();
            passengerThread3.join();
            passengerThread4.join();
            paperTechnicianThread.join();
            tonerTechnicianThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ticketMachine.printStatus();
    }
}
