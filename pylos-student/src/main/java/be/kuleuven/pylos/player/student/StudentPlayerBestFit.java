package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;
import java.util.stream.Collectors;

public class StudentPlayerBestFit extends PylosPlayer{
    private static final int MAX_DEPTH = 4;
    private static final int INFINITY = 1000000;
    private PylosGameSimulator simulator;
    private boolean debugInfo = false;
    private int randomSeed = 100;
    private boolean useRandimization = false;
    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        simulator = new PylosGameSimulator(game.getState(),this.PLAYER_COLOR,board);

        int bestEval = -INFINITY;
        PylosSphere bestSphere = null;
        PylosLocation bestLocation = null;

        for(PylosSphere sphere : Arrays.asList(board.getSpheres(this)).stream().filter(x -> !x.isReserve()).collect(Collectors.toList())){

            List<PylosLocation> allUsableLocations = Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList());
            if(useRandimization) {
                Collections.shuffle(allUsableLocations, new Random(randomSeed));
            }
            else{
                sortZorMaxInSquare(allUsableLocations, this);
            }

            for(PylosLocation location : allUsableLocations){

                if(sphere.canMoveTo(location)){
                    PylosLocation previousLocation = sphere.getLocation();

                    simulator.moveSphere(sphere, location);
                    int eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);
                    simulator.undoMoveSphere(sphere, previousLocation, PylosGameState.MOVE, sphere.PLAYER_COLOR);

                    if (eval >= bestEval) {
                        bestEval = eval;
                        bestLocation = location;
                        bestSphere = sphere;
                    }
                }
            }
        }

        //try to move reserve spheres
        List<PylosLocation> allUsableLocations = Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList());
        if(useRandimization) {
            Collections.shuffle(allUsableLocations, new Random(randomSeed));
        }
        else{
            sortZorMaxInSquare(allUsableLocations, this);
        }

        for(PylosLocation location : allUsableLocations){

            PylosSphere reserveSphere = board.getReserve(simulator.getColor());

            simulator.moveSphere(reserveSphere, location);
            int eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);
            simulator.undoAddSphere(reserveSphere, PylosGameState.MOVE, PLAYER_COLOR);

            if (eval >= bestEval) {
                bestEval = eval;
                bestLocation = location;
                bestSphere = reserveSphere;
            }
        }
        //try {
            game.moveSphere(bestSphere, bestLocation);
        //}catch(Exception ex){
        //    System.out.println(ex.toString());
        //}
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {

        simulator = new PylosGameSimulator(game.getState(),this.PLAYER_COLOR,board);

        int bestEval = -INFINITY;
        PylosSphere bestSphere = null;

        List<PylosSphere> usableSpheres = Arrays.asList(board.getSpheres(this));
        if(useRandimization){
            Collections.shuffle(usableSpheres);
        }

        for(PylosSphere sphere : usableSpheres){
            if(sphere.canRemove()){

                PylosLocation ploc = sphere.getLocation();
                simulator.removeSphere(sphere);
                int eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, false);
                simulator.undoRemoveFirstSphere(sphere, ploc, PylosGameState.REMOVE_FIRST, this.PLAYER_COLOR);

                if (eval >= bestEval) {
                    bestEval = eval;
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
                eval = alphaBeta(game, board, MAX_DEPTH, -INFINITY, INFINITY, true);
                simulator.undoRemoveSecondSphere(sphere, ploc, PylosGameState.REMOVE_SECOND, this.PLAYER_COLOR);

                if (eval >= bestEval) {
                    bestEval = eval;
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
            //return evaluatePosition(board);
            return evaluatePosition(board, game);
        }

        //hier opletten, 't is niet zoals in schaak/dammen dat het altijd aan de andere speler is na elke zet
        if (simulator.getColor() == this.PLAYER_COLOR) {
        //if(maximizingPlayer){

            PylosPlayerColor currentPlayerColor = simulator.getColor();

            int maxEval = -INFINITY;

            switch(simulator.getState()) {

                case MOVE:

                    for(PylosSphere sphere : Arrays.asList(board.getSpheres(this)).stream().filter(x -> !x.isReserve()).collect(Collectors.toList())){

                        List<PylosLocation> allUsableLocations = Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList());
                        if(useRandimization) {
                            Collections.shuffle(allUsableLocations, new Random(randomSeed));
                        }
                        else{
                            sortZorMaxInSquare(allUsableLocations, this);
                        }

                        for(PylosLocation location : allUsableLocations){

                            if(sphere.canMoveTo(location)){
                                PylosLocation previousLocation = sphere.getLocation();

                                simulator.moveSphere(sphere, location);
                                eval = alphaBeta(game, board, depth-1, alpha, beta, false);
                                simulator.undoMoveSphere(sphere, previousLocation, PylosGameState.MOVE, sphere.PLAYER_COLOR);

                                maxEval = Math.max(maxEval, eval);
                                alpha = Math.max(alpha, eval);
                                if (beta <= alpha) {
                                    if(debugInfo) System.out.println("Pruning at move");
                                    break;
                                }
                            }
                        }
                    }

                    //try to move reserve spheres
                    List<PylosLocation> allUsableLocations = Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList());
                    if(useRandimization) {
                        Collections.shuffle(allUsableLocations, new Random(randomSeed));
                    }
                    else {
                        sortZorMaxInSquare(allUsableLocations, this);
                    }

                    for(PylosLocation location : allUsableLocations){

                        PylosSphere reserveSphere = board.getReserve(simulator.getColor());
                        PylosLocation prevLocation = reserveSphere.getLocation();

                        simulator.moveSphere(reserveSphere, location);
                        eval = alphaBeta(game, board, depth - 1, alpha, beta, false);
                        simulator.undoAddSphere(reserveSphere, PylosGameState.MOVE, reserveSphere.PLAYER_COLOR);

                        maxEval = Math.max(maxEval, eval);
                        alpha = Math.max(alpha, eval);
                        if (beta <= alpha) {
                            if(debugInfo) System.out.println("Pruning at add reserve NEW");
                            break;
                        }
                    }

                    break;

                case REMOVE_FIRST:

                    for(PylosSphere sphere : Arrays.asList(board.getSpheres(this))){

                        if(sphere.canRemove()){

                            PylosLocation ploc = sphere.getLocation();
                            simulator.removeSphere(sphere);
                            eval = alphaBeta(game, board, depth-1, alpha, beta, false);
                            simulator.undoRemoveFirstSphere(sphere, ploc, PylosGameState.REMOVE_FIRST, this.PLAYER_COLOR);

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

                    for(PylosSphere sphere : Arrays.asList(board.getSpheres(this))){

                        if(sphere.canRemove()){

                            PylosLocation ploc = sphere.getLocation();
                            simulator.removeSphere(sphere);
                            eval = alphaBeta(game, board, depth-1, alpha, beta, false);
                            simulator.undoRemoveSecondSphere(sphere, ploc, PylosGameState.REMOVE_SECOND, this.PLAYER_COLOR);

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

        } else {

            int minEval = INFINITY;

            switch(simulator.getState()) {

                case MOVE:

                    for(PylosSphere sphere : Arrays.asList(board.getSpheres(this.OTHER)).stream().filter(x -> !x.isReserve()).collect(Collectors.toList())){

                        List<PylosLocation> allUsableLocations = Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList());
                        if(useRandimization) {
                            Collections.shuffle(allUsableLocations, new Random(randomSeed));
                        }
                        else{
                            sortZorMaxInSquare(allUsableLocations, this.OTHER);
                        }

                        for(PylosLocation location : allUsableLocations){

                            if(sphere.canMoveTo(location)){
                                PylosLocation previousLocation = sphere.getLocation();

                                if(sphere.PLAYER_COLOR == simulator.getColor()) {
                                    simulator.moveSphere(sphere, location);
                                    eval = alphaBeta(game, board, depth - 1, alpha, beta, true);
                                    simulator.undoMoveSphere(sphere, previousLocation, PylosGameState.MOVE, sphere.PLAYER_COLOR.other());
                                }
                                minEval = Math.min(minEval, eval);
                                beta = Math.min(beta, eval);
                                if (beta <= alpha) {
                                    if(debugInfo) System.out.println("Pruning at move");
                                    break;
                                }
                            }
                        }
                    }

                    //try to move reserve spheres
                    List<PylosLocation> allUsableLocations = Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList());
                    if(useRandimization) {
                        Collections.shuffle(allUsableLocations, new Random(randomSeed));
                    }
                    else{
                        sortZorMaxInSquare(allUsableLocations, this.OTHER);
                    }

                    for(PylosLocation location : allUsableLocations){

                        PylosSphere reserveSphere = board.getReserve(simulator.getColor());

                        simulator.moveSphere(reserveSphere, location);
                        eval = alphaBeta(game, board, depth - 1, alpha, beta, true);
                        simulator.undoAddSphere(reserveSphere, PylosGameState.MOVE, reserveSphere.PLAYER_COLOR.other());

                        minEval = Math.min(minEval, eval);
                        beta = Math.min(beta, eval);
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
                            eval = alphaBeta(game, board, depth-1, alpha, beta, true);
                            simulator.undoRemoveFirstSphere(sphere, ploc, PylosGameState.REMOVE_FIRST, this.PLAYER_COLOR.other());

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

                    for(PylosSphere sphere : Arrays.asList(board.getSpheres(this.OTHER))){

                        if(sphere.canRemove()){

                            PylosLocation ploc = sphere.getLocation();
                            simulator.removeSphere(sphere);
                            eval = alphaBeta(game, board, depth-1, alpha, beta, true);
                            simulator.undoRemoveSecondSphere(sphere, ploc, PylosGameState.REMOVE_SECOND, this.PLAYER_COLOR.other());

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

        }
    }

    private int evaluatePosition(PylosBoard board, PylosGameIF game) {

        int weightReserveSpheres = 12;
        int weight4SpheresInSquare = 100;
        int weight3SpheresInSquare = 50;
        int weight2SpheresInSquare = 10;
        int weightOurLevel0 = 5;
        int weightEnemyLevel0 = 5;
        int weightOurLevel1= 10;
        int weightEnemyLevel1 = 10;
        int weightOurLevel2 = 25;
        int weightEnemyLevel2 = 25;
        int weightOurLevel3 = 25;
        int weightEnemyLevel3 = 25;

        int scoreReserveSpheres = weightReserveSpheres * (board.getReservesSize(PLAYER_COLOR) - board.getReservesSize(PLAYER_COLOR.other()));

        List<PylosLocation> allUsableLocations = Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList());

        int ourSquares_4 = weight4SpheresInSquare * countSpheresInSquares(allUsableLocations, this, 4);
        int enemySquares_4 = weight4SpheresInSquare * countSpheresInSquares(allUsableLocations, this.OTHER, 4);
        int ourSquares_3 = weight3SpheresInSquare * countSpheresInSquares(allUsableLocations, this, 3);
        int enemySquares_3 = weight3SpheresInSquare * countSpheresInSquares(allUsableLocations, this.OTHER, 3);
        int ourSquares_2 = weight2SpheresInSquare * countSpheresInSquares(allUsableLocations, this,2);
        int enemySquares_2 = weight2SpheresInSquare * countSpheresInSquares(allUsableLocations, this.OTHER,2);

        int ourSpheresLevel0 = weightOurLevel0 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this)), 1);
        int enemySpheresLevel0 = weightEnemyLevel0 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this.OTHER)), 1);
        int ourSpheresLevel1 = weightOurLevel1 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this)), 2);
        int enemySpheresLevel1 = weightEnemyLevel1 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this.OTHER)), 2);
        int ourSpheresLevel2 = weightOurLevel2 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this)), 3);
        int enemySpheresLevel2 = weightEnemyLevel2 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this.OTHER)), 3);
        int ourSpheresLevel3 = weightOurLevel3 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this)), 4);
        int enemySpheresLevel3 = weightEnemyLevel3 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this.OTHER)), 4);

        //int our_center_spheres = Arrays.stream(board.getSpheres(this)).filter(s-> !s.isReserve() && s.getLocation().X > 0 && s.getLocation().X < 3 && s.getLocation().Y >0 && s.getLocation().Y < 3).collect(Collectors.toList()).size();
        //int enemy_center_spheres = Arrays.stream(board.getSpheres(this.OTHER)).filter(s -> !s.isReserve() && s.getLocation().X > 0 && s.getLocation().X < 3 && s.getLocation().Y >0 && s.getLocation().Y < 3).collect(Collectors.toList()).size();
        //int board_stability = 5 * (our_center_spheres - enemy_center_spheres);

        //int ourOpeningMove = Arrays.stream(board.getSpheres(this)).filter(s-> !s.isReserve() && s.getLocation().X > 0 && s.getLocation().X < 3 && s.getLocation().Y >0 && s.getLocation().Y < 3 && s.getLocation().Z == 0).collect(Collectors.toList()).size();
        //int enemyOpeningMove = Arrays.stream(board.getSpheres(this.OTHER)).filter(s -> !s.isReserve() && s.getLocation().X > 0 && s.getLocation().X < 3 && s.getLocation().Y >0 && s.getLocation().Y < 3 && s.getLocation().Z == 0).collect(Collectors.toList()).size();

        int openingMove = 0;
        int board_stability = 0;


        int sabotageScore = evaluateSphereLocations(Arrays.asList(board.getSpheres(this)));
        //sabotageScore -= evaluateSphereLocations(Arrays.asList(board.getSpheres(this.OTHER)));



        int finalScore = scoreReserveSpheres +
                         ourSquares_4 + ourSquares_3 + ourSquares_2 - enemySquares_4 - enemySquares_3 - enemySquares_2
                         + ourSpheresLevel0 + ourSpheresLevel1 + ourSpheresLevel2 + ourSpheresLevel3 - enemySpheresLevel0 - enemySpheresLevel1 - enemySpheresLevel2 - enemySpheresLevel3
                         + board_stability
                         + openingMove
                         + sabotageScore;

        return finalScore;
    }

    public int countSpheresInSquares(List<PylosLocation> allowedLocations, PylosPlayer player, int amount){

        List<PylosLocation> result =new ArrayList<>();
        for (PylosLocation l : allowedLocations){
            for (PylosSquare square : l.getSquares()){
                if (square.getInSquare(player)==amount){
                    result.add(l);
                }
            }
        }
        return result.size();
    }

    public int evaluateSphereLocations(List<PylosSphere> spheres){

        int score = 0;

        for(PylosSphere sphere : spheres.stream().filter(x -> !x.isReserve()).collect(Collectors.toList())){
            for(PylosSquare square : sphere.getLocation().getSquares()){

                if(square.getInSquare(this) == 4){
                    //score += 100;
                }
                else if(square.getInSquare(this) == 3 && square.getInSquare(this.OTHER) == 0){
                    score += 50;
                }
                else if(square.getInSquare(this) == 1 && square.getInSquare(this.OTHER) == 3){
                    score += 25;
                }
            }
        }

        return score;
    }

    public int countSpheresOnLevel(List<PylosSphere> spheres, int level){

        int count = 0;
        for(PylosSphere sphere : spheres.stream().filter(x -> !x.isReserve()).collect(Collectors.toList())){

            if(sphere.getLocation().Z == level){
                count++;
            }
        }
        return count;
    }
    private void sortZorMaxInSquare(List<PylosLocation> locations, PylosPlayer player) {
        Collections.sort(locations, new Comparator<PylosLocation>() {
            @Override
            public int compare(PylosLocation o1, PylosLocation o2) {
                int compZ = -Integer.compare(o1.Z, o2.Z);
                if (compZ != 0) return compZ;
                return -Integer.compare(o1.getMaxInSquare(player), o2.getMaxInSquare(player));
            }
        });
    }

}


