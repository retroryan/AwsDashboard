package models;

import org.codehaus.jackson.JsonNode;

public class Ec2Req {

    private String region;
    private String imageId;
    private String size;
    private String userData;
    private int numInstances;

    public Ec2Req() {
    }

    public Ec2Req(String region, String imageId, String size) {
        this.region = region;
        this.imageId = imageId;
        this.size = size;
    }

    public Ec2Req(String region, String imageId, String size, String userData, int numInstances) {
        this.region = region;
        this.imageId = imageId;
        this.size = size;
        this.userData = userData;
        this.numInstances = numInstances;
    }

    public static Ec2Req getEc2Req(JsonNode ec2ReqJson) {
        String region = ec2ReqJson.findPath("region").getTextValue();
        String imageId = ec2ReqJson.findPath("imageId").getTextValue();
        String size = ec2ReqJson.findPath("size").getTextValue();
        String userData = ec2ReqJson.findPath("userData").getTextValue();
        String numInstances1 = ec2ReqJson.findPath("numInstances").getTextValue();
        System.out.println("numInstances1 = " + numInstances1);
        int numInstances = Integer.parseInt(numInstances1);
        Ec2Req ec2Req = new Ec2Req(region, imageId, size, userData, numInstances);

        return ec2Req;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public int getNumInstances() {
        return numInstances;
    }

    public void setNumInstances(int numInstances) {
        this.numInstances = numInstances;
    }
}
