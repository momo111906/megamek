package megamek.rating;

public class RatingManagerHolder {
    private static final RatingManager instance = new RatingManager(new EloRatingCalculator(30));

    public static RatingManager getInstance() {
        return instance;
    }
}
