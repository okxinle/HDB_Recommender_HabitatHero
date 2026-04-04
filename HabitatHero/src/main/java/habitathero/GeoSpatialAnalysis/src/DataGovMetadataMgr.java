package habitathero.GeoSpatialAnalysis.src;

import org.json.JSONObject;

// Service to manage metadata of dataset from DataGov and dataset metadata SQL
public class DataGovMetadataMgr {
    private static DataGovMetadataMgr instance;
    private DatasetMetadataSQLHandler datasetMetadataSQLHandler; 
    private DatasetMetadataFetcher datasetMetadataFetcher;

    //Dataset id for api retrival
    //Landuse = d_90d86daa5bfaa371668b84fa5f01424f
    //transportline = d_222bfc84eb86c7c11994d02f8939da8d
    //hdbbuilding = d_16b157c52ed637edd6ba1232e026258d

    public static DataGovMetadataMgr getInstance(){
        if(instance == null){
            instance = new DataGovMetadataMgr();
        }
        return instance;
    }

    private DataGovMetadataMgr(){
        datasetMetadataSQLHandler = DatasetMetadataSQLHandler.getInstance();
        datasetMetadataFetcher = DatasetMetadataFetcher.getInstance();
    }

    public JSONObject fetchAPIMetadata(String dataset_id) {
        return datasetMetadataFetcher.getMetadata(dataset_id);
    }

    public void createSQLTable(){
        datasetMetadataSQLHandler.createSQLTable();
    }

    public void upsertSQLMetadata(JSONObject metadata){
        datasetMetadataSQLHandler.upsertSQLMetadata(metadata);
    }

    public JSONObject retrieveSQLMetadata(String datasetid){
        return datasetMetadataSQLHandler.retrieveSQLMetadata(datasetid);
    }
}
