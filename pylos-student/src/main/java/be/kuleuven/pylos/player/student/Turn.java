package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;

public class Turn {
    TurnType type;
    PylosSphere sphere;
    PylosLocation endLocation;

    enum TurnType {
        ADD,
        MOVE,
        REMOVE_FIRST,
        REMOVE_SECOND,
        PASS
    }

    public Turn(TurnType type, PylosSphere sphere, PylosLocation endLocation) {
        this.type = type;
        this.sphere = sphere;
        this.endLocation = endLocation;
    }

    public void doTurnSimulate(PylosLocation endLocation, PylosGameSimulator simulator){
        switch(type){
            case ADD:
            case MOVE:
                simulator.moveSphere(sphere, endLocation); break;
            case REMOVE_FIRST:
            case REMOVE_SECOND:
                simulator.removeSphere(sphere);break;
            case PASS:simulator.pass();break;
        }
    }
}
