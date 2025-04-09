package megamek.rating;

public class EloRatingCalculator implements IRatingCalculator {
    private final double K;

    public EloRatingCalculator(double K) {
        this.K = K;
    }

    @Override
    public double[] calculateNewRatings(double ratingA, double ratingB, double resultA) {
        double expectedA = 1.0 / (1.0 + Math.pow(10, (ratingB - ratingA) / 400.0));
        double expectedB = 1.0 - expectedA;
        double newRatingA = ratingA + K * (resultA - expectedA);
        double newRatingB = ratingB + K * ((1.0 - resultA) - expectedB);
        return new double[] { newRatingA, newRatingB };
    }
}
