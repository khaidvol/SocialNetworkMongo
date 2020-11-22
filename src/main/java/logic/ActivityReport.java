package logic;

import com.mongodb.client.MongoClient;
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

  public static final MongoClient MONGO_CLIENT = DatabaseConnection.getMongoClient();
  public static final MongoDatabase MONGO_DATABASE = DatabaseConnection.getMongoDatabase();

  private static final String USERS_COLLECTION = "users";
  private static final String MESSAGES_COLLECTION = "messages";
  private static final String FRIENDSHIPS_COLLECTION = "friendships";

  private static MongoCollection<Document> messagesCollection =
      MONGO_DATABASE.getCollection(MESSAGES_COLLECTION);
  private static MongoCollection<Document> friendshipsCollection =
      MONGO_DATABASE.getCollection(FRIENDSHIPS_COLLECTION);
  private static MongoCollection<Document> usersCollection =
      MONGO_DATABASE.getCollection(USERS_COLLECTION);

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
                            Projections.computed("year", new Document("$year", "$date")),
                            Projections.computed("month", new Document("$month", "$date")),
                            Projections.computed("week", new Document("$week", "$date")),
                            Projections.computed("dayOfWeek", new Document("$dayOfWeek", "$date")),
                            Projections.excludeId())),
                    Aggregates.group(
                        new Document("year", "$year")
                            .append("month", "$month")
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
                            Projections.computed("year", new Document("$year", "$date")),
                            Projections.computed("month", new Document("$month", "$date")),
                            Projections.computed("friendships", new Document("$size", "$friends")),
                            Projections.excludeId())),
                    Aggregates.group(
                        new Document("year", "$year").append("month", "$month"),
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
                  + ids.get("month")
                  + ", Max number of new friendships: "
                  + document.get("maxFriendships"));
        });
  }

  public static void showMinNumberOfWatchedMoviesByUsersWithMoreThan100friends() {

    List<Document> minNumOfWatchedMovies =
        usersCollection
            .aggregate(
                Arrays.asList(
                    Aggregates.lookup("friendships", "id", "userId", "copiedFriends"),
                    Aggregates.unwind("$copiedFriends"),
                    Aggregates.project(
                        Projections.fields(
                            Projections.include("id", "name", "surname"),
                            Projections.computed(
                                "numberOfMovies", new Document("$size", "$movies")),
                            Projections.computed(
                                "numberOfFriends", new Document("$size", "$copiedFriends.friends")),
                            Projections.excludeId())),
                    Aggregates.match(Filters.gt("numberOfFriends", 100)),
                    Aggregates.sort(Sorts.ascending("numberOfMovies"))))
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
                        + document.get("numberOfMovies")
                        + ", Number of Friends: "
                        + document.get("numberOfFriends")));
  }
}
