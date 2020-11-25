package logic;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import connection.DatabaseConnection;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActivityReport {

  public static final Logger LOGGER = Logger.getLogger(ActivityReport.class);
  public static final MongoDatabase MONGO_DATABASE = DatabaseConnection.getMongoDatabase();

  private static final String USERS = "users";
  private static final String MESSAGES = "messages";
  private static final String FRIENDSHIPS = "friendships";
  public static final String NUMBER_OF_MOVIES = "numberOfMovies";
  public static final String MONTH = "month";
  public static final String SIGNED_DATE = "$date";
  public static final String SIGNED_YEAR = "$year";
  public static final String SIGNED_MONTH = "$month";
  public static final String SIGNED_SIZE = "$size";

  private static final MongoCollection<Document> messagesCollection =
      MONGO_DATABASE.getCollection(MESSAGES);
  private static final MongoCollection<Document> friendshipsCollection =
      MONGO_DATABASE.getCollection(FRIENDSHIPS);
  private static final MongoCollection<Document> usersCollection =
      MONGO_DATABASE.getCollection(USERS);

  private ActivityReport() {}

  public static void createIndexes() {
    LOGGER.info("Creating indexes...");
    usersCollection.createIndex(Indexes.ascending("id"));
    messagesCollection.createIndex(Indexes.ascending("date"));
    friendshipsCollection.createIndex(Indexes.ascending("userId"));
    friendshipsCollection.createIndex(Indexes.ascending("date"));
    LOGGER.info("Indexes created!");
  }

  public static void showAverageMessagesByDayOfWeek() {

    List<Document> averageMessagesByDayOfWeek =
        messagesCollection
            .aggregate(
                Arrays.asList(
                    Aggregates.project(
                        Projections.fields(
                            Projections.include("id"),
                            Projections.computed("year", new Document(SIGNED_YEAR, SIGNED_DATE)),
                            Projections.computed(MONTH, new Document(SIGNED_MONTH, SIGNED_DATE)),
                            Projections.computed("week", new Document("$week", SIGNED_DATE)),
                            Projections.computed(
                                "dayOfWeek", new Document("$dayOfWeek", SIGNED_DATE)),
                            Projections.excludeId())),
                    Aggregates.group(
                        new Document("year", SIGNED_YEAR)
                            .append(MONTH, SIGNED_MONTH)
                            .append("week", "$week")
                            .append("dayOfWeek", "$dayOfWeek"),
                        Accumulators.sum("totalMessagesPerWeekDay", 1)),
                    Aggregates.sort(Sorts.ascending("_id")),
                    Aggregates.group(
                        "$_id.dayOfWeek",
                        Accumulators.avg(
                            "averageNumberOfMessagesByWeekDay", "$totalMessagesPerWeekDay")),
                    Aggregates.sort(Sorts.ascending("_id"))))
            .allowDiskUse(true)
            .map(Document::new)
            .into(new ArrayList<>());

    averageMessagesByDayOfWeek.forEach(
        document ->
            LOGGER.info(
                "Average number of messages by day of week - "
                    + " Day of the week: "
                    + document.get("_id")
                    + ", Average number of messages: "
                    + document.get("averageNumberOfMessagesByWeekDay")));
  }

  public static void showMaxNumberOfNewFriendshipsFromMonthToMonth() {

    List<Document> maxNumberOfNewFriendships =
        friendshipsCollection
            .aggregate(
                Arrays.asList(
                    Aggregates.project(
                        Projections.fields(
                            Projections.computed("year", new Document(SIGNED_YEAR, SIGNED_DATE)),
                            Projections.computed(MONTH, new Document(SIGNED_MONTH, SIGNED_DATE)),
                            Projections.computed(
                                FRIENDSHIPS, new Document(SIGNED_SIZE, "$friends")),
                            Projections.excludeId())),
                    Aggregates.group(
                        new Document("year", SIGNED_YEAR).append(MONTH, SIGNED_MONTH),
                        Accumulators.max("maxFriendships", "$friendships")),
                    Aggregates.sort(Sorts.ascending("_id"))))
            .allowDiskUse(true)
            .map(Document::new)
            .into(new ArrayList<>());

    maxNumberOfNewFriendships.forEach(
        document -> {
          Document ids = (Document) document.get("_id");
          LOGGER.info(
              "Max number of new friendships from month to month - "
                  + " Year: "
                  + ids.get("year")
                  + ",Month: "
                  + ids.get(MONTH)
                  + ", Max number of new friendships: "
                  + document.get("maxFriendships"));
        });
  }

  public static void showMinNumberOfWatchedMoviesByUsersWithMoreThan100friends() {

    List<Document> minNumOfWatchedMovies =
        usersCollection
            .aggregate(
                Arrays.asList(
                    Aggregates.lookup(FRIENDSHIPS, "id", "userId", "copiedFriends"),
                    Aggregates.unwind("$copiedFriends"),
                    Aggregates.project(
                        Projections.fields(
                            Projections.include("id", "name", "surname"),
                            Projections.computed(
                                NUMBER_OF_MOVIES, new Document(SIGNED_SIZE, "$movies")),
                            Projections.computed(
                                NUMBER_OF_MOVIES,
                                new Document(SIGNED_SIZE, "$copiedFriends.friends")),
                            Projections.excludeId())),
                    Aggregates.match(Filters.gt("numberOfFriends", 100)),
                    Aggregates.sort(Sorts.ascending(NUMBER_OF_MOVIES))))
            .allowDiskUse(true)
            .map(Document::new)
            .into(new ArrayList<>());

    minNumOfWatchedMovies.stream()
        .limit(500)
        .forEach(
            document ->
                LOGGER.info(
                    "Min number of watched movies by users with more than 100 friends - "
                        + " Id: "
                        + document.get("id")
                        + ",Name: "
                        + document.get("name")
                        + ", Surname: "
                        + document.get("surname")
                        + ", Number of Movies: "
                        + document.get(NUMBER_OF_MOVIES)
                        + ", Number of Friends: "
                        + document.get("numberOfFriends")));
  }
}
