package megamek.rating;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class RatingManagerTest {
    private RatingManager ratingManager;
    private EloRatingCalculator calculator;
    private static final double DELTA = 0.0001;

    @Before
    public void setUp() {
        calculator = new EloRatingCalculator(30);
        ratingManager = new RatingManager(calculator);
    }

    @Test
    public void testInitialRating() {
        int playerId = 1;
        // Le rating par défaut pour un joueur non initialisé doit être 1500
        assertEquals(1500, ratingManager.getRating(playerId), DELTA);
    }

    @Test
    public void testUpdateRatings() {
        int playerA = 1;
        int playerB = 2;
        // On s'assure que les joueurs sont initialisés (rating 1500)
        ratingManager.initializePlayer(playerA);
        ratingManager.initializePlayer(playerB);

        // Simuler une victoire de playerA contre playerB (resultA = 1.0)
        ratingManager.updateRatings(playerA, playerB, 1.0);

        // D'après le calcul Elo avec K = 30 et un match entre deux joueurs ayant le même rating :
        // nouveau rating de A = 1515 et de B = 1485.
        assertEquals(1515, ratingManager.getRating(playerA), DELTA);
        assertEquals(1485, ratingManager.getRating(playerB), DELTA);
    }
}
