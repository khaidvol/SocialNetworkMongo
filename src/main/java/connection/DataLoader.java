package connection;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import model.*;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.util.List;
import java.util.stream.Collectors;

public class DataLoader {

  private static final Logger LOGGER = Logger.getLogger(DataLoader.class);
  public static final MongoClient MONGO_CLIENT = DatabaseConnection.getMongoClient();
  public static final MongoDatabase MONGO_DATABASE = DatabaseConnection.getMongoDatabase();

  private static final String BATCH_START = "Storing batch #%s/%s to the mongoDB...";
  private static final String BATCH_FINISH = "Batch #%s/%s stored successfully!";
  private static final String DELIMITER = "-----------------------------------------";

  private static final String USERS_COLLECTION = "users";
  private static final String MOVIES_COLLECTION = "movies";
  private static final String AUDIO_TRACKS_COLLECTION = "audio_tracks";
  private static final String MESSAGES_COLLECTION = "messages";
  private static final String FRIENDSHIPS_COLLECTION = "friendships";

  public static final long BATCH_SIZE = 100_000;

  public static final long NUMBER_OF_USERS = 1_000_000;
  public static final long NUMBER_OF_MOVIES = 10_000_000;
  public static final long NUMBER_AUDIO_TRACKS = 10_000_000;
  public static final long NUMBER_MESSAGES = 10_000_000;
  public static final long NUMBER_FRIENDSHIPS = 10_000_000;

  private DataLoader() {}

  public static void executeLoading() {
    loadMovies(BATCH_SIZE, NUMBER_OF_MOVIES);
    loadAudioTracks(BATCH_SIZE, NUMBER_AUDIO_TRACKS);
    loadUsers(BATCH_SIZE, NUMBER_OF_USERS, NUMBER_OF_MOVIES, NUMBER_AUDIO_TRACKS);
    loadFriendships(BATCH_SIZE, NUMBER_FRIENDSHIPS, NUMBER_OF_USERS);
    loadMessages(BATCH_SIZE, NUMBER_MESSAGES, NUMBER_OF_USERS);
  }

  public static void loadUsers(
      long batchSize, long numberOfUsers, long maxNumberOfMovies, long maxNumberOfAudioTracks) {

    MongoCollection<Document> collection = MONGO_DATABASE.getCollection(USERS_COLLECTION);

    long numberOfBatches = numberOfUsers / batchSize;
    long startUsersId = 0;
    long endUsersId = batchSize;

    for (long i = 0; i < numberOfBatches; i++) {
      // generate movies
      List<User> users =
          DataGenerator.generateUsers(
              startUsersId, endUsersId, maxNumberOfMovies, maxNumberOfAudioTracks);
      // map movies to documents list
      List<Document> usersDocs =
          users.stream()
              .map(
                  user ->
                      new Document("id", user.getId())
                          .append("name", user.getName())
                          .append("surname", user.getSurname())
                          .append("birthday", user.getBirthdate())
                          .append("movies", user.getMovies())
                          .append("audio_tracks", user.getAudioTracks()))
              .collect(Collectors.toList());

      // load users to mongodb
      LOGGER.info(String.format(BATCH_START, i + 1, numberOfBatches));
      collection.insertMany(usersDocs);
      LOGGER.info(String.format(BATCH_FINISH, i + 1, numberOfBatches));
      LOGGER.info(DELIMITER);

      startUsersId = endUsersId;
      endUsersId += batchSize;
    }
  }

  public static void loadMovies(long batchSize, long numberOfMovies) {

    MongoCollection<Document> collection = MONGO_DATABASE.getCollection(MOVIES_COLLECTION);

    long numberOfBatches = numberOfMovies / batchSize;
    long startMoviesId = 0;
    long endMoviesId = batchSize;

    for (long i = 0; i < numberOfBatches; i++) {
      // generate movies
      List<Movie> movies = DataGenerator.generateMovies(startMoviesId, endMoviesId);
      // map movies to documents list
      List<Document> moviesDocs =
          movies.stream()
              .map(
                  movie ->
                      new Document("id", movie.getId())
                          .append("title", movie.getTitle())
                          .append("country", movie.getCountry())
                          .append("year", movie.getYear()))
              .collect(Collectors.toList());

      // load movies to mongodb
      LOGGER.info(String.format(BATCH_START, i + 1, numberOfBatches));
      collection.insertMany(moviesDocs);
      LOGGER.info(String.format(BATCH_FINISH, i + 1, numberOfBatches));
      LOGGER.info(DELIMITER);

      // increment ids for next batch
      startMoviesId = endMoviesId;
      endMoviesId += batchSize;
    }
  }

  public static void loadAudioTracks(long batchSize, long numberOfAudioTracks) {

    MongoCollection<Document> collection = MONGO_DATABASE.getCollection(AUDIO_TRACKS_COLLECTION);

    long numberOfBatches = numberOfAudioTracks / batchSize;

    long startAudioTracksId = 0;
    long endAudioTracksId = batchSize;

    for (long i = 0; i < numberOfBatches; i++) {
      // generate movies
      List<AudioTrack> audioTracks =
          DataGenerator.generateAudioTracks(startAudioTracksId, endAudioTracksId);
      // map movies to documents list
      List<Document> audioTracksDocs =
          audioTracks.stream()
              .map(
                  audioTrack ->
                      new Document("id", audioTrack.getId())
                          .append("title", audioTrack.getTitle())
                          .append("author", audioTrack.getAuthor())
                          .append("album", audioTrack.getAlbum())
                          .append("year", audioTrack.getYear()))
              .collect(Collectors.toList());

      // load audio tracks to mongodb
      LOGGER.info(String.format(BATCH_START, i + 1, numberOfBatches));
      collection.insertMany(audioTracksDocs);
      LOGGER.info(String.format(BATCH_FINISH, i + 1, numberOfBatches));
      LOGGER.info(DELIMITER);

      startAudioTracksId = endAudioTracksId;
      endAudioTracksId += batchSize;
    }
  }

  public static void loadMessages(long batchSize, long numberOfMessages, long maxNumberOfUsers) {

    MongoCollection<Document> collection = MONGO_DATABASE.getCollection(MESSAGES_COLLECTION);

    long startMessagesId = 0;
    long endMessagesId = batchSize;

    long numberOfBatches = numberOfMessages / batchSize;
    for (long i = 0; i < numberOfBatches; i++) {
      // generate movies
      List<Message> messages =
          DataGenerator.generateMessages(startMessagesId, endMessagesId, maxNumberOfUsers);
      // map movies to documents list
      List<Document> messagesDocs =
          messages.stream()
              .map(
                  message ->
                      new Document("id", message.getId())
                          .append("sender", message.getSenderId())
                          .append("recipient", message.getRecipientId())
                          .append("text", message.getText())
                          .append("date", message.getDate()))
              .collect(Collectors.toList());

      // load movies to mongodb
      LOGGER.info(String.format(BATCH_START, i + 1, numberOfBatches));
      collection.insertMany(messagesDocs);
      LOGGER.info(String.format(BATCH_FINISH, i + 1, numberOfBatches));
      LOGGER.info(DELIMITER);

      startMessagesId = endMessagesId;
      endMessagesId += batchSize;
    }
  }

  public static void loadFriendships(
      long batchSize, long numberOfFriendships, long maxNumberOfUsers) {

    MongoCollection<Document> collection = MONGO_DATABASE.getCollection(FRIENDSHIPS_COLLECTION);

    long startFriendshipsId = 0;
    long endFriendshipsId = batchSize;

    long numberOfBatches = numberOfFriendships / batchSize;
    for (long i = 0; i < numberOfBatches; i++) {
      // generate friendships
      List<Friendship> friendships =
          DataGenerator.generateFriendships(startFriendshipsId, endFriendshipsId, maxNumberOfUsers);
      // map friendships to documents list
      List<Document> friendshipsDocs =
          friendships.stream()
              .map(
                  friendship ->
                      new Document("userId", friendship.getUserId())
                          .append("date", friendship.getDate())
                          .append("friends", friendship.getFriendsIds()))
              .collect(Collectors.toList());

      // load friendships to mongodb
      LOGGER.info(String.format(BATCH_START, i + 1, numberOfBatches));
      collection.insertMany(friendshipsDocs);
      LOGGER.info(String.format(BATCH_FINISH, i + 1, numberOfBatches));
      LOGGER.info(DELIMITER);

      startFriendshipsId = endFriendshipsId;
      endFriendshipsId += batchSize;
    }
  }
}
