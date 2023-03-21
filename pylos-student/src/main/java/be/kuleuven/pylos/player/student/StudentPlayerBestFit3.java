package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;
import be.kuleuven.pylos.player.codes.PylosPlayerBestFit;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class StudentPlayerBestFit3 extends PylosPlayer{

    private static final int MAX_DEPTH = 4;
    private static final int INFINITY = 1000000;
    private PylosGameSimulator simulator;
    private boolean debugInfo = false;
    private int randomSeed = 100;
    private boolean useRandimization = false;
    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        //TODO: functie om locations/spheres ipv random te shufflen echt een score geeft. Je gaat maar 4 diep dus hij evalueert eigenlijk maar 4 locations/moves
        //dat is eigenlijk beter of die evaluatiefunctie proberen aanpassen
        //BEST OVERAL DEZELFDE SEED BIJ RANDOMISEREN

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

        try {
            game.moveSphere(bestSphere, bestLocation);
        }catch(Exception ex){
            System.out.println(ex.toString());
        }
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
        //for(PylosSphere sphere : Arrays.asList(board.getSpheres(this))){

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
                    else{
                        sortZorMaxInSquare(allUsableLocations, this);
                    }

                    for(PylosLocation location : allUsableLocations){
                    //for(PylosLocation location : Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList())){

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
            //return Math.max(maxEval, maxEval2);

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
                        //for(PylosLocation location : Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList())){

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

    private int evaluatePositionOrig(PylosBoard board) {

        //momenteel een zeer simpele functie om te testen
        //we trekken het aantal zwarte spheres die nog op het bord liggen af van de witte
        //hoe minder witte spheres, hoe beter want dan moet zwart vroeger alles op 't bord leggen en winnen wij uiteindelijk
        //deze score moet natuurlijk uiteindelijk rekening houden met vierkanten die gemaakt kunnen worden en dergelijke

        //int score = board.getSpheres(this).length - board.getSpheres(this.OTHER).length;
        //int score = board.getSpheres(this.OTHER).length - board.getSpheres(this).length;
        int score = board.getReservesSize(PLAYER_COLOR) - board.getReservesSize(PLAYER_COLOR.other());

        //kijken hoeveel "bijna vierkanten" er gemaakt zijn (dus 3 spheres in een vierkant)
        List<PylosLocation> allUsableLocations = Arrays.asList(board.getLocations()).stream().filter(x -> x.isUsable()).collect(Collectors.toList());

        List<PylosLocation> ourSquares = searchForSquares(allUsableLocations, this);
        List<PylosLocation> enemySquares = searchForSquares(allUsableLocations, this.OTHER);

        //int squareDifference = ourSquares.size() - enemySquares.size();
        int squareDifference = enemySquares.size() - ourSquares.size();
        if(squareDifference > 2){
            if(debugInfo) System.out.println("squarediff > 2");
        }


        score = score - (squareDifference * 20);

        return score;

        //return board.getReservesSize(PLAYER_COLOR) - board.getReservesSize(PLAYER_COLOR.other());
    }

    private int evaluatePosition(PylosBoard board, PylosGameIF game) {

        //int weightReserveSpheres = 15;
        //int weightReserveSpheres = 12;
        int weightReserveSpheres = 12;
        int weight4SpheresInSquare = 100;
        int weight3SpheresInSquare = 50;
        int weight2SpheresInSquare = 10;
        int weightEnemyHigherZ = 5;
        int weightOurHigherZ = 5;
        int weightOurLevel0 = 5;
        int weightEnemyLevel0 = 5;
        int weightOurLevel1= 5;
        int weightEnemyLevel1 = 5;
        int weightOurLevel2 = 15;
        int weightEnemyLevel2 = 15;
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

        //int ourMaxZ = weightEnemyHigherZ * getMaxZorMaxInSquare(allUsableLocations, this).Z;
        //int enemyMaxZ = weightOurHigherZ * getMaxZorMaxInSquare(allUsableLocations, this.OTHER).Z;

        //we moeten hier iets verzinnen dat een betere score geeft als we een bal hoger kunnen leggen
        int ourSpheresLevel0 = weightOurLevel0 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this)), 1);
        int enemySpheresLevel0 = weightEnemyLevel0 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this.OTHER)), 1);
        int ourSpheresLevel1 = weightOurLevel1 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this)), 2);
        int enemySpheresLevel1 = weightEnemyLevel1 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this.OTHER)), 2);
        int ourSpheresLevel2 = weightOurLevel2 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this)), 3);
        int enemySpheresLevel2 = weightEnemyLevel2 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this.OTHER)), 3);
        int ourSpheresLevel3 = weightOurLevel3 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this)), 4);
        int enemySpheresLevel3 = weightEnemyLevel3 * countSpheresOnLevel(Arrays.asList(board.getSpheres(this.OTHER)), 4);


        //int centerMarbles = countMarblesInSquare(board, CENTER_X, CENTER_Y);
        //int outerMarbles = countMarblesInSquare(board, OUTER_X, OUTER_Y);
        //score += centerMarbles * 5;
        //score += outerMarbles * 2;
        //openingszet in 't midden belonen
        //int board_stability = 50 * allUsableLocations.stream().filter(l -> l.X > 0 && l.X < 4 && l.Y >0 && l.Y < 4).collect(Collectors.toList()).size();

        int our_center_spheres = Arrays.stream(board.getSpheres(this)).filter(s-> !s.isReserve() && s.getLocation().X > 0 && s.getLocation().X < 4 && s.getLocation().Y >0 && s.getLocation().Y < 4 && s.getLocation().Z == 0).collect(Collectors.toList()).size();
        int enemy_center_spheres = Arrays.stream(board.getSpheres(this.OTHER)).filter(s -> !s.isReserve() && s.getLocation().X > 0 && s.getLocation().X < 4 && s.getLocation().Y >0 && s.getLocation().Y < 4 && s.getLocation().Z == 0).collect(Collectors.toList()).size();

        int board_stability = (our_center_spheres - enemy_center_spheres);
        board_stability = 0;


        int finalScore = scoreReserveSpheres +
                         ourSquares_4 + ourSquares_3 + ourSquares_2 - enemySquares_4 - enemySquares_3 - enemySquares_2
                         + ourSpheresLevel0 + ourSpheresLevel1 + ourSpheresLevel2 + ourSpheresLevel3 - enemySpheresLevel0 - enemySpheresLevel1 - enemySpheresLevel2 - enemySpheresLevel3
                         + board_stability;


        //OVERBODIG DENK IK
        /*
        if(game.getWinner() == this){
            finalScore += 1000;
        }
        else if(game.getWinner() == this.OTHER){
            finalScore -= 1000;
        }
        */


        return finalScore;
    }

    public List<PylosLocation> searchForSquares(List<PylosLocation> allowedLocations, PylosPlayer player){

        List<PylosLocation> result = new ArrayList<>();
        for (PylosLocation l : allowedLocations){
            for (PylosSquare square : l.getSquares()){
                if (square.getInSquare(player)==3){
                    result.add(l);
                }
            }
        }
        return result;
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

    public int countSpheresOnLevel(List<PylosSphere> spheres, int level){

        int count = 0;
        for(PylosSphere sphere : spheres.stream().filter(x -> !x.isReserve()).collect(Collectors.toList())){

            if(sphere.getLocation().Z == level){
                count++;
            }
        }
        return count;
    }

    private PylosLocation getMaxInSquare(List<PylosLocation> locations, PylosPlayer player) {
        return Collections.max(locations, new Comparator<PylosLocation>() {
            @Override
            public int compare(PylosLocation o1, PylosLocation o2) {
                return Integer.compare(o1.getMaxInSquare(player), o2.getMaxInSquare(player));
            }
        });
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


