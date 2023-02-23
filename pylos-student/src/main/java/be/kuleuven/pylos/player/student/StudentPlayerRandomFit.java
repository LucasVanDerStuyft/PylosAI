package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosGameIF;
import be.kuleuven.pylos.game.PylosLocation;
import be.kuleuven.pylos.game.PylosSphere;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Ine on 5/05/2015.
 */
public class StudentPlayerRandomFit extends PylosPlayer{

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
		/* add a reserve sphere to a feasible random location */
        PylosLocation[] allLocations = board.getLocations();
        ArrayList<PylosLocation> usableLocations = new ArrayList<PylosLocation>();
        for (PylosLocation l : allLocations){
            if(l.isUsable()){
                usableLocations.add(l);
            }
        }
        PylosSphere myReserveSphere = board.getReserve(this);
        Random random = new Random();
        int index = random.nextInt(usableLocations.size());
        PylosLocation location = usableLocations.get(index);
        game.moveSphere(myReserveSphere,location);
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
		/* removeSphere a random sphere */


    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
		/* always pass */
        game.pass();
    }
}
