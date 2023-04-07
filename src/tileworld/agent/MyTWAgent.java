package tileworld.agent;

import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.environment.*;
import tileworld.exceptions.CellBlockedException;
import tileworld.planners.DefaultTWPlanner;

import java.util.LinkedList;
import java.util.Queue;

public class MyTWAgent extends TWAgent {
    private static final int MAX_TILES = 3;
    private static final int FUEL_TOLERANCE = Parameters.defaultFuelLevel - 10;
    private static final int FUEL_THRESHOLD = Parameters.defaultFuelLevel / 10;
    private static final int FUEL_DISTANCE_BOUND = 10;
    private static int AGENT_NUM;
    private static boolean[] IsOccupied;
    private final String name;
    private DefaultTWPlanner planner;
    private Region myRegion;
    public static void setAgentNum(int agentNum) {
        AGENT_NUM = agentNum;
        IsOccupied = new boolean[agentNum];
    }
    private class Region {
        Queue<Int2D> keyPoints;
        private Int2D currentGoal;
        public Region(int lowerBound, int upperBound, int sensorRange) {
            keyPoints = new LinkedList<>();
            int length = upperBound - lowerBound + 1;
            int x;
            int y1, y2;
            int distance = sensorRange * 2 + 1;
            int leftBound = sensorRange;
            int rightBound = Parameters.yDimension - sensorRange - 1;
            for (int i = 0; i < length / distance + 1; i++) {
                if ((i + 1) * distance > length) {
                    x = upperBound - sensorRange;
                } else {
                    x = lowerBound + i * distance + sensorRange;
                }
                if (i % 2 == 0) {
                    y1 = leftBound;
                    y2 = rightBound;
                } else {
                    y1 = rightBound;
                    y2 = leftBound;
                }
                keyPoints.add(new Int2D(x, y1));
                keyPoints.add(new Int2D(x, y2));
            }
            currentGoal = keyPoints.remove();
            keyPoints.add(currentGoal);
        }
        public Int2D getCurrentGoal() {
            return currentGoal;
        }
        public Int2D renewGoal() {
            currentGoal = keyPoints.poll();
            keyPoints.add(currentGoal);
            return currentGoal;
        }
    }
    public MyTWAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
        super(xpos, ypos, env, fuelLevel);
        this.name = name;
        this.planner = new DefaultTWPlanner(this);
        for (int i = 0; i < AGENT_NUM; i++) {
            if (!IsOccupied[i]) {
                setRegion(i);
                IsOccupied[i] = true;
                break;
            }
        }
    }
    public static void clearRegion() {
        for (int i = 0; i < AGENT_NUM; i++) {
            IsOccupied[i] = false;
        }
    }
    private void setRegion(int i) {
        int length = Parameters.xDimension / AGENT_NUM;
        this.myRegion = new Region(i * length, (i + 1) * length - 1, Parameters.defaultSensorRange);
    }
    private Int2D getGoal() {
        Int2D currentGoal = checkGoal(myRegion.getCurrentGoal());
        if (this.x == currentGoal.getX() && this.y == currentGoal.getY()) {
            currentGoal = myRegion.renewGoal();
        }
        currentGoal = checkGoal(currentGoal);
        return currentGoal;
    }
    private Int2D checkGoal(Int2D currentGoal) {
        int goalX = currentGoal.getX();
        int goalY = currentGoal.getY();
        boolean isBlocked = memory.isCellBlocked(goalX, goalY);
        while (isBlocked) {
            goalY = currentGoal.getY();
            if (goalY < Parameters.yDimension / 2) {
                goalY--;
            } else {
                goalY++;
            }
            currentGoal = new Int2D(goalX, goalY);
            isBlocked = memory.isCellBlocked(goalX, goalY);
        }
        return currentGoal;
    }
    @Override
    protected TWThought think() {
        // Check if agent is standing on an object, then take corresponding actions if possible.
        TWEntity localObject = getLocal();
        if (localObject != null) {
            // refuel
            if (localObject instanceof TWFuelStation && (getFuelLevel() < FUEL_TOLERANCE)) {
                return new TWThought(TWAction.REFUEL, TWDirection.Z);
            }
            // fill hole
            if (localObject instanceof TWHole && hasTile()) {
                return new TWThought(TWAction.PUTDOWN, TWDirection.Z);
            }
            // pick tile
            if (localObject instanceof TWTile && carriedTiles.size() < MAX_TILES) {
                return new TWThought(TWAction.PICKUP, TWDirection.Z);
            }
        }
        planner.clearGoals();
        planner.voidPlan();
        Int2D currentGoal = getGoal();
        // If fuel station has not been found, try to find fuel station.
        TWFuelStation fuelStation = TWAgentWorkingMemory.getFuelStation();
        if (fuelStation == null) {
            return goalToThought(currentGoal);
        }
        // If agent runs out fuel, go to fuel station.
        if (fuelStation != null && (fuelLevel < FUEL_THRESHOLD || fuelLevel - getDistanceTo(fuelStation) < FUEL_DISTANCE_BOUND)) {
            return goalToThought(fuelStation);
        }
        // Choose the closest object as target.
        TWHole closestHole = (TWHole) memory.getClosestObjectInSensorRange(TWHole.class);
        TWTile closestTile = (TWTile) memory.getClosestObjectInSensorRange(TWTile.class);
        if (hasTile() && closestHole != null) {
            return goalToThought(closestHole);
        } else if (closestTile != null && carriedTiles.size() < MAX_TILES) {
            return goalToThought(closestTile);
        }
        // Use the next key point as goal, instead of random walk.
        return goalToThought(currentGoal);
//        return new TWThought(TWAction.MOVE, getRandomDirection());
    }
    private TWThought goalToThought(Int2D pos) {
        planner.addGoal(pos);
        planner.generatePlan();
        return new TWThought(TWAction.MOVE, planner.execute());
    }
    private TWThought goalToThought(TWEntity entity) {
        return goalToThought(new Int2D(entity.getX(), entity.getY()));
    }
    @Override
    protected void act(TWThought thought) {
        //You can do:
        //move(thought.getDirection())
        //pickUpTile(Tile)
        //putTileInHole(Hole)
        //refuel()
        try {
            switch (thought.getAction()) {
                case MOVE:
                    this.move(thought.getDirection());
                    break;
                case PICKUP:
                    TWTile localTile = (TWTile) getLocal();
                    this.pickUpTile(localTile);
                    break;
                case PUTDOWN:
                    TWHole localHole = (TWHole) getLocal();
                    this.putTileInHole(localHole);
                    break;
                case REFUEL:
                    this.refuel();
                    break;
            }
        } catch (CellBlockedException ex) {
            // Cell is blocked, replan?
        }
    }
    private TWDirection getRandomDirection(){
        TWDirection randomDir = TWDirection.values()[this.getEnvironment().random.nextInt(5)];
        if(this.getX()>=this.getEnvironment().getxDimension() ){
            randomDir = TWDirection.W;
        }else if(this.getX()<=1 ){
            randomDir = TWDirection.E;
        }else if(this.getY()<=1 ){
            randomDir = TWDirection.S;
        }else if(this.getY()>=this.getEnvironment().getxDimension() ){
            randomDir = TWDirection.N;
        }
        return randomDir;
    }
    @Override
    public String getName() {
        return name;
    }
    /**
     * A help method to get the object at the agent's position
     * @return the object on the agent's position
     */
    private TWEntity getLocal() {
        return (TWEntity) this.memory.getMemoryGrid().get(this.x, this.y);
    }
}