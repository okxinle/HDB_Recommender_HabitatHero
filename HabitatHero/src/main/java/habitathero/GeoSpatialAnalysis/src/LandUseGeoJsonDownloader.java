package habitathero.GeoSpatialAnalysis.src;

public class LandUseGeoJsonDownloader {
    private static LandUseGeoJsonDownloader instance;

    private LandUseGeoJsonDownloader() {
    }

    public static LandUseGeoJsonDownloader getInstance() {
        if (instance == null) {
            instance = new LandUseGeoJsonDownloader();
        }
        return instance;
    }

    public Boolean downloadGeoJson(String datasetid, String localfilepath){
        return DataGovAPIHandler.getInstance().pollDownloadAndSave(datasetid, localfilepath);
    }

    public Boolean forceDownloadGeoJson(String datasetid, String localfilepath){
        return DataGovAPIHandler.getInstance().pollForcedDownloadAndSave(datasetid, localfilepath);
    }

    
}
