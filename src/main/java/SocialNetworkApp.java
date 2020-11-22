import connection.DataLoader;
import logic.ActivityReport;

public class SocialNetworkApp {

  public static void main(String[] args) {

    DataLoader.executeLoading();
    ActivityReport.createIndexes();

    ActivityReport.showAverageMessagesByDayOfWeek();
    ActivityReport.showMaxNumberOfNewFriendshipsFromMonthToMonth();
    ActivityReport.showMinNumberOfWatchedMoviesByUsersWithMoreThan100friends();
  }
}
