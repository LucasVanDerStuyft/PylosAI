package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StudentPlayerBestFit3 extends PylosPlayer{

    private static final int MAX_DEPTH = 500;
    private static final int INFINITY = 1000000;
    private PylosGameSimulator simulator;
    private PylosGameState prevState;
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

        List<PylosSphere> allSpheres = Arrays.asList(board.getSpheres(this));
        List<PylosSphere> removableSpheres = new ArrayList<>(allSpheres);
        removableSpheres.removeIf(s -> s.isReserve() || s.getLocation().hasAbove());

        PylosSphere sphereToRemove;
        sphereToRemove = removableSpheres.size() == 1 ? removableSpheres.get(0) : removableSpheres.get(getRandom().nextInt(removableSpheres.size() - 1));
        game.removeSphere(sphereToRemove);
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
        //if (maximizingPlayer){
        if (maximizingPlayer && simulator.getColor() == this.PLAYER_COLOR.other()) {
            int maxEval = -INFINITY;

            switch(simulator.getState()) {

                case MOVE:

                    for (Move move : getLegalMoves(board, this.OTHER).stream().filter(s -> !s.sphere.isReserve()).collect(Collectors.toList())) {

                        simulator.moveSphere(move.sphere, move.location);
                        eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);
                        simulator.undoMoveSphere(move.sphere, move.previousLocation, PylosGameState.MOVE, move.sphere.PLAYER_COLOR.other());

                        maxEval = Math.max(maxEval, eval);
                        alpha = Math.max(alpha, eval);
                        if (beta <= alpha) {
                            break;
                        }
                    }
                    //try to move reserve spheres
                    for (Move move : getLegalMoves(board, this.OTHER).stream().filter(s -> s.sphere.isReserve()).collect(Collectors.toList())) {

                        simulator.moveSphere(move.sphere, move.location);
                        eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);
                        simulator.undoAddSphere(move.sphere, prevState, move.sphere.PLAYER_COLOR.other());

                        maxEval = Math.max(maxEval, eval);
                        alpha = Math.max(alpha, eval);
                        if (beta <= alpha) {
                            break;
                        }
                    }
                    break;

                case REMOVE_FIRST:
                    break;
                case REMOVE_SECOND:
                    break;
                //case COMPLETED:
                //    break;
                default:
                    break;

            }

            return maxEval;

        } else {

            //CHECK FOR OTHER PLAYER COLOR
            int minEval = INFINITY;

            if(simulator.getColor() == this.PLAYER_COLOR){

            switch(simulator.getState()) {

                case MOVE:

                    for (Move move : getLegalMoves(board, this).stream().filter(s -> !s.sphere.isReserve()).collect(Collectors.toList())) {

                        simulator.moveSphere(move.sphere, move.location);
                        eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, true);
                        simulator.undoMoveSphere(move.sphere, move.previousLocation, PylosGameState.MOVE, move.sphere.PLAYER_COLOR);

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
                            eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, true);
                            simulator.undoAddSphere(move.sphere, prevState, move.sphere.PLAYER_COLOR);
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
                    break;
                case REMOVE_SECOND:
                    break;
                //case COMPLETED:
                //    break;
                default:
                    break;

            }}
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


