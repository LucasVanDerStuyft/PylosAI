package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;
import java.util.stream.Collectors;

import static be.kuleuven.pylos.game.PylosPlayerColor.*;

public class StudentPlayerBestFit extends PylosPlayer{

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        PylosGameSimulator simulator = new PylosGameSimulator(game.getState(),this.PLAYER_COLOR, board);

        //1) alle vrije bollen van een speler in een lijst

        List<PylosSphere> allSpheresOurPlayer = Arrays.asList(board.getSpheres(this));
        List<PylosSphere> lstAllFreeSpheres_Our_Player = allSpheresOurPlayer.stream().filter(s -> s.canMove()).collect(Collectors.toList());

        PylosPlayerColor enemycolor = null;
        switch(this.PLAYER_COLOR){
            case DARK:
                enemycolor = LIGHT;
                break;
            case LIGHT:
                enemycolor = DARK;
                break;
        }
        List<PylosSphere> allSpheresEnemy = Arrays.asList(board.getSpheres(enemycolor));
        List<PylosSphere> lstAllFreeSpheres_Enemy = allSpheresEnemy.stream().filter(s -> s.canMove()).collect(Collectors.toList());


        //2) lijst met toegelaten turns
        ArrayList<PylosLocation> allowedLocations = new ArrayList<>();
        for (PylosLocation pl : board.getLocations()) {
            if (pl.isUsable()) {
                allowedLocations.add(pl);
            }
        }
        Map<PylosSphere, List<Turn>> turnList_Our_Player= new HashMap<PylosSphere, List<Turn>>();
        Map<PylosSphere, List<Turn>> turnList_Enemy= new HashMap<PylosSphere, List<Turn>>();




        //3) simuleren + evaluatiefunctie
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {

    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {


    }
}
