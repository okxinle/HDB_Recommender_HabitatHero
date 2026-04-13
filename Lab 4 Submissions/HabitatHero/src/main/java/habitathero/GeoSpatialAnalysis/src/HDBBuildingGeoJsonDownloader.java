package habitathero.GeoSpatialAnalysis.src;

public class HDBBuildingGeoJsonDownloader {
    private static HDBBuildingGeoJsonDownloader instance;

    private HDBBuildingGeoJsonDownloader() {
    }

    public static HDBBuildingGeoJsonDownloader getInstance() {
        if (instance == null) {
            instance = new HDBBuildingGeoJsonDownloader();
        }
        return instance;
    }

    public Boolean downloadGeoJson(String datasetid, String localfilepath) {
        return DataGovAPIHandler.getInstance().pollDownloadAndSave(datasetid, localfilepath);
    }

    public Boolean forceDownloadGeoJson(String datasetid, String localfilepath){
        return DataGovAPIHandler.getInstance().pollForcedDownloadAndSave(datasetid, localfilepath);
    }
}
