package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class StudentPlayerBestFit3 extends PylosPlayer{

    private static final int MAX_DEPTH = 2;
    private static final int INFINITY = 1000000;
    private PylosGameSimulator simulator;
    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        simulator = new PylosGameSimulator(game.getState(),this.PLAYER_COLOR,board);

        int bestEval = -INFINITY;
        Move bestMove = null;
        int eval = 0;

        //try to move "normal/non-reserve" spheres
        for (Move move : getLegalMoves(board, this).stream().filter(s -> !s.sphere.isReserve()).collect(Collectors.toList())) {

            simulator.moveSphere(move.sphere, move.location);
            eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);
            simulator.undoMoveSphere(move.sphere, move.previousLocation, PylosGameState.MOVE, move.sphere.PLAYER_COLOR);

            if (eval > bestEval) {
                bestEval = eval;
                bestMove = move;
            }
        }

        //try to move reserve spheres
        for (Move move : getLegalMoves(board, this).stream().filter(s -> s.sphere.isReserve()).collect(Collectors.toList())) {

            simulator.moveSphere(move.sphere, move.location);
            eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);
            simulator.undoAddSphere(move.sphere, PylosGameState.MOVE, move.sphere.PLAYER_COLOR);

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

        simulator = new PylosGameSimulator(game.getState(),this.PLAYER_COLOR,board);

        int bestEval = -INFINITY;
        //Move bestMove = null;
        PylosSphere bestSphere = null;
        int eval = 0;

        for(PylosSphere sphere : Arrays.asList(board.getSpheres(this))){

            if(sphere.canRemove()){

                PylosLocation ploc = sphere.getLocation();
                simulator.removeSphere(sphere);
                eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);
                simulator.undoRemoveFirstSphere(sphere, ploc, PylosGameState.REMOVE_FIRST, this.PLAYER_COLOR);

                if (eval > bestEval) {
                    bestEval = eval;
                    //bestMove = move;
                    bestSphere = sphere;
                }
            }
        }

        game.removeSphere(bestSphere);
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {

        //Zowel RemoveSecond als Pass proberen en evalueren, beste van de 2 kiezen
        //game.pass();

        simulator = new PylosGameSimulator(game.getState(),this.PLAYER_COLOR,board);

        //CHECK REMOVESECOND
        int bestEval = -INFINITY;
        //Move bestMove = null;
        PylosSphere bestSphere = null;
        int eval = 0;
        int eval2 = 0;

        for(PylosSphere sphere : Arrays.asList(board.getSpheres(this))){

            if(sphere.canRemove()){

                PylosLocation ploc = sphere.getLocation();
                simulator.removeSphere(sphere);
                eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);
                simulator.undoRemoveSecondSphere(sphere, ploc, PylosGameState.REMOVE_SECOND, this.PLAYER_COLOR);

                if (eval > bestEval) {
                    bestEval = eval;
                    //bestMove = move;
                    bestSphere = sphere;
                }
            }
        }

        //CHECK PASS
        simulator.pass();
        eval2 = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);
        simulator.undoPass(PylosGameState.REMOVE_SECOND, this.PLAYER_COLOR);

        if(eval >= eval2 && bestSphere != null){
            game.removeSphere(bestSphere);
        }
        else{
            game.pass();
        }

    }

    private int alphaBeta(PylosGameIF game, PylosBoard board, int depth, int alpha, int beta, boolean maximizingPlayer) {

        PylosPlayerColor beforeMove = null;
        PylosPlayerColor afterMove = null;

        int eval = 0;

        if (depth == 0 || simulator.getState() == PylosGameState.COMPLETED) {
            return evaluatePosition(board);
        }

        //hier opletten, 't is niet gelijk in schaak/dammen dat het altijd aan de andere speler is na elke zet
        if (simulator.getColor() == this.PLAYER_COLOR.other()) {

            PylosPlayerColor currentPlayerColor = simulator.getColor();

            int maxEval = -INFINITY;

            switch(simulator.getState()) {

                case MOVE:

                    for (Move move : getLegalMoves(board, this.OTHER).stream().filter(s -> !s.sphere.isReserve()).collect(Collectors.toList())) {

                        try{
                            beforeMove = simulator.getColor();

                            if(move.sphere.PLAYER_COLOR == simulator.getColor())    {
                                simulator.moveSphere(move.sphere, move.location);
                                eval = alphaBeta(game, board, depth-1, alpha, beta, false);
                                simulator.undoMoveSphere(move.sphere, move.previousLocation, PylosGameState.MOVE, move.sphere.PLAYER_COLOR.other());
                            }

                            afterMove = simulator.getColor();

                        }catch(AssertionError ex){
                            System.out.println(ex.toString());
                        }
                        maxEval = Math.max(maxEval, eval);
                        alpha = Math.max(alpha, eval);
                        if (beta <= alpha) {
                            break;
                        }
                    }
                    //try to move reserve spheres
                    for (Move move : getLegalMoves(board, this.OTHER).stream().filter(s -> s.sphere.isReserve()).collect(Collectors.toList())) {

                        try{

                            beforeMove = simulator.getColor();

                            if(move.sphere.PLAYER_COLOR == simulator.getColor()) {
                                simulator.moveSphere(move.sphere, move.location);
                                eval = alphaBeta(game, board, depth - 1, alpha, beta, false);
                                simulator.undoAddSphere(move.sphere, PylosGameState.MOVE, move.sphere.PLAYER_COLOR.other());
                            }

                            afterMove = simulator.getColor();

                        }catch(AssertionError ex){
                            System.out.println(ex.toString());
                        }

                        maxEval = Math.max(maxEval, eval);
                        alpha = Math.max(alpha, eval);
                        if (beta <= alpha) {
                            break;
                        }
                    }

                    break;

                case REMOVE_FIRST:

                    for(PylosSphere sphere : Arrays.asList(board.getSpheres(this.OTHER))){

                        if(sphere.canRemove()){

                            PylosLocation ploc = sphere.getLocation();
                            simulator.removeSphere(sphere);
                            eval = alphaBeta(game, board, depth-1, alpha, beta, false);
                            simulator.undoRemoveFirstSphere(sphere, ploc, PylosGameState.REMOVE_FIRST, this.PLAYER_COLOR.other());

                            maxEval = Math.max(maxEval, eval);
                            alpha = Math.max(alpha, eval);
                            if (beta <= alpha) {
                                break;
                            }
                        }
                    }

                    break;

                case REMOVE_SECOND:

                    for(PylosSphere sphere : Arrays.asList(board.getSpheres(this.OTHER))){

                        if(sphere.canRemove()){

                            PylosLocation ploc = sphere.getLocation();
                            simulator.removeSphere(sphere);
                            eval = alphaBeta(game, board, depth-1, alpha, beta, false);
                            simulator.undoRemoveSecondSphere(sphere, ploc, PylosGameState.REMOVE_SECOND, this.PLAYER_COLOR.other());

                            maxEval = Math.max(maxEval, eval);
                            alpha = Math.max(alpha, eval);
                            if (beta <= alpha) {
                                break;
                            }
                        }
                    }

                    break;
                //case COMPLETED:
                //    break;
                default:
                    break;

            }

            return maxEval;
            //return Math.max(maxEval, maxEval2);

        } else {

            int minEval = INFINITY;

            switch(simulator.getState()) {

                case MOVE:

                    for (Move move : getLegalMoves(board, this).stream().filter(s -> !s.sphere.isReserve()).collect(Collectors.toList())) {

                        try {

                            simulator.moveSphere(move.sphere, move.location);
                            eval = alphaBeta(game, board, depth-1, alpha, beta, true);
                            simulator.undoMoveSphere(move.sphere, move.previousLocation, PylosGameState.MOVE, move.sphere.PLAYER_COLOR);

                        }catch(AssertionError ex){
                            System.out.println(ex.toString());
                        }

                        minEval = Math.min(minEval, eval);
                        beta = Math.min(beta, eval);
                        if (beta <= alpha) {
                            break;
                        }
                    }
                    //try to move reserve spheres
                    for (Move move : getLegalMoves(board, this).stream().filter(s -> s.sphere.isReserve()).collect(Collectors.toList())) {

                        try {

                            simulator.moveSphere(move.sphere, move.location);
                            eval = alphaBeta(game, board, depth-1, alpha, beta, true);
                            simulator.undoAddSphere(move.sphere, PylosGameState.MOVE, move.sphere.PLAYER_COLOR);

                            //System.out.println("Successfully removed reserve sphere");
                        }catch(AssertionError ex){
                            System.out.println(ex.toString());
                        }

                        minEval = Math.min(minEval, eval);
                        beta = Math.min(beta, eval);
                        if (beta <= alpha) {
                            break;
                        }
                    }
                    break;

                case REMOVE_FIRST:

                    for(PylosSphere sphere : Arrays.asList(board.getSpheres(this))){

                        if(sphere.canRemove()){

                            PylosLocation ploc = sphere.getLocation();
                            simulator.removeSphere(sphere);
                            eval = alphaBeta(game, board, depth-1, alpha, beta, true);
                            simulator.undoRemoveFirstSphere(sphere, ploc, PylosGameState.REMOVE_FIRST, this.PLAYER_COLOR);

                            minEval = Math.min(minEval, eval);
                            beta = Math.min(beta, eval);
                            if (beta <= alpha) {
                                break;
                            }
                        }
                    }

                    break;

                case REMOVE_SECOND:

                    //TODO: FAILS

                    for(PylosSphere sphere : Arrays.asList(board.getSpheres(this))){

                        if(sphere.canRemove()){

                            PylosLocation ploc = sphere.getLocation();
                            simulator.removeSphere(sphere);
                            eval = alphaBeta(game, board, depth-1, alpha, beta, true);
                            simulator.undoRemoveSecondSphere(sphere, ploc, PylosGameState.REMOVE_SECOND, this.PLAYER_COLOR);

                            minEval = Math.min(minEval, eval);
                            beta = Math.min(beta, eval);
                            if (beta <= alpha) {
                                break;
                            }
                        }
                    }

                    break;
                //case COMPLETED:
                //    break;
                default:
                    break;

            }

            return minEval;
            //return Math.min(minEval, minEval2);
        }
    }

    private List<Move> getLegalMoves(PylosBoard board, PylosPlayer player){

        List<Move> allPossibleMoves = new ArrayList<>();

        List<PylosLocation> usableLocations = new ArrayList<>();
        List<PylosSphere> usableSpheres = new ArrayList<>();

        for(PylosLocation location: board.getLocations()){
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
        //return (-15 + new Random().nextInt(31));
    }

}


