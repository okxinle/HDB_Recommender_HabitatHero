package habitathero.GeoSpatialAnalysis.src;

public class TransportLineGeoJsonDownloader {
    private static TransportLineGeoJsonDownloader instance;

    public static TransportLineGeoJsonDownloader getInstance(){
        if(instance == null){
            instance = new TransportLineGeoJsonDownloader();
        }
        return instance;
    }
    
    public Boolean downloadGeoJson(String datasetid, String localfilepath) {
        return DataGovAPIHandler.getInstance().pollDownloadAndSave(datasetid, localfilepath);
    }
}
