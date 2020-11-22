package connection;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConnection {

  private static final Logger logger = Logger.getLogger(DatabaseConnection.class);
  private static final MongoClient mongoClient;
  private static final MongoDatabase mongoDatabase;
  private static final MongoCollection<Document> mongoCollection;

  static {
    Properties propMongo = new Properties();
    try (InputStream input = new FileInputStream("src/main/resources/mongodb.properties")) {
      propMongo.load(input);
    } catch (IOException e) {
      logger.error(e);
    }

    MongoCredential credential =
        MongoCredential.createScramSha1Credential(
            propMongo.getProperty("mongo.user"),
            propMongo.getProperty("mongo.admindb"),
            propMongo.getProperty("mongo.password").toCharArray());

    MongoClientSettings settings = MongoClientSettings.builder().credential(credential).build();
    mongoClient = MongoClients.create(settings);
    mongoDatabase = mongoClient.getDatabase(propMongo.getProperty("mongo.database_for_task"));
    mongoCollection =
        mongoDatabase.getCollection(propMongo.getProperty("mongo.collection_for_task"));
  }

  private DatabaseConnection() {}

  public static MongoClient getMongoClient() {
    return mongoClient;
  }

  public static MongoDatabase getMongoDatabase() {
    return mongoDatabase;
  }

  public static MongoCollection<Document> getMongoCollection() {
    return mongoCollection;
  }
}
