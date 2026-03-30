public class LandUseGeoJsonDownloader {

    public void downloadGeoJson(String datasetid, String localfilepath){
        DataGovAPIHandler.getInstance().pollDownloadAndSave(datasetid, localfilepath);
    }
}
