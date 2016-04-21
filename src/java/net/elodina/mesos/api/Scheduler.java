package net.elodina.mesos.api;

import java.util.List;

public abstract class Scheduler {
    public abstract void registered(Driver driver, String id, Master master);

    public abstract void reregistered(Driver driver, Master master);

    public abstract void offers(List<Offer> offers);

    public abstract void status(Task.Status status);

    public abstract void message(String executorId, String slaveId, byte[] data);

    public abstract void disconnected();

    public abstract class Driver {
        public abstract void declineOffer(String id);

        public abstract void launchTask(String offerId, Task task);

        public abstract void reconcileTasks(List<String> ids);

        public abstract void killTask(String id);
    }
}
