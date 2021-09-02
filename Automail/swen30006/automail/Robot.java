package automail;

import java.util.Map;
import java.util.TreeMap;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import strategies.IMailPool;

/**
 * The Class Robot.
 *
 * @author Group W13-5
 * @Description: Robot is used to delivers mail.
 */
public class Robot
{

    /** The Constant maximum carring weight of the robot and group robots. */
    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;

    static public final int PAIR_MAX_WEIGHT = 2600;

    static public final int TRIPLE_MAX_WEIGHT = 3000;

    /** The Constant moving speed of the robot and group robots. */
    static public final float SINGLE_SPEED = 1.0f;

    static public final float GROUP_SPEED = 1.0f / 3;

    IMailDelivery delivery;

    protected final String id;

    /** Possible states the robot can be in */
    public enum RobotState
    {
        DELIVERING,
        WAITING,
        RETURNING
    }

    public RobotState current_state;

    private float current_floor;

    private int destination_floor;

    private float speed;

    private IMailPool mailPool;

    private boolean receivedDispatch;

    private MailItem deliveryItem = null;

    private MailItem tube = null;

    private int deliveryCounter;

    /** The Flag to determine if the item is delivered. */
    private boolean isDelivering = false;

    /** The Flag to determine if robot is in a group. */
    private boolean belongToGroup = false;

    /**
     * Initiates the robot's location at the start to be at the mailroom also set it to be waiting for mail.
     * 
     * @param behaviour governs selection of mail items for delivery and behaviour on priority arrivals
     * @param delivery governs the final delivery
     * @param mailPool is the source of mail items
     */
    public Robot(IMailDelivery delivery, IMailPool mailPool)
    {
        id = "R" + hashCode();
        // current_state = RobotState.WAITING;
        current_state = RobotState.RETURNING;
        current_floor = Building.MAILROOM_LOCATION;
        this.delivery = delivery;
        this.mailPool = mailPool;
        this.receivedDispatch = false;
        this.deliveryCounter = 0;
    }

    /**
     * The robot dispatch the item.
     */
    public void dispatch()
    {
        receivedDispatch = true;
    }

    /**
     * This is called on every time step
     * 
     * @throws ExcessiveDeliveryException if robot delivers more than the capacity of the tube without refilling
     */
    public void step() throws ExcessiveDeliveryException
    {
        switch (current_state) {
            /** This state is triggered when the robot is returning to the mailroom after a delivery */
            case RETURNING:
                /** If its current position is at the mailroom, then the robot should change state */
                if (current_floor <= Building.MAILROOM_LOCATION) {
                    if (tube != null) {
                        mailPool.addToPool(tube);
                        System.out.printf("T: %3d > old addToPool [%s]%n", Clock.Time(), tube.toString());
                        tube = null;
                    }
                    /** Tell the sorter the robot is ready */
                    mailPool.registerWaiting(this);
                    changeState(RobotState.WAITING);
                    current_floor = Building.MAILROOM_LOCATION;
                } else {
                    /** If the robot is not at the mailroom floor yet, then move towards it! */
                    moveTowards(Building.MAILROOM_LOCATION, SINGLE_SPEED);
                    break;
                }
            case WAITING:
                /** If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery */
                if (!isEmpty() && receivedDispatch) {
                    receivedDispatch = false;
                    deliveryCounter = 0; // reset delivery counter
                    setRoute();
                    changeState(RobotState.DELIVERING);
                }
                break;
            case DELIVERING:
                if (current_floor <= destination_floor && current_floor > (destination_floor - GROUP_SPEED)) {
                    // If already here drop off either way

                    /** Delivery complete, report this to the simulator! */

                    // ensures that only 1 mail item is delivered whether or not the robot is in a group or individually
                    if (isDelivering || (deliveryItem.getWeight() < INDIVIDUAL_MAX_WEIGHT)) {
                        delivery.deliver(deliveryItem);
                        isDelivering = false;
                    }

                    deliveryItem = null;
                    deliveryCounter++;
                    if (deliveryCounter > 2) { // Implies a simulation bug
                        throw new ExcessiveDeliveryException();
                    }
                    /** Check if want to return, i.e. if there is no item in the tube */
                    if (tube == null) {
                        changeState(RobotState.RETURNING);
                        removeFromGroup();
                    } else {
                        /** If there is another item, set the robot's route to the location to deliver the item */
                        deliveryItem = tube;
                        tube = null;
                        removeFromGroup();
                        setRoute();
                        changeState(RobotState.DELIVERING);
                    }
                } else {
                    /** The robot is not at the destination yet, move towards it! */
                    if (belongToGroup) {
                        speed = GROUP_SPEED;
                    } else {
                        speed = SINGLE_SPEED;
                    }
                    moveTowards(destination_floor, speed);
                }
                break;
        }
    }

    /**
     * Sets the route for the robot
     */
    private void setRoute()
    {
        /** Set the destination floor */
        destination_floor = deliveryItem.getDestFloor();
    }

    /**
     * Generic function that moves the robot towards the destination
     * 
     * @param destination the floor towards which the robot is moving
     * @param how fast the robot will travel
     */
    private void moveTowards(int destination, float speed)
    {
        if (current_floor < destination) {
            current_floor += speed;
        } else {
            current_floor -= speed;
        }
    }

    private String getIdTube()
    {
        return String.format("%s(%1d)", id, (tube == null ? 0 : 1));
    }

    /**
     * Prints out the change in state
     * 
     * @param nextState the state to which the robot is transitioning
     */
    private void changeState(RobotState nextState)
    {
        assert (!(deliveryItem == null && tube != null));
        if (current_state != nextState) {
            System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), current_state,
                nextState);
        }
        current_state = nextState;
        if (nextState == RobotState.DELIVERING) {
            System.out.printf("T: %3d > %7s-> [%s]%n", Clock.Time(), getIdTube(), deliveryItem.toString());
        }
    }

    public MailItem getTube()
    {
        return tube;
    }

    static private int count = 0;

    static private Map<Integer, Integer> hashMap = new TreeMap<Integer, Integer>();

    @Override
    public int hashCode()
    {
        Integer hash0 = super.hashCode();
        Integer hash = hashMap.get(hash0);
        if (hash == null) {
            hash = count++;
            hashMap.put(hash0, hash);
        }
        return hash;
    }

    /**
     * Return true if the robot doesn't carry anything in hand and tube.
     */
    public boolean isEmpty()
    {
        return (deliveryItem == null && tube == null);
    }

    /**
     * Add the mailItem to robot's hand.
     * 
     * @param mailItem the mailItem to deliver
     * @throws ItemTooHeavyException if the weight of mailItem exceed the maxmium weight that robot can carry
     */
    public void addToHand(MailItem mailItem) throws ItemTooHeavyException
    {
        assert (deliveryItem == null);
        deliveryItem = mailItem;
        if (deliveryItem.weight > TRIPLE_MAX_WEIGHT)
            throw new ItemTooHeavyException();
    }

    /**
     * Add the mailItem to robot's tube.
     * 
     * @param mailItem the mailItem to deliver
     * @throws ItemTooHeavyException if the weight of mailItem exceed the maxmium weight that robot can put in the tube
     */
    public void addToTube(MailItem mailItem) throws ItemTooHeavyException
    {
        assert (tube == null);
        tube = mailItem;
        if (tube.weight > INDIVIDUAL_MAX_WEIGHT)
            throw new ItemTooHeavyException();
    }

    public void setIsDelivering()
    {
        isDelivering = true;
    }

    public String getID()
    {
        return id;
    }

    public int getDestination()
    {
        return destination_floor;
    }

    public float getCurrentFloor()
    {
        return current_floor;
    }

    /**
     * Reset the priority of mailItem for the robot.
     */
    public void carryPriority()
    {
        deliveryItem = null;
        isDelivering = false;
    }

    /**
     * Add robot to the group.
     */
    public void addToGroup()
    {
        belongToGroup = true;
    }

    /**
     * Remove robot from the group.
     */
    public void removeFromGroup()
    {
        belongToGroup = false;
    }

}
