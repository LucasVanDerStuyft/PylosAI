package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StudentPlayerBestFit2 extends PylosPlayer{

    private static final int MAX_DEPTH = 5;
    private static final int INFINITY = 1000000;
    private PylosGameSimulator simulator;
    private PylosGameState prevState;
    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        simulator = new PylosGameSimulator(game.getState(),this.PLAYER_COLOR,board);

        int bestEval = -INFINITY;
        Move bestMove = null;
        int eval = 0;

        for (Move move : getLegalMoves(board, this)) {

            prevState = simulator.getState();

            if(move.sphere.canMoveTo(move.location) && simulator.getState() == PylosGameState.MOVE){

                boolean isReserve = move.sphere.isReserve();
                simulator.moveSphere(move.sphere, move.location);

                eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);

                if (isReserve) {
                    try{
                        simulator.undoAddSphere(move.sphere, prevState, move.sphere.PLAYER_COLOR);
                    } catch(AssertionError ex){
                        System.out.println(ex.toString());
                    }

                }
                else{
                    try{
                        simulator.undoMoveSphere(move.sphere, move.previousLocation, prevState, move.sphere.PLAYER_COLOR);
                    } catch (Exception ex){
                        String s = ex.toString();
                    }
                }
            }

            if (eval > bestEval) {
                bestEval = eval;
                bestMove = move;
            }
        }

        game.moveSphere(bestMove.sphere, bestMove.location);
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {

        //TODO: dit is momenteel gewoon overgenomen van de random player
        //idealiter doen we hier een alpha-beta minimax maar dan in 't voordeel van de zwarte speler MET als extra voorwaarde dat wij die bal kunnen wegnemen door
        //een zet te doen
        //we zoeken op die manier de bal die hij meest nodig heeft

        /*
        List<PylosSphere> allSpheres = Arrays.asList(board.getSpheres(this));
        List<PylosSphere> removableSpheres = new ArrayList<>(allSpheres);
        removableSpheres.removeIf(s -> s.isReserve() || s.getLocation().hasAbove());

        PylosSphere sphereToRemove;
        sphereToRemove = removableSpheres.size() == 1 ? removableSpheres.get(0) : removableSpheres.get(getRandom().nextInt(removableSpheres.size() - 1));
        game.removeSphere(sphereToRemove);
        */


        //INIT-FUNCTION
        //----------------------------------------------------------------------------
        simulator = new PylosGameSimulator(game.getState(),this.PLAYER_COLOR,board);
        int bestEval = -INFINITY;
        Move bestMove = null;
        PylosSphere bestSphere = null;
        int eval = 0;

        int counttries = 0;
        int countremove = 0;
        //----------------------------------------------------------------------------

        //DIT IS IN SE VERKEERD, we moeten hier niet op zoek gaan naar zetten, maar ENKEL naar ballen die we kunnen wegnemen...
        //for (Move move : getLegalMoves(board, this)) {


        //TODO: DIE ALPHA-BETA WERKT NU ALLEEN MAAR ALS WE IN "MOVE" STATE ZITTEN!!!!
        for(PylosSphere sphere: board.getSpheres(this.PLAYER_COLOR)) {

            prevState = simulator.getState();

            //if(sphere.canRemove() && simulator.getState() == PylosGameState.REMOVE_FIRST && simulator.getColor() == this.PLAYER_COLOR ){
            if(sphere.canRemove() && simulator.getColor() == this.PLAYER_COLOR ){
                PylosLocation prevLoc = sphere.getLocation();

                simulator.removeSphere(sphere);
                System.out.println("chosen sphere id = " + sphere.ID);

                eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);

                try{
                    //simulator.undoRemoveFirstSphere(sphere, prevLoc, prevState, sphere.PLAYER_COLOR);
                    simulator.undoRemoveFirstSphere(sphere, prevLoc, PylosGameState.REMOVE_FIRST, sphere.PLAYER_COLOR);
                } catch (Exception ex){
                    String s = ex.toString();
                }
                catch(AssertionError e){
                    System.out.println(e.toString());
                }


                if (eval > bestEval) {

                    bestEval = eval;
                    //bestMove = move;
                    bestSphere = sphere;
                    System.out.println("best sphere id = " + sphere.ID + " with eval: " + bestEval);
                }

            }

            //TODO: ZEER vreemd dat we die check van canRemove hier nog eens moeten doen...
            //if (eval > bestEval && sphere.canRemove()) {
            /*
            if (eval > bestEval) {

                bestEval = eval;
                //bestMove = move;
                bestSphere = sphere;
                System.out.println("best sphere id = " + sphere.ID);
            }

             */
        }

        try{
            game.removeSphere(bestSphere);
        }
        catch(AssertionError e){
            System.out.println(e.toString());
        }

    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {

        //TODO: gewoon overgenomen van random player
        //er bestaan strategy guides voor Pylos, we zouden daar eens moeten naar kijken.
        //heel wat hangt af van de openingszet
        game.pass();
    }

    private int alphaBeta(PylosGameIF game, PylosBoard board, int depth, int alpha, int beta, boolean maximizingPlayer) {

        prevState = simulator.getState();
        int eval = 0;
        boolean isReserve;

        if (depth == 0 || simulator.getState() == PylosGameState.COMPLETED) {
            return evaluatePosition(board);
        }

        if (maximizingPlayer) {
            int maxEval = -INFINITY;
            for (Move move : getLegalMoves(board, this.OTHER)) {

                switch(simulator.getState()){

                    case MOVE:
                        if(move.sphere.canMoveTo(move.location) && simulator.getColor() == this.PLAYER_COLOR.other()){

                            isReserve = move.sphere.isReserve();

                            simulator.moveSphere(move.sphere, move.location);

                            eval = alphaBeta(game, board, depth - 1, alpha, beta, false);

                            if (isReserve) simulator.undoAddSphere(move.sphere, prevState, move.sphere.PLAYER_COLOR.other());
                            else
                                try{
                                    simulator.undoMoveSphere(move.sphere, move.previousLocation, prevState, move.sphere.PLAYER_COLOR.other());
                                } catch (Exception ex){
                                    System.out.println(ex.toString());
                                }
                                catch(AssertionError e){
                                    System.out.println(e.toString());
                                }
                        }

                        break;

                    case REMOVE_FIRST:

                        if(move.sphere.canRemove() && simulator.getColor() == this.PLAYER_COLOR ){

                            simulator.removeSphere(move.sphere);

                            eval = alphaBeta(game, board, depth - 1, alpha, beta, false);

                            try{
                                simulator.undoRemoveFirstSphere(move.sphere, move.previousLocation, prevState, move.sphere.PLAYER_COLOR);
                            } catch (Exception ex){
                                String s = ex.toString();
                            }
                            catch(AssertionError e){
                                System.out.println(e.toString());
                            }
                        }

                    case REMOVE_SECOND:

                        if(move.sphere.canRemove() && simulator.getColor() == this.PLAYER_COLOR ){

                            simulator.removeSphere(move.sphere);

                            eval = alphaBeta(game, board, depth - 1, alpha, beta, false);

                            try{
                                simulator.undoRemoveSecondSphere(move.sphere, move.previousLocation, prevState, move.sphere.PLAYER_COLOR);
                            } catch (Exception ex){
                                String s = ex.toString();
                            }
                            catch(AssertionError e){
                                System.out.println(e.toString());
                            }
                        }


                        break;
                    default:
                        System.out.println("default reached");

                }

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) {
                    break;
                }
            }

            return maxEval;

        } else {

            int minEval = INFINITY;
            for (Move move : getLegalMoves(board, this)){

                switch(simulator.getState()){

                    case MOVE:
                        if(move.sphere.canMoveTo(move.location) && simulator.getColor() == this.PLAYER_COLOR){

                            isReserve = move.sphere.isReserve();

                            try{simulator.moveSphere(move.sphere, move.location);}
                            catch(Exception ex){String s = ex.toString();}
                            catch (AssertionError e) {String s = e.toString();}

                            eval = alphaBeta(game, board, depth - 1, alpha, beta, true);

                            if (isReserve ) {try{simulator.undoAddSphere(move.sphere, prevState, move.sphere.PLAYER_COLOR);} catch (AssertionError e) {String s = e.toString();}}
                            else simulator.undoMoveSphere(move.sphere, move.previousLocation, prevState, move.sphere.PLAYER_COLOR);
                        }
                        break;

                    case REMOVE_FIRST:

                        if(move.sphere.canRemove() && simulator.getColor() == this.PLAYER_COLOR ){

                            simulator.removeSphere(move.sphere);

                            eval = alphaBeta(game, board, depth - 1, alpha, beta, true);

                            try{
                                simulator.undoRemoveFirstSphere(move.sphere, move.previousLocation, prevState, move.sphere.PLAYER_COLOR);
                            } catch (Exception ex){
                                String s = ex.toString();
                            }
                            catch(AssertionError e){
                                System.out.println(e.toString());
                            }
                        }
                        break;

                    case REMOVE_SECOND:

                        if(move.sphere.canRemove() && simulator.getColor() == this.PLAYER_COLOR ){

                            simulator.removeSphere(move.sphere);

                            eval = alphaBeta(game, board, depth - 1, alpha, beta, true);

                            try{
                                simulator.undoRemoveSecondSphere(move.sphere, move.previousLocation, prevState, move.sphere.PLAYER_COLOR);
                            } catch (Exception ex){
                                String s = ex.toString();
                            }
                            catch(AssertionError e){
                                System.out.println(e.toString());
                            }
                        }


                        break;

                    default:
                        System.out.println("default reached");

                }

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) {
                    break;
                }
            }
            return minEval;
        }
    }

    private List<Move> getLegalMoves(PylosBoard board, PylosPlayer player){

        List<Move> allPossibleMoves = new ArrayList<>();

        List<PylosLocation> usableLocations = new ArrayList<>();
        List<PylosSphere> usableSpheres = new ArrayList<>();

        for(PylosLocation location: board.getLocations()){
            if(location == null){

                //hier opvangen dat elke "reserve location" eigenlijk gelijk is (niet allemaal alle ballen mee kruisen
                //to check if van toepassing
                System.out.println("NULL LOCATION");
            }


            if(location.isUsable()){
                for(PylosSphere sphere: board.getSpheres(player.PLAYER_COLOR)) {
                    if(sphere.canMoveTo(location)) {
                        if (!usableSpheres.contains(sphere)) usableSpheres.add(sphere);
                        if (!usableLocations.contains(location)) usableLocations.add(location);

                        MoveType type = null;
                        if(sphere.isReserve()){
                            type = MoveType.ADD;
                        }
                        else{
                            type = MoveType.MOVE;
                        }

                        if(sphere.PLAYER_COLOR == player.PLAYER_COLOR)
                        {
                            allPossibleMoves.add(
                                    new Move(sphere, location, sphere.getLocation(), type)
                            );
                        }

                    }
                }
            }
        }

        return allPossibleMoves;
    }

    private int evaluatePosition(PylosBoard board) {

        //momenteel een zeer simpele functie om te testen
        //we trekken het aantal zwarte spheres die nog op het bord liggen af van de witte
        //hoe minder witte spheres, hoe beter want dan moet zwart vroeger alles op 't bord leggen en winnen wij uiteindelijk
        //deze score moet natuurlijk uiteindelijk rekening houden met vierkanten die gemaakt kunnen worden en dergelijke
        return board.getSpheres(this).length - board.getSpheres(this.OTHER).length;
    }



}

class Move{

    public PylosSphere sphere;
    public PylosLocation location;
    public PylosLocation previousLocation;
    public MoveType type;

    public Move(PylosSphere s, PylosLocation l, PylosLocation pl, MoveType t){
        this.sphere = s;
        this.location = l;
        this.previousLocation = pl;    //location null is reserve, maar kun je evengoed checken met sphere.isReserve()
        this.type = t;
    }


}

enum MoveType {
    ADD,
    MOVE,
    REMOVE_FIRST,
    REMOVE_SECOND,
    PASS
}
