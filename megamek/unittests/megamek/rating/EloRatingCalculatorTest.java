package megamek.rating;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
public class EloRatingCalculatorTest {
    private EloRatingCalculator calculator;
    // Précision pour les comparaisons en virgule flottante
    private static final double DELTA = 0.0001;

    @BeforeEach
    public void setUp() {
        // Ici K est fixé à 30 (vous pouvez le modifier si besoin)
        calculator = new EloRatingCalculator(30);
    }

    @Test
    public void testCalculateNewRatingsWhenEqual() {
        double ratingA = 1500;
        double ratingB = 1500;
        // Match nul : résultat de A est 0.5
        double[] newRatings = calculator.calculateNewRatings(ratingA, ratingB, 0.5);
        // Pour un match nul, avec des ratings égaux, il n'y a pas de changement
        assertEquals(1500, newRatings[0], DELTA);
        assertEquals(1500, newRatings[1], DELTA);
    }

    @Test
    public void testCalculateNewRatingsWhenAWin() {
        double ratingA = 1500;
        double ratingB = 1500;
        // Victoire de A (resultA = 1.0)
        double[] newRatings = calculator.calculateNewRatings(ratingA, ratingB, 1.0);
        // expectedA = 1 / (1 + 10^(0/400)) = 0.5
        // nouvelle valeur de A = 1500 + 30 * (1.0 - 0.5) = 1515
        // nouvelle valeur de B = 1500 + 30 * (0.0 - 0.5) = 1485
        assertEquals(1515, newRatings[0], DELTA);
        assertEquals(1485, newRatings[1], DELTA);
    }

    @Test
    public void testCalculateNewRatingsWhenALose() {
        double ratingA = 1500;
        double ratingB = 1500;
        // Défaite de A (resultA = 0.0)
        double[] newRatings = calculator.calculateNewRatings(ratingA, ratingB, 0.0);
        // Nouvelle valeur de A = 1500 + 30 * (0.0 - 0.5) = 1485
        // Nouvelle valeur de B = 1500 + 30 * (1.0 - 0.5) = 1515
        assertEquals(1485, newRatings[0], DELTA);
        assertEquals(1515, newRatings[1], DELTA);
    }
}
