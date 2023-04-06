package tileworld.agent;

import tileworld.Parameters;
import tileworld.environment.*;
import tileworld.exceptions.CellBlockedException;
import tileworld.planners.DefaultTWPlanner;

public class MyTWAgent extends TWAgent {
    private static final int MAX_TILES = 3;
    private static final int FUEL_TOLERANCE = Parameters.defaultFuelLevel - 10;
    private static final int FUEL_THRESHOLD = Parameters.defaultFuelLevel / 10;
    private static final int FUEL_DISTANCE_BOUND = 10;
    private final String name;
    private DefaultTWPlanner planner;
    public MyTWAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
        super(xpos, ypos, env, fuelLevel);
        this.name = name;
        this.planner = new DefaultTWPlanner(this);
    }

    @Override
    protected TWThought think() {
        init();
        // Check if agent is standing on an object, then take corresponding actions if possible
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
        // If fuel station has not been found, try to find fuel station.
        TWFuelStation fuelStation = TWAgentWorkingMemory.getFuelStation();
        if (fuelStation == null) {

        }
        // If agent runs out fuel, go to fuel station.
        if (fuelStation != null && (fuelLevel < FUEL_THRESHOLD || fuelLevel - getDistanceTo(fuelStation) < FUEL_DISTANCE_BOUND)) {
            planner.addGoal(fuelStation);
            planner.generatePlan();
            return new TWThought(TWAction.MOVE, planner.execute());
        }
        // Choose the closest object as target
        TWHole closestHole = (TWHole) memory.getClosestObjectInSensorRange(TWHole.class);
        TWTile closestTile = (TWTile) memory.getClosestObjectInSensorRange(TWTile.class);
        if (hasTile() && closestHole != null) {
            planner.addGoal(closestHole);
        } else if (closestTile != null && carriedTiles.size() < MAX_TILES) {
            planner.addGoal(closestTile);
        }

        if (planner.hasGoal()) {
            planner.generatePlan();
            return new TWThought(TWAction.MOVE, planner.execute());
        }
        //System.out.println("Simple Score: " + this.score);
        return new TWThought(TWAction.MOVE, getRandomDirection());
    }

    /**
     * Initialize the thinking environment
     */
    private void init() {
        sense();
        planner.clearGoals();
        planner.voidPlan();
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
