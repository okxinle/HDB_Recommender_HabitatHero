package habitathero.GeoSpatialAnalysis.src;

public class TransportLineGeoJsonDownloader {
    
    public void downloadGeoJson(String datasetid, String localfilepath) {
        DataGovAPIHandler.getInstance().pollDownloadAndSave(datasetid, localfilepath);
    }
}

