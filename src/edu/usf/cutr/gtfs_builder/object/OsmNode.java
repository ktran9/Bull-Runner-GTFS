/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.usf.cutr.gtfs_builder.object;

import java.util.HashSet;
import java.util.Hashtable;

/**
 *
 * @author ktran
 */
public class OsmNode {
    private String latitude, longitude, route, distTraveled;

    private Hashtable<String, String> tags = new Hashtable<String, String>();

    private HashSet<Integer> sequenceNumber = new HashSet<Integer>();

    public OsmNode(String lat, String lon){
        latitude = lat;
        longitude = lon;
    }
    
    public String getLat(){
        return latitude;
    }

    public String getLon(){
        return longitude;
    }

    public void setLat(String lat){
        latitude = lat;
    }

    public void setLon(String lon){
        longitude = lon;
    }

    public void setRoute(String r){
        route = r;
    }

    public String getRoute(){
        return this.getTags().get("gtfs_shape_id");
    }

    public String getStopId(){
        return this.getTags().get("gtfs_stop_id");
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof OsmNode) {
            OsmNode n = (OsmNode)o;
            return (this.latitude.equals(n.getLat()) && this.longitude.equals(n.getLon()) ); //&& this.getRoute().equals(n.getRoute()));
        }
        return false;
    }

    public HashSet<Integer> getSequenceNumber(){
        return sequenceNumber;
    }

    public void addSequenceNumber(Integer i){
        sequenceNumber.add(i);
    }

    @Override
    public int hashCode(){
        String id = this.getLat().concat(this.getLon());
        return id.hashCode();
    }

    @Override
    public String toString(){
        return route+" ("+getLat()+", "+getLon()+")";
    }

    /**
     * @return the tags
     */
    public Hashtable<String, String> getTags() {
        return tags;
    }

    /**
     * @param tags the tags to set
     */
    public void setTags(Hashtable<String, String> tags) {
        this.tags = tags;
    }

    public void addTag(String k, String v){
        this.tags.put(k, v);
    }

    /**
     * @return the distTraveled
     */
    public String getDistTraveled() {
        return distTraveled;
    }

    /**
     * @param distTraveled the distTraveled to set
     */
    public void setDistTraveled(String distTraveled) {
        this.distTraveled = distTraveled;
    }
}
