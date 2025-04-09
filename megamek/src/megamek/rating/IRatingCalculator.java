package megamek.rating;

public interface IRatingCalculator {
    /**
     * Calcule les nouveaux ratings pour deux joueurs après un match.
     * @param ratingA rating actuel du joueur A
     * @param ratingB rating actuel du joueur B
     * @param resultA résultat du match pour le joueur A (1.0 victoire, 0.5 match nul, 0 défaite)
     * @return un tableau contenant les nouveaux ratings {nouveauRatingA, nouveauRatingB}
     */
    double[] calculateNewRatings(double ratingA, double ratingB, double resultA);
}
