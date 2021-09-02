
package strategies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;

import automail.GroupRobot;
import automail.MailItem;
import automail.PriorityMailItem;
import automail.Robot;
import exceptions.ItemTooHeavyException;

/**
 * The Class MailPool.
 *
 * @author Group W13-5
 * @Description: MailPool contain the mailItems and robots. The robots are loaded to deliver each mailItem to its
 *               destination, until there is no mailItem in the mailpool.
 */
public class MailPool implements IMailPool
{

    private class Item
    {
        int priority;

        int destination;

        MailItem mailItem;
        // Use stable sort to keep arrival time relative positions

        public Item(MailItem mailItem)
        {
            priority = (mailItem instanceof PriorityMailItem) ? ((PriorityMailItem) mailItem).getPriorityLevel() : 1;
            destination = mailItem.getDestFloor();
            this.mailItem = mailItem;
        }
    }

    public class ItemComparator implements Comparator<Item>
    {
        @Override
        public int compare(Item i1, Item i2)
        {
            int order = 0;
            if (i1.priority < i2.priority) {
                order = 1;
            } else if (i1.priority > i2.priority) {
                order = -1;
            } else if (i1.destination < i2.destination) {
                order = 1;
            } else if (i1.destination > i2.destination) {
                order = -1;
            }
            return order;
        }
    }

    private LinkedList<Item> pool;

    private LinkedList<Robot> robots;

    private GroupRobot groupRobot;

    private ArrayList<Robot> availableRobots = null;

    public MailPool(int nrobots)
    {
        // Start empty
        pool = new LinkedList<Item>();
        robots = new LinkedList<Robot>();
    }

    @Override
    public void addToPool(MailItem mailItem)
    {
        Item item = new Item(mailItem);
        pool.add(item);
        pool.sort(new ItemComparator());
    }

    /**
     * {@inheritDoc} Load up any waiting robots with mailItems, add robots into the list.
     * 
     * @throws ItemTooHeavyException if the weight of mailItem exceed the maxmium weight that group robots can carry
     */
    @Override
    public void step() throws ItemTooHeavyException
    {
        try {
            ListIterator<Robot> i = robots.listIterator();
            // continues to load until there are no more free robots
            while (i.hasNext())
                loadRobot(i);
            while (availableRobots != null) {
                for (Robot r : availableRobots) {
                    registerWaiting(r);
                }
                availableRobots = null;

            }

        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Load robots to delivering mailItems that are in the mailpool.
     * 
     * @param Iterate each robot in the list
     * @throws ItemTooHeavyException if the weight of mailItem exceed the maxmium weight that group robots can carry
     */
    public void loadRobot(ListIterator<Robot> i) throws ItemTooHeavyException
    {
        Robot robot = i.next();
        assert (robot.isEmpty());

        // the current item that need to be deal with
        MailItem currentPackage;
        // the flag to check if the item can be delivered
        boolean itemDelivered = false;

        // System.out.printf("P: %3d%n", pool.size());
        ListIterator<Item> j = pool.listIterator();

        if (pool.size() > 0) {
            try {
                currentPackage = j.next().mailItem;

                // if the priority item exist, deal with it first
                dealPriority(currentPackage);

                // hand first as we want higher priority delivered first
                robot.addToHand(currentPackage);

                // if the weight of item is smaller than INDIVIDUAL_MAX_WEIGHT, robot just carry it
                if (currentPackage.getWeight() <= Robot.INDIVIDUAL_MAX_WEIGHT) {
                    j.remove();
                    itemDelivered = true;
                    // System.out.println("item " + currentPackage.getId() + ": carried by 1 robot " + robot.getID());

                } else if (currentPackage.getWeight() > Robot.INDIVIDUAL_MAX_WEIGHT
                    && currentPackage.getWeight() <= Robot.PAIR_MAX_WEIGHT) {
                    // if the item is too heavy and need additional robots to carry it
                    // add the robot to the group to carry it
                    addRobots(robot, currentPackage);
                    if (groupRobot.NumberOfRobots() == 2) {
                        j.remove();
                        itemDelivered = true;
                        // System.out.println(
                        // "item " + currentPackage.getId() + ": carried by 2 robot " + groupRobot.toString());
                    }
                } else if (currentPackage.getWeight() > Robot.PAIR_MAX_WEIGHT
                    && currentPackage.getWeight() <= Robot.TRIPLE_MAX_WEIGHT) {
                    // same as the above
                    addRobots(robot, currentPackage);
                    if (groupRobot.NumberOfRobots() == 3) {
                        j.remove();
                        itemDelivered = true;
                        // System.out.println(
                        // "item " + currentPackage.getId() + ": carried by 3 robot " + groupRobot.toString());
                    }
                } else {
                    throw new ItemTooHeavyException();
                }

                if (pool.size() > 0) {
                    // get the next new item for delivering
                    if (itemDelivered) {
                        currentPackage = j.next().mailItem;
                        // add new item to the tube if the robot can carry it individually
                        if (currentPackage.getWeight() < Robot.INDIVIDUAL_MAX_WEIGHT) {
                            robot.addToTube(currentPackage);
                            j.remove();
                            // System.out.println("put next item " + currentPackage.getId() + " in tube.");
                        }
                    }
                }

                // delivering mailItem
                deliveryItem(robot, itemDelivered);

                i.remove(); // remove from mailPool queue

            } catch (Exception e) {
                throw e;
            }
        }
    }

    /**
     * If the higher priority mailItem comes into the pool, and robots are in the group to carry a heavy item. Remove
     * the robots from the group and carry the new item
     *
     * @param m the mailItem that comes into the pool
     */
    public void dealPriority(MailItem m)
    {
        // if the priority item exist, deal with it first
        if ((groupRobot != null) && (m != groupRobot.getMailItem())) {
            groupRobot.carryPriority();
            availableRobots = groupRobot.getRobots();
            groupRobot = null;
        }

    }

    /**
     * Add robot into the group.
     *
     * @param r the robot that need to be added into a group
     * @param m the mailItem that comes into the pool
     */
    public void addRobots(Robot r, MailItem m)
    {
        // add robot into group if there is already a group
        if ((groupRobot != null) && !groupRobot.foundRobot(r)) {
            groupRobot.addRobot(r);
            r.addToGroup();
            // System.out.println("group robots include: " + groupRobot.toString() + " carry item " + m.getId());
        } else {
            // or create a new group robots to delivery the heavy item, add first robot into group
            r.setIsDelivering();
            groupRobot = new GroupRobot(new ArrayList<Robot>(Arrays.asList(r)), m);
            r.addToGroup();
            // System.out.println("group robots include: " + groupRobot.toString() + ", carry item " + m.getId()
            // + ", but need more robots.");
        }

    }

    /**
     * Delivering item by group robots or individual robot.
     *
     * @param robot the robot that need to deliver the item.
     * @param itemDelivered the flag to determine if the item can be delivered
     */
    public void deliveryItem(Robot robot, boolean itemDelivered)
    {
        // if enough robots to delivery heavy item, start delivering
        if ((groupRobot != null) && itemDelivered) {
            for (Robot r : groupRobot.getRobots()) {
                groupRobot = null;
                r.dispatch();
            }
            // or if one robot is required to delivery the light item, start delivering
        } else if (groupRobot == null && itemDelivered) {
            // send the robot off if it has any items to deliver
            robot.dispatch();
        }

    }

    @Override
    public void registerWaiting(Robot robot)
    { // assumes won't be there already
        robots.add(robot);
    }

}
