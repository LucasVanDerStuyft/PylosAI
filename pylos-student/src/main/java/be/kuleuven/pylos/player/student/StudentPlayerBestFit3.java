package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;
import java.util.stream.Collectors;

public class StudentPlayerBestFit3 extends PylosPlayer{

    private static final int MAX_DEPTH = 3;
    private static final int INFINITY = 1000000;
    private PylosGameSimulator simulator;
    private boolean debugInfo = false;
    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        //TODO: die Minimax speler randomiseert telkens de lijst van de locations en spheres
        //collections.shuffle()
        //mogelijks kiest die dan zo soms een betere openingszet

        simulator = new PylosGameSimulator(game.getState(),this.PLAYER_COLOR,board);

        int bestEval = -INFINITY;
        Move bestMove = null;
        int eval = 0;

        //try to move "normal/non-reserve" spheres
        //for (Move move : getLegalMoves(board, this).stream().filter(s -> !s.sphere.isReserve()).collect(Collectors.toList())) {
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
        //TODO: tip van docent was hier: 't is feitelijk gelijk welke reserve sphere je pakt die doen allemaal 't zelfde
        /*
        for (Move move : getLegalMoves(board, this).stream().filter(s -> s.sphere.isReserve()).collect(Collectors.toList())) {

            simulator.moveSphere(move.sphere, move.location);
            eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);
            simulator.undoAddSphere(move.sphere, PylosGameState.MOVE, move.sphere.PLAYER_COLOR);

            if (eval > bestEval) {
                bestEval = eval;
                bestMove = move;
            }
        }
        */

        //try to move reserve spheres
        for(PylosLocation location : Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList())){

            PylosSphere reserveSphere = board.getReserve(simulator.getColor());
            PylosLocation prevLocation = reserveSphere.getLocation();

            simulator.moveSphere(reserveSphere, location);
            eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);
            simulator.undoAddSphere(reserveSphere, PylosGameState.MOVE, PLAYER_COLOR);

            if (eval > bestEval) {
                bestEval = eval;
                bestMove = new Move(reserveSphere, location, prevLocation, MoveType.ADD);
            }
        }

        game.moveSphere(bestMove.sphere, bestMove.location);
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {

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

        simulator = new PylosGameSimulator(game.getState(),this.PLAYER_COLOR,board);

        //CHECK REMOVESECOND
        int bestEval = -INFINITY;
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

        //hier opletten, 't is niet zoals in schaak/dammen dat het altijd aan de andere speler is na elke zet
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
                            if(debugInfo) System.out.println("Pruning at move");
                            break;
                        }
                    }

                    //try to move reserve spheres
                    /*
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
                            System.out.println("Pruning at add reserve");
                            break;
                        }
                    }
                    */
                    for(PylosLocation location : Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList())){

                        PylosSphere reserveSphere = board.getReserve(simulator.getColor());
                        PylosLocation prevLocation = reserveSphere.getLocation();

                        simulator.moveSphere(reserveSphere, location);
                        eval = alphaBeta(game, board, depth - 1, alpha, beta, false);
                        simulator.undoAddSphere(reserveSphere, PylosGameState.MOVE, reserveSphere.PLAYER_COLOR.other());

                        maxEval = Math.max(maxEval, eval);
                        alpha = Math.max(alpha, eval);
                        if (beta <= alpha) {
                            if(debugInfo) System.out.println("Pruning at add reserve NEW");
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
                                if(debugInfo) System.out.println("Pruning at remove first");
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
                                if(debugInfo) System.out.println("Pruning at remove second");
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
                            if(debugInfo) System.out.println("Pruning at move");
                            break;
                        }
                    }
                    //try to move reserve spheres
                    /*
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
                            System.out.println("Pruning at add reserve");
                            break;
                        }
                    }
                    */

                    //try to move reserve spheres
                    for(PylosLocation location : Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList())){

                        PylosSphere reserveSphere = board.getReserve(simulator.getColor());
                        PylosLocation prevLocation = reserveSphere.getLocation();

                        simulator.moveSphere(reserveSphere, location);
                        eval = alphaBeta(game, board, depth - 1, alpha, beta, false);
                        simulator.undoAddSphere(reserveSphere, PylosGameState.MOVE, reserveSphere.PLAYER_COLOR);

                        minEval = Math.min(minEval, eval);
                        alpha = Math.max(alpha, eval);
                        if (beta <= alpha) {
                            if(debugInfo) System.out.println("Pruning at add reserve NEW2");
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
                                if(debugInfo) System.out.println("Pruning at remove first");
                                break;
                            }
                        }
                    }

                    break;

                case REMOVE_SECOND:

                    for(PylosSphere sphere : Arrays.asList(board.getSpheres(this))){

                        if(sphere.canRemove()){

                            PylosLocation ploc = sphere.getLocation();
                            simulator.removeSphere(sphere);
                            eval = alphaBeta(game, board, depth-1, alpha, beta, true);
                            simulator.undoRemoveSecondSphere(sphere, ploc, PylosGameState.REMOVE_SECOND, this.PLAYER_COLOR);

                            minEval = Math.min(minEval, eval);
                            beta = Math.min(beta, eval);
                            if (beta <= alpha) {
                                if(debugInfo) System.out.println("Pruning at remove_second");
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

        //for(PylosLocation location: board.getLocations()){
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

        int score = board.getSpheres(this).length - board.getSpheres(this.OTHER).length;

        //kijken hoeveel "bijna vierkanten" er gemaakt zijn (dus 3 spheres in een vierkant)
        List<PylosLocation> allUsableLocations = Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList());

        List<PylosLocation> ourSquares = searchForSquares(allUsableLocations, this);
        List<PylosLocation> enemySquares = searchForSquares(allUsableLocations, this.OTHER);

        //score = score - (ourSquares.size() - enemySquares.size());

        return score;

        //return (-15 + new Random().nextInt(31));
        //return (-15 + new Random().nextInt(31));
        //return board.getReservesSize(PLAYER_COLOR) - board.getReservesSize(PLAYER_COLOR.other());
    }

    public List<PylosLocation> searchForSquares(List<PylosLocation> allowedLocations, PylosPlayer player){

        List<PylosLocation> result =new ArrayList<>();
        for (PylosLocation l : allowedLocations){
            for (PylosSquare square : l.getSquares()){
                if (square.getInSquare(player)==3){
                    result.add(l);
                }
            }
        }
        return result;
    }

    //mogelijke vierkanten van eigen team
    /*
    ArrayList<PylosLocation> possibleSquares = searchForSquares(allowedLocations, this);

        if(possibleSquares.size()>0){
        Collections.shuffle(possibleSquares);
        PylosLocation destination = possibleSquares.get(0);
        //bal kiezen voor de verplaatsing te doen, voorkeur geven aan bal op bord
        //als geen ballen op bord, bal uit reserve. Indien nodig, ook type aanpassen.
        PylosSphere[] sphereList = board.getSpheres(this);
        PylosSphere finalSphere = null;
        Turn.TurnType type= MOVE;

        for (PylosSphere ps:sphereList) {
            if (ps.canMoveTo(destination) && !(ps.isReserve())){
                finalSphere = ps;
                break;
            }
        }
        if(finalSphere ==null){
            finalSphere = board.getReserve(this);
            type=ADD;
        }

        Turn moveTurn = new Turn(type, finalSphere,destination);
        moveTurn.doTurnSimulate(destination,simulator);
    }

     */


}


