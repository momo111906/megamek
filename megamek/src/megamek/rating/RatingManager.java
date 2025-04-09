package megamek.rating;

import java.util.HashMap;
import java.util.Map;

public class RatingManager {
    private IRatingCalculator calculator;
    // Mapping playerId -> rating
    private Map<Integer, Double> playerRatings;

    public RatingManager(IRatingCalculator calculator) {
        this.calculator = calculator;
        this.playerRatings = new HashMap<>();
    }

    // Initialise un rating par défaut si non défini
    public void initializePlayer(int playerId) {
        if (!playerRatings.containsKey(playerId)) {
            playerRatings.put(playerId, 1500.0);
        }
    }

    public double getRating(int playerId) {
        initializePlayer(playerId);
        return playerRatings.get(playerId);
    }

    /**
     * Met à jour les ratings de deux joueurs à partir du résultat du match.
     * @param playerAId ID du joueur A
     * @param playerBId ID du joueur B
     * @param resultA 1.0 si A gagne, 0.5 match nul, 0 si A perd
     */
    public void updateRatings(int playerAId, int playerBId, double resultA) {
        double ratingA = getRating(playerAId);
        double ratingB = getRating(playerBId);
        double[] newRatings = calculator.calculateNewRatings(ratingA, ratingB, resultA);
        playerRatings.put(playerAId, newRatings[0]);
        playerRatings.put(playerBId, newRatings[1]);
    }
}
