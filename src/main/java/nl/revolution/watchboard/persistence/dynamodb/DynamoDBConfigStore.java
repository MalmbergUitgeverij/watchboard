package nl.revolution.watchboard.persistence.dynamodb;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import nl.revolution.watchboard.Config;
import nl.revolution.watchboard.data.Dashboard;
import nl.revolution.watchboard.persistence.DashboardConfig;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class DynamoDBConfigStore implements DashboardConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DynamoDBConfigStore.class);

    public static final String DASHBOARD_CONFIG_DOCUMENT_KEY = "dashboards";
    public static final String ID_KEY = "id";
    public static final String UPDATED_AT_KEY = "updatedAt";
    private Table table;

    public DynamoDBConfigStore(JSONObject config) {
        String accessKeyId = (String)config.get(Config.AWS_ACCESS_KEY_ID);
        String secretKeyId = (String)config.get(Config.AWS_SECRET_KEY_ID);
        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretKeyId);
        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(new StaticCredentialsProvider(awsCredentials));
        dynamoDBClient.setRegion(Region.getRegion(Regions.fromName((String)config.get(Config.AWS_REGION))));
        DynamoDB dynamoDB = new DynamoDB(dynamoDBClient);
        table = dynamoDB.getTable((String)config.get(Config.AWS_DYNAMODB_TABLE_NAME));
    }

    private Item readConfigAsItem() {
        return table.getItem(new PrimaryKey().addComponent(ID_KEY, DASHBOARD_CONFIG_DOCUMENT_KEY));
    }

    @Override
    public String readConfig() {
        Item config = readConfigAsItem();
        if (config == null) {
            return null;
        }
        return config.toJSONPretty();
    }

    @Override
    public void updateConfig(String dashboardConfig, String tsPreviousUpdate) {
        JSONObject configJo;
        try {
            configJo = (JSONObject)new JSONParser().parse(dashboardConfig);
        } catch (ParseException e) {
            LOG.error("Error parsing config: ", e);
            throw new RuntimeException("Error parsing config");
        }
        String validationResult = Dashboard.validateConfig(configJo);
        if (StringUtils.isNotEmpty(validationResult)) {
            LOG.error("Error while validating config:\n" + validationResult);
            throw new RuntimeException("Error validating config: " + validationResult);
        }

        Item oldConfig = table.getItem(new PrimaryKey().addComponent(ID_KEY, DASHBOARD_CONFIG_DOCUMENT_KEY));
        String tsUpdateFromDB = oldConfig.getString("updatedAt");

        if (!tsUpdateFromDB.equals(tsPreviousUpdate)) {
            LOG.error("Error while updating dashboard config: timestamp from request ('" + tsPreviousUpdate + "') does not match " +
                    "timestamp from database ('" + tsUpdateFromDB + "').");
            throw new RuntimeException("Update failed: configuration was saved by another user while you were editing it. " +
                    "Refresh the page and perform your change again to continue.");
        }

        String currentTime = LocalDateTime.now().toString();
        String newId = DASHBOARD_CONFIG_DOCUMENT_KEY + "-" + currentTime;
        oldConfig.withKeyComponent(ID_KEY, newId);
        writeConfig(oldConfig);
        LOG.info("Backed up old config version with suffix '" + currentTime + "'.");

        Item newConfig = Item.fromJSON(dashboardConfig);
        newConfig.with(UPDATED_AT_KEY, currentTime);
        newConfig.withKeyComponent(ID_KEY, DASHBOARD_CONFIG_DOCUMENT_KEY);
        writeConfig(newConfig);
    }

    @Override
    public String getLastUpdated() {
        Item configItem = readConfigAsItem();
        return String.valueOf(configItem.get(UPDATED_AT_KEY));
    }

    public void writeConfig(Item dashboardConfig) {
        if (!dashboardConfig.isPresent(UPDATED_AT_KEY)) {
            dashboardConfig.with(UPDATED_AT_KEY, LocalDateTime.now().toString());
        }
        table.putItem(dashboardConfig);
        LOG.info("Wrote config.");
    }

    public void writeConfig(String dashboardConfig) {
        writeConfig(Item.fromJSON(dashboardConfig));
    }

}
