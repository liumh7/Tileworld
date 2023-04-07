/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tileworld.planners;

import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.agent.TWAgent;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEntity;

import java.util.ArrayList;

/**
 * DefaultTWPlanner
 *
 * @author michaellees
 * Created: Apr 22, 2010
 *
 * Copyright michaellees 2010
 *
 * Here is the skeleton for your planner. Below are some points you may want to
 * consider.
 *
 * Description: This is a simple implementation of a Tileworld planner. A plan
 * consists of a series of directions for the agent to follow. Plans are made,
 * but then the environment changes, so new plans may be needed
 *
 * As an example, your planner could have 4 distinct behaviors:
 *
 * 1. Generate a random walk to locate a Tile (this is triggered when there is
 * no Tile observed in the agents memory
 *
 * 2. Generate a plan to a specified Tile (one which is nearby preferably,
 * nearby is defined by threshold - @see TWEntity)
 *
 * 3. Generate a random walk to locate a Hole (this is triggered when the agent
 * has (is carrying) a tile but doesn't have a hole in memory)
 *
 * 4. Generate a plan to a specified hole (triggered when agent has a tile,
 * looks for a hole in memory which is nearby)
 *
 * The default path generator might use an implementation of A* for each of the behaviors
 *
 */
public class DefaultTWPlanner implements TWPlanner {
    private ArrayList<Int2D> goals;
    private TWPath plan;
    private TWAgent agent;
    private AstarPathGenerator pathGenerator;
    public DefaultTWPlanner(TWAgent agent) {
        this.agent = agent;
        this.plan = null;
        this.goals = new ArrayList<Int2D>(4);
        int maxSearchDepth = Parameters.xDimension + Parameters.yDimension;
        this.pathGenerator = new AstarPathGenerator(agent.getEnvironment(), agent, maxSearchDepth);
    }
    public void addGoal(TWEntity object) {
        addGoal(object.getX(), object.getY());
    }
    public void addGoal(Int2D int2D) {
        goals.add(int2D);
    }
    public void addGoal(int x, int y) {
        goals.add(new Int2D(x, y));
    }
    public void clearGoals() {
        goals = new ArrayList<Int2D>(4);
    }
    public boolean hasGoal() {
        return !goals.isEmpty();
    }
    @Override
    public TWPath generatePlan() {
        plan = pathGenerator.findPath(agent.getX(), agent.getY(), goals.get(0).x, goals.get(0).y);
        return plan;
    }
    @Override
    public boolean hasPlan() {
        return plan != null && plan.hasNext();
    }
    @Override
    public void voidPlan() {
        plan = null;
    }
    @Override
    public Int2D getCurrentGoal() {
        return goals.isEmpty() ? null : goals.get(0);
    }
    @Override
    public TWDirection execute() {
        TWPathStep nextStep = plan.popNext();
        return nextStep.getDirection();
    }

}

