/**
Copyright 2010 University of South Florida

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

**/
package edu.usf.cutr.gtfs_builder.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import edu.usf.cutr.gtfs_builder.object.OperatorInfo;
import edu.usf.cutr.gtfs_builder.object.OsmNode;
import edu.usf.cutr.gtfs_builder.object.Route;
import edu.usf.cutr.gtfs_builder.object.Stop;
import edu.usf.cutr.gtfs_builder.tools.OsmFormatter;

public class GTFSReadIn {
    private List<Stop> stops;
    private final String ROUTE_KEY = "route_ref";
    private final String NTD_ID_KEY = "ntd_id";
    private static Hashtable<String, Route> allRoutes;

    public static ArrayList<OsmNode> readLatLon(String fname){
        ArrayList<OsmNode> results = new ArrayList<OsmNode>();
        String thisLine;
        String [] elements;
        int latKey=-1, lonKey=-1;
        try {
            BufferedReader br = new BufferedReader(new FileReader(fname));
            boolean isFirstLine = true;
            Hashtable keysIndex = new Hashtable();
            while ((thisLine = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    thisLine = thisLine.replace("\"", "");
                    String[] keys = thisLine.split(",");
                    for(int i=0; i<keys.length; i++){
                        if(keys[i].contains("lat")) latKey = i;
                        else if(keys[i].contains("lon")) lonKey = i;
                        else {
                            String t = "gtfs_"+keys[i];
                            keysIndex.put(t, i);
                        }
                    }
//                    System.out.println(stopIdKey+","+stopNameKey+","+stopLatKey+","+stopLonKey);
                }
                else {
                    boolean lastIndexEmpty=false;
                    thisLine = thisLine.trim();

                    if(thisLine.contains("\"")) {
                         String[] temp = thisLine.split("\"");
                         for(int x=0; x<temp.length; x++){
                             if(x%2==1) temp[x] = temp[x].replace(",", "");
                         }
                         thisLine = "";
                         for(int x=0; x<temp.length; x++){
                             thisLine = thisLine + temp[x];
                         }
                    }
                    elements = thisLine.split(",");
                    if(thisLine.charAt(thisLine.length()-1)==',') lastIndexEmpty=true;
                    //add leading 0's to gtfs_id
                    OsmNode n = new OsmNode(elements[latKey],elements[lonKey]);
                    HashSet<String> keys = new HashSet<String>();
                    keys.addAll(keysIndex.keySet());
                    Iterator it = keys.iterator();
                    try {
                        while(it.hasNext()){
                            String k = (String)it.next();
                            String v = null;
                            int tempIndex = (Integer)keysIndex.get(k);
                            if(tempIndex<elements.length) v = elements[(Integer)keysIndex.get(k)];
                            if ((v!=null) && (!v.equals(""))) n.addTag(k, v);
                        }
                    } catch(Exception e){
                        System.out.println("Error occurred! Please check your GTFS input files");
                        System.out.println(e.toString());
                        System.exit(0);
                    }
                    
                    results.add(n);
//                    System.out.println(thisLine);
                }
            }
        }
        catch (IOException e) {
            System.err.println("Error: " + e);
        }
        return results;
    }

    public static Hashtable<String, ArrayList<String>> readStopTime(String fname){
        Hashtable<String, ArrayList<String>> results = new Hashtable<String, ArrayList<String>>();
        results.put("0", new ArrayList<String>());
        results.put("1", new ArrayList<String>());
        results.put("2", new ArrayList<String>());
        results.put("3", new ArrayList<String>());
        results.put("4", new ArrayList<String>());
        Hashtable<String, String> tripIdToShapeId = new Hashtable<String, String>();
        tripIdToShapeId.put("1", "0");
        tripIdToShapeId.put("3", "1");
        tripIdToShapeId.put("5", "2");
        tripIdToShapeId.put("8", "3");
        tripIdToShapeId.put("11", "4");
        String thisLine;
        String [] elements;
        int tripKey=-1, stopKey=-1;
        try {
            BufferedReader br = new BufferedReader(new FileReader(fname));
            boolean isFirstLine = true;
            Hashtable keysIndex = new Hashtable();
            while ((thisLine = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    thisLine = thisLine.replace("\"", "");
                    String[] keys = thisLine.split(",");
                    for(int i=0; i<keys.length; i++){
                        if(keys[i].contains("trip_id")) tripKey = i;
                        else if(keys[i].contains("stop_id")) stopKey = i;
                        else {
                            String t = "gtfs_"+keys[i];
                            keysIndex.put(t, i);
                        }
                    }
//                    System.out.println(stopIdKey+","+stopNameKey+","+stopLatKey+","+stopLonKey);
                }
                else {
                    boolean lastIndexEmpty=false;
                    thisLine = thisLine.trim();

                    if(thisLine.contains("\"")) {
                         String[] temp = thisLine.split("\"");
                         for(int x=0; x<temp.length; x++){
                             if(x%2==1) temp[x] = temp[x].replace(",", "");
                         }
                         thisLine = "";
                         for(int x=0; x<temp.length; x++){
                             thisLine = thisLine + temp[x];
                         }
                    }
                    elements = thisLine.split(",");
                    if(thisLine.charAt(thisLine.length()-1)==',') lastIndexEmpty=true;
                    //add leading 0's to gtfs_id
                    String shapeId = tripIdToShapeId.get(elements[tripKey]);
                    if(shapeId!=null){
                        ArrayList<String> sequence = results.get(shapeId);
                        sequence.add(elements[stopKey]);
                    }
//                    System.out.println(thisLine);
                }
            }
        }
        catch (IOException e) {
            System.err.println("Error: " + e);
        }
        return results;
    }

    public List<Stop> readBusStop(String fName, String agencyName, String routes_fName, String trips_fName, String stop_times_fName){
        Hashtable<String, HashSet<Route>> stopIDs = new Hashtable<String, HashSet<Route>>();
        Hashtable id = matchRouteToStop(routes_fName, trips_fName, stop_times_fName);
        stopIDs.putAll(id);
        
        String thisLine;
        String [] elements;
        int stopIdKey=-1, stopNameKey=-1, stopLatKey=-1, stopLonKey=-1;
        try {
            BufferedReader br = new BufferedReader(new FileReader(fName));
            boolean isFirstLine = true;
            Hashtable keysIndex = new Hashtable();
            while ((thisLine = br.readLine()) != null) { 
                if (isFirstLine) {
                    isFirstLine = false;
                    OperatorInfo.setGtfsFields(thisLine);
                    thisLine = thisLine.replace("\"", "");
                    String[] keys = thisLine.split(",");
                    for(int i=0; i<keys.length; i++){
                        if(keys[i].equals("stop_id")) stopIdKey = i;
                        else if(keys[i].equals("stop_name")) stopNameKey = i;
                        else if(keys[i].equals("stop_lat")) stopLatKey = i;
                        else if(keys[i].equals("stop_lon")) stopLonKey = i;
                        // gtfs stop_url is mapped to source_ref tag in OSM
                        else if(keys[i].equals("stop_url")){
                            keysIndex.put("source_ref", i);
                        }
                        else {
                            String t = "gtfs_"+keys[i];
                            keysIndex.put(t, i);
                        }
                    }
//                    System.out.println(stopIdKey+","+stopNameKey+","+stopLatKey+","+stopLonKey);
                }
                else {
                    boolean lastIndexEmpty=false;
                    thisLine = thisLine.trim();

                    if(thisLine.contains("\"")) {
                         String[] temp = thisLine.split("\"");
                         for(int x=0; x<temp.length; x++){
                             if(x%2==1) temp[x] = temp[x].replace(",", "");
                         }
                         thisLine = "";
                         for(int x=0; x<temp.length; x++){
                             thisLine = thisLine + temp[x];
                         }
                    }
                    elements = thisLine.split(",");
                    if(thisLine.charAt(thisLine.length()-1)==',') lastIndexEmpty=true;
                    //add leading 0's to gtfs_id
                    String tempStopId = OsmFormatter.getValidBusStopId(elements[stopIdKey]);
                    Stop s = new Stop(tempStopId, agencyName, elements[stopNameKey],elements[stopLatKey],elements[stopLonKey]);
                    HashSet<String> keys = new HashSet<String>();
                    keys.addAll(keysIndex.keySet());
                    Iterator it = keys.iterator();
                    try {
                        while(it.hasNext()){
                            String k = (String)it.next();
                            String v = null;
                            if(!lastIndexEmpty) v = elements[(Integer)keysIndex.get(k)];
                            if ((v!=null) && (!v.equals(""))) s.addTag(k, v);
                        }
                        s.addTag(NTD_ID_KEY, OperatorInfo.getNTDID());
                    } catch(Exception e){
                        System.out.println("Error occurred! Please check your GTFS input files");
                        System.out.println(e.toString());
                        System.exit(0);
                    }
                    String r = getRoutesInTextByBusStop((HashSet<Route>)stopIDs.get(tempStopId));
                    if (!r.isEmpty()) s.addTag(ROUTE_KEY, r);
                    HashSet<Route> asdf = (HashSet<Route>)stopIDs.get(tempStopId);
                    if(asdf!=null)s.addRoutes((HashSet<Route>)stopIDs.get(tempStopId));
                    stops.add(s);
//                    System.out.println(thisLine);
                }
            } 
        }
        catch (IOException e) {
            System.err.println("Error: " + e);
        }
        return stops;
    }

    public Hashtable<String, Route> readRoutes(String routes_fName){
        Hashtable<String, Route> routes = new Hashtable<String, Route>();
        String thisLine;
        String [] elements;
        int routeIdKey=-1, routeShortNameKey=-1;
        try {
            BufferedReader br = new BufferedReader(new FileReader(routes_fName));
            boolean isFirstLine = true;
            Hashtable keysIndex = new Hashtable();
            while ((thisLine = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    thisLine = thisLine.replace("\"", "");
                    String[] keys = thisLine.split(",");
                    for(int i=0; i<keys.length; i++){
                        if(keys[i].equals("route_id")) routeIdKey = i;
                        else {
                            if(keys[i].equals("route_short_name")) routeShortNameKey = i;
                            String t = "gtfs_"+keys[i];
                            keysIndex.put(t, i);
                        }
                    }
//                    System.out.println(stopIdKey+","+stopNameKey+","+stopLatKey+","+stopLonKey);
                }
                else {
                    boolean lastIndexEmpty=false;
                    thisLine = thisLine.trim();
                    if(thisLine.contains("\"")) {
                         String[] temp = thisLine.split("\"");
                         for(int x=0; x<temp.length; x++){
                             if(x%2==1) temp[x] = temp[x].replace(",", "");
                         }
                         thisLine = "";
                         for(int x=0; x<temp.length; x++){
                             thisLine = thisLine + temp[x];
                         }
                    }
                    elements = thisLine.split(",");
                    if(thisLine.charAt(thisLine.length()-1)==',') lastIndexEmpty=true;
                    String routeName;
                    if(elements[routeShortNameKey]==null || elements[routeShortNameKey].equals("")) routeName = elements[routeIdKey];
                    else routeName = elements[routeShortNameKey];
                    Route r = new Route(elements[routeIdKey], routeName, OperatorInfo.getFullName());
                    HashSet<String> keys = new HashSet<String>();
                    keys.addAll(keysIndex.keySet());
                    Iterator it = keys.iterator();
                    try {
                        while(it.hasNext()){
                            String k = (String)it.next();
                            String v = null;
                            if(!lastIndexEmpty) v = elements[(Integer)keysIndex.get(k)];
                            if ((v!=null) && (!v.equals(""))) r.addTag(k, v);
                        }
                    } catch(Exception e){
                        System.out.println("Error occurred! Please check your GTFS input files");
                        System.out.println(e.toString());
                        System.exit(0);
                    }
                    routes.put(elements[routeIdKey], r);
                }
            }
        }
        catch (IOException e) {
            System.err.println("Error: " + e);
        }
        return routes;
    }

    public Hashtable<String, HashSet<Route>> matchRouteToStop(String routes_fName, String trips_fName, String stop_times_fName){
        allRoutes.putAll(readRoutes(routes_fName));
        String thisLine;
        String [] elements;
        // hashtable String vs. String
        Hashtable tripIDs = new Hashtable();

        // trips.txt read-in
        try {
            int tripIdKey=-1, routeIdKey=-1;
            BufferedReader br = new BufferedReader(new FileReader(trips_fName));
            boolean isFirstLine = true;
            while ((thisLine = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    thisLine = thisLine.replace("\"", "");
                    String[] keys = thisLine.split(",");
                    for(int i=0; i<keys.length; i++){
                        if(keys[i].equals("route_id")) routeIdKey = i;
                        else if(keys[i].equals("trip_id")) tripIdKey = i;
                    }
                }
                else {
                    if(thisLine.contains("\"")) {
                         String[] temp = thisLine.split("\"");
                         for(int x=0; x<temp.length; x++){
                             if(x%2==1) temp[x] = temp[x].replace(",", "");
                         }
                         thisLine = "";
                         for(int x=0; x<temp.length; x++){
                             thisLine = thisLine + temp[x];
                         }
                    }
                    elements = thisLine.split(",");
                    // not sure if tripId is unique in trips.txt, e.g. can 1 trip_id has multiple route_id
                    if (tripIDs.containsKey(elements[tripIdKey])) {
                        System.out.println("Repeat "+elements[tripIdKey]);
                    }
                    tripIDs.put(elements[tripIdKey], elements[routeIdKey]);
                }
            }
        }
        catch (IOException e) {
            System.err.println("Error: " + e);
        }

        // hashtable String(stop_id) vs. HashSet(routes)
        Hashtable stopIDs = new Hashtable();
        // stop_times.txt read-in
        int stopIdKey=-1, tripIdKey = -1;
        try {
            BufferedReader br = new BufferedReader(new FileReader(stop_times_fName));
            boolean isFirstLine = true;
            while ((thisLine = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    thisLine = thisLine.replace("\"", "");
                    String[] keys = thisLine.split(",");
                    for(int i=0; i<keys.length; i++){
                        if(keys[i].equals("stop_id")) stopIdKey = i;
                        else if(keys[i].equals("trip_id")) tripIdKey = i;
                    }
                }
                else {
                    if(thisLine.contains("\"")) {
                         String[] temp = thisLine.split("\"");
                         for(int x=0; x<temp.length; x++){
                             if(x%2==1) temp[x] = temp[x].replace(",", "");
                         }
                         thisLine = "";
                         for(int x=0; x<temp.length; x++){
                             thisLine = thisLine + temp[x];
                         }
                    }
                    elements = thisLine.split(",");
                    String trip = elements[tripIdKey];
                    HashSet<Route> routes = new HashSet<Route>();
                    Route tr = null;
                    if((String)tripIDs.get(trip)!=null) tr = (Route)allRoutes.get((String)tripIDs.get(trip));
                    if(tr!=null) routes.add(tr);
                    String sid = OsmFormatter.getValidBusStopId(elements[stopIdKey]);
                    if (stopIDs.containsKey(sid)) {
                        routes.addAll((HashSet<Route>)stopIDs.get(sid));
                        stopIDs.remove(sid);
                    }
                    stopIDs.put(sid, routes);
                }
            }
        }
        catch (IOException e) {
            System.err.println("Error: " + e);
        }
        return stopIDs;
    }

    public String getRoutesInTextByBusStop(HashSet<Route> r) {
        String text="";
        if (r!=null) {
            ArrayList<Route> routes = new ArrayList<Route>();
            //convert from hashset to arraylist
            routes.addAll(r);
            //ordering by hashcode
            for (int i=0; i<routes.size()-1; i++) {
                int k=i;
                for (int j=i+1; j<routes.size(); j++) {
                    if (routes.get(k).getRouteRef().hashCode() > routes.get(j).getRouteRef().hashCode()) {
                        k = j;
                    }
                }
                Route temp = routes.get(i);
                routes.set(i, routes.get(k));
                routes.set(k, temp);
            }

            //to text
            for (int i=0; i<routes.size(); i++) {
                text = text + ";" + routes.get(i).getRouteRef();
            }
            //delete the 1st semi-colon
            if (!text.isEmpty()) {
                text = text.substring(1);
            }
        }
        return text;
    }

    public static Set<String> getAllRoutesID(){
        return allRoutes.keySet();
    }
    
    public GTFSReadIn() {
        stops = new ArrayList<Stop>();
        allRoutes = new Hashtable<String, Route>();
//        readBusStop("C:\\Users\\Khoa Tran\\Desktop\\Summer REU\\Khoa_transit\\stops.txt");
    }
}