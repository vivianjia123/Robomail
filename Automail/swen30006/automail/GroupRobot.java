
package automail;

import java.util.ArrayList;

/**
 * The Class GroupRobot.
 *
 * @author Group W13-5
 * @Description: GroupRobot is used to deliver the heavy mailItem.
 */
public class GroupRobot
{
    /** The arraylist that contain robots. */
    private ArrayList<Robot> robots;

    /** The mailItem that carried by the grouprobot. */
    private MailItem mailItem;

    /** The flag to determine if the adding robot is already in the list */
    private boolean isSameRobot;

    /**
     * Instantiates a GroupRobot.
     *
     * @param robots the arraylist that contain robots
     * @param mailItem the mailItem that need to be delivered
     */
    public GroupRobot(ArrayList<Robot> robots, MailItem mailItem)
    {
        this.robots = robots;
        this.mailItem = mailItem;
    }

    /**
     * Add robot into the arraylist if it is not in the list before.
     *
     * @param robot the robot that add to the arraylist
     */
    public void addRobot(Robot robot)
    {
        isSameRobot = false;
        for (Robot r : robots) {
            if (r.id == robot.id) {
                isSameRobot = true;
            }
        }
        if (!isSameRobot) {
            robots.add(robot);
        }

    }

    /**
     * Return the number of robots in the arraylist.
     */
    public int NumberOfRobots()
    {
        return robots.size();
    }

    /**
     * Get the arraylist of robot.
     */
    public ArrayList<Robot> getRobots()
    {
        return robots;
    }

    /**
     * Check if the robot is in the arraylist.
     * 
     * @param otherRobot the robot that compare to the robot in arraylist
     */
    public boolean foundRobot(Robot otherRobot)
    {
        for (Robot robot : robots) {
            if (otherRobot.id == robot.id) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the mailItem.
     */
    public MailItem getMailItem()
    {
        return mailItem;
    }

    /**
     * Reset the priority of mailItem for each robot in the arraylist.
     */
    public void carryPriority()
    {
        for (Robot r : robots) {
            r.carryPriority();
        }
    }

    /**
     * Return a string of each robot's id.
     */
    @Override
    public String toString()
    {
        String n = "";
        for (int i = 0; i < robots.size(); i++) {
            n += robots.get(i).getID();
            n += " ";
        }
        return n;
    }

}
