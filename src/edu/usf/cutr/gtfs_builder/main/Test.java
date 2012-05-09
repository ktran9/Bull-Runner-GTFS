/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.usf.cutr.gtfs_builder.main;

import edu.usf.cutr.gtfs_builder.gui.Viewer;
import edu.usf.cutr.gtfs_builder.io.GTFSReadIn;
import edu.usf.cutr.gtfs_builder.io.WriteFile;
import edu.usf.cutr.gtfs_builder.object.OperatorInfo;
import edu.usf.cutr.gtfs_builder.object.OsmNode;
import edu.usf.cutr.gtfs_builder.object.Stop;
import edu.usf.cutr.gtfs_builder.parser.BusStopParser;
import edu.usf.cutr.gtfs_builder.parser.RelationParser;
import edu.usf.cutr.gtfs_builder.tools.OsmDistance;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author ktran
 */
public class Test {
    private ArrayList<AttributesImpl> existingNodes = new ArrayList<AttributesImpl>();
    private ArrayList<Hashtable> existingBusTags = new ArrayList<Hashtable>();
    private Hashtable<String, ArrayList<OsmNode>> shape = new Hashtable<String, ArrayList<OsmNode>>();
    private ArrayList<ArrayList<String>> agency = new ArrayList<ArrayList<String>>();
    private ArrayList<ArrayList<String>> routes = new ArrayList<ArrayList<String>>();
    private ArrayList<ArrayList<String>> calendar = new ArrayList<ArrayList<String>>();
    private ArrayList<ArrayList<String>> trips = new ArrayList<ArrayList<String>>();
    private ArrayList<ArrayList<String>> frequencies = new ArrayList<ArrayList<String>>();
    private ArrayList<ArrayList<String>> stop_times = new ArrayList<ArrayList<String>>();
    private ArrayList<ArrayList<String>> fare_attributes = new ArrayList<ArrayList<String>>();

    private Hashtable<String, String> routeToShape = new Hashtable<String, String>();
    private Hashtable<String, ArrayList<String>> routeToService = new Hashtable<String, ArrayList<String>>();
    private Hashtable<String, Integer> routeToNumberOfBus = new Hashtable<String, Integer>();
    private Hashtable<String, String> serviceIdToStartTime = new Hashtable<String, String>();
    private Hashtable<String, String> serviceIdToEndTime = new Hashtable<String, String>();
    private Hashtable<String, String> routeToHeadWaySec = new Hashtable<String, String>();

    private Hashtable<String, String> tripToStartTime = new Hashtable<String, String>();
    private Hashtable<String, String> tripToEndTime = new Hashtable<String, String>();

    private final int BUS_SPEED = 5; // meter per second

    private String map_file = "2011_10_24_map_area.osm";
    private String csv_stops_file = "Ed_input_2011-10-18.csv";
    private String csv_stops_file_lat_lon = "Ed_input_2011-10-18_lat_lon.csv";

    private void matchLatLonForStops(){
        try {
            // get data from file - need to remove this for REAL APPLICATION
            InputSource inputSource = new InputSource(map_file);
            BusStopParser par = new BusStopParser();
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, par);
            existingNodes.addAll(par.getNodes());
            existingBusTags.addAll(par.getTags());
        } catch(IOException e) {
            System.out.println(e);
        } catch(SAXException e) {
            System.out.println(e);
        } catch(ParserConfigurationException e) {
            System.out.println(e);
        }

        System.out.println(existingNodes.size());

        Hashtable<String, Stop> stops = new Hashtable<String, Stop>();

        stops.putAll(getStops());

        WriteFile.exportStops(csv_stops_file_lat_lon, stops, true);
    }

    private void getAllNodesInRelation(){
        Hashtable<String, ArrayList<OsmNode>> tempShape = new Hashtable<String, ArrayList<OsmNode>>();
        try {
            // get data from file - need to remove this for REAL APPLICATION
            InputSource inputSource = new InputSource(map_file);
            RelationParser par = new RelationParser();
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, par);
            tempShape.putAll(par.getShape());
        } catch(IOException e) {
            System.out.println(e);
        } catch(SAXException e) {
            System.out.println(e);
        } catch(ParserConfigurationException e) {
            System.out.println(e);
        }

        /*
         * Set shape keys to 0 - n
         */
        ArrayList<String> tempShapeKeys = new ArrayList<String>();
        tempShapeKeys.addAll(tempShape.keySet());
        for (int i=0; i<tempShape.size(); i++){
            shape.put(Integer.toString(i), tempShape.get(tempShapeKeys.get(i)));
        }

        System.out.println(shape.size());
        WriteFile.exportShapeFile("shapes.txt", shape, tempShapeKeys);

        /*
        ArrayList<String> shapeKeys = new ArrayList<String>();
        shapeKeys.addAll(shape.keySet());
        for(int i=0; i<shapeKeys.size(); i++){
            System.out.println(shape.get(shapeKeys.get(i)));
        }*/

        Hashtable<String, Stop> csvStops = readCsvFile(csv_stops_file_lat_lon);

        final ArrayList<Stop>stops =  new ArrayList<Stop>();
        stops.addAll(csvStops.values());

        /*
         * Run the UI to manually select the shapes
         */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Viewer(shape, stops).setVisible(true);
            }
        });
    }

    private void buildRoute(){
        WriteFile.exportArrayList2File("routes.txt", routes);
    }

    private void buildCalendar(){
        WriteFile.exportArrayList2File("calendar.txt", calendar);
    }

    /*
     * Need to invoke buildRoute and buildCalendar BEFORE calling this method
     */
    private void buildTrips(){
        trips.add(new ArrayList<String>(Arrays.asList("route_id","service_id","trip_id","shape_id")));

        //PROCESS
        int tripId = 1;
        //start with the second row. The first row is the key, not data
        //assume the first column is route_id (indeed it is in our particular data)
        for(int i=1; i<routes.size(); i++){
            String route_id = routes.get(i).get(0);
            if(route_id==null) continue;
            ArrayList<String> service = routeToService.get(route_id);
            if (service==null) continue;
            for(int numService=0; numService<service.size(); numService++){
                for(int numBus=0; numBus<routeToNumberOfBus.get(route_id); numBus++){
                    trips.add(new ArrayList<String>(Arrays.asList(route_id, service.get(numService), Integer.toString(tripId), routeToShape.get(route_id))));
                    tripId++;
                }
            }
        }

        WriteFile.exportArrayList2File("trips.txt", trips);
    }

    // build trips before invoking this method
    private void buildFrequencies(){
//        trips.add(new ArrayList<String>(Arrays.asList("route_id","service_id","trip_id","shape_id")));
        frequencies.add(new ArrayList<String>(Arrays.asList("trip_id","start_time","end_time","headway_secs")));
        //start at the second row
        for(int i=1; i<trips.size(); i++){
            String trip_id = trips.get(i).get(2);
            ArrayList<String> current_trip = trips.get(Integer.parseInt(trip_id));
            String service_id = trips.get(i).get(1);
            String start_time = serviceIdToStartTime.get(service_id);
            String end_time = serviceIdToEndTime.get(service_id);
            // check if same route and same service id with the previous entry--> if yes, start_time = previous start_time+headway_secs
            if(frequencies.size()>1) {
                ArrayList<String> previous_frequencies = frequencies.get(frequencies.size()-1);
                String previous_trip_id = previous_frequencies.get(0);
                ArrayList<String> previous_trip = trips.get(Integer.parseInt(previous_trip_id));
                if(previous_trip.get(0).equals(current_trip.get(0)) && previous_trip.get(1).equals(current_trip.get(1))){
                    start_time = addSec(previous_frequencies.get(1),Integer.parseInt(routeToHeadWaySec.get(previous_trip.get(0))));
                    end_time = addSec(previous_frequencies.get(2),Integer.parseInt(routeToHeadWaySec.get(previous_trip.get(0))));
                }
            }

            tripToStartTime.put(trip_id, start_time);
            tripToEndTime.put(trip_id, end_time);
            frequencies.add(new ArrayList<String>(Arrays.asList(trip_id, start_time, end_time, routeToHeadWaySec.get(current_trip.get(0)))));
        }

        WriteFile.exportArrayList2File("frequencies.txt", frequencies);
    }

    private void buildStop_times(){
        //        trips.add(new ArrayList<String>(Arrays.asList("route_id","service_id","trip_id","shape_id")));
        //frequencies.add(new ArrayList<String>(Arrays.asList("trip_id","start_time","end_time","headway_secs")));
        stop_times.add(new ArrayList<String>(Arrays.asList("trip_id,arrival_time,departure_time,stop_id,stop_sequence")));

        ArrayList<Stop> stops = new ArrayList<Stop>();
        stops.addAll(readCsvFile("stop_times_input.csv").values());

        Stop previous_stop=null;

        // start at 1
        for(int i=1; i<trips.size(); i++){
            String trip_id = trips.get(i).get(2);
            String route_id = trips.get(i).get(0);
            int sequence_index = 1;
            while(true){
                Stop s = null;
                for(int stopIndex=0; stopIndex<stops.size(); stopIndex++){
                    Stop stemp = stops.get(stopIndex);
                    String seq = stemp.getTag(route_id+"_Sequence");
                    if(seq==null) continue;
                    if(seq.equals(Integer.toString(sequence_index))){
                        s = stemp;
                        break;
                    }
                }

                if(s==null)
                    break;

                if(sequence_index==1){
                    stop_times.add(new ArrayList<String>(Arrays.asList(trip_id, tripToStartTime.get(trip_id), tripToStartTime.get(trip_id), s.getTag("Number"),"1")));
                } else {
                    double dist = OsmDistance.distVincenty(previous_stop.getLat(), previous_stop.getLon(), s.getLat(), s.getLon());
                    int timeTravel = Math.round((float)dist/BUS_SPEED);     // in second
                    String previous_start_time = stop_times.get(stop_times.size()-1).get(1);
                    String new_start_time = addSec(previous_start_time, timeTravel);
                    stop_times.add(new ArrayList<String>(Arrays.asList(trip_id, new_start_time, new_start_time, s.getTag("Number"), Integer.toString(sequence_index))));
                }

                previous_stop = s;
            
                sequence_index++;
            }
        }

        WriteFile.exportArrayList2File("stop_times.txt", stop_times);
    }

    private void buildAgency(){
        WriteFile.exportArrayList2File("agency.txt", agency);
    }

    private void buildFareAttributes(){
        WriteFile.exportArrayList2File("fare_attributes.txt", fare_attributes);
    }

    private void filterStopsFromShapes(String stopfname, String shapefname){
        ArrayList<OsmNode> stops = new ArrayList<OsmNode>();
        ArrayList<OsmNode> shapes = new ArrayList<OsmNode>();

        stops.addAll(GTFSReadIn.readLatLon(stopfname));
        shapes.addAll(GTFSReadIn.readLatLon(shapefname));

        Hashtable<String, ArrayList<OsmNode>> newShapes = new Hashtable<String, ArrayList<OsmNode>>();
        Hashtable<String, Integer> indexes = new Hashtable<String, Integer>();

        Writer output = null;
        File file = new File("newShapes.txt");
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write("shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence\n");
            
            int line=0;
            for(OsmNode shapePoint: shapes){
                line++;
                String routeName = shapePoint.getTags().get("gtfs_shape_id");
                ArrayList<OsmNode> route;
                if(newShapes.containsKey(routeName)) route = newShapes.get(routeName);
                else {
                    route = new ArrayList<OsmNode>();
                    newShapes.put(routeName, route);
                }

                int index;
                if(!indexes.containsKey(routeName))
                    indexes.put(routeName, -1);

                index = indexes.get(routeName);

                boolean shouldAdd = true;
                for(OsmNode stop: stops){
                    if(stop.equals(shapePoint)) {
                        shouldAdd = false;
                        System.out.println("line "+line+","+shapePoint.getLat()+","+shapePoint.getLon());
                        break;
                    }
                }
                
                if(shouldAdd) {
                    index++;
                    indexes.put(routeName, index);
                    output.write(routeName+","+shapePoint.getLat()+","+shapePoint.getLon()+","+index+"\n");
                    route.add(shapePoint);
                }
            }
            output.close();
            System.out.println("Your file: newShapes.txt has been written");
        } catch (IOException ioe) {
            System.out.println(ioe);
        }

        //WriteFile.exportShapeFile("newShapes.txt", newShapes);
    }

    private void displayShapesInKml(String inshapefname, String outshapefname){
        ArrayList<OsmNode> shapes = new ArrayList<OsmNode>();

        shapes.addAll(GTFSReadIn.readLatLon(inshapefname));

        WriteFile.writeNodesToKmlFile(outshapefname, shapes);
    }

//    private void changeDuplicateCoordInARoute(String inshapefname, String outshapefname){
//        ArrayList<OsmNode> shapes = new ArrayList<OsmNode>();
//        shapes.addAll(GTFSReadIn.readLatLon(inshapefname));
//
//        Hashtable<String, ArrayList<OsmNode>> newShapes = new Hashtable<String, ArrayList<OsmNode>>();
//
//        HashSet<OsmNode> filteredShapes = new HashSet<OsmNode>();
////        filteredShapes.addAll(shapes);
//        for(OsmNode n: shapes){
//            if(filteredShapes.contains(n)) {
//                n.setLat(Double.toString(Double.parseDouble(n.getLat())+0.00001));
//            } else{
//                filteredShapes.add(n);
//            }
//            String route=n.getTags().get("gtfs_shape_id");
//            ArrayList<OsmNode> points;
//            if(newShapes.containsKey(route)){
//                points = newShapes.get(route);
//            } else {
//                points = new ArrayList<OsmNode>();
//            }
//            points.add(n);
//
//            newShapes.put(route, points);
//        }
//
//        WriteFile.exportShapeFile(outshapefname, newShapes);
//    }

    private void addShapeDistanceTraveled(String inshapefname, String outshapefname){
        ArrayList<OsmNode> shapes = new ArrayList<OsmNode>();
        shapes.addAll(GTFSReadIn.readLatLon(inshapefname));
        Hashtable<String, ArrayList<OsmNode>> newShapes = new Hashtable<String, ArrayList<OsmNode>>();

        OsmNode previousNode = null;
        double totalDistance = 0;
        for(OsmNode n: shapes){
            if(previousNode!=null){
                double d = OsmDistance.distVincenty(previousNode.getLat(), previousNode.getLon(), n.getLat(), n.getLon());
                totalDistance += d;
            }
            String route=n.getTags().get("gtfs_shape_id");
            ArrayList<OsmNode> points;
            if(newShapes.containsKey(route)){
                points = newShapes.get(route);
            } else {
                points = new ArrayList<OsmNode>();
                totalDistance = 0;
                previousNode = null;
            }
            n.setDistTraveled(Double.toString(totalDistance));
            points.add(n);

            newShapes.put(route, points);

            previousNode = n;
        }

        ArrayList<String> shapeIDs = new ArrayList<String>();
        shapeIDs.addAll(newShapes.keySet());
        WriteFile.exportShapeFile(outshapefname, newShapes, shapeIDs);
    }

    private void addShapeDistanceTraveledStopTime(String instopfname, String inshapefname, String outshapefname){
        ArrayList<OsmNode> shapes = new ArrayList<OsmNode>();
        shapes.addAll(GTFSReadIn.readLatLon(inshapefname));
        Hashtable<String, ArrayList<OsmNode>> newShapes = new Hashtable<String, ArrayList<OsmNode>>();

        Hashtable<String, OsmNode> stops = new Hashtable<String, OsmNode>();
        ArrayList<OsmNode> stopSet = new ArrayList<OsmNode>();
        stopSet.addAll(GTFSReadIn.readLatLon(instopfname));
        for(OsmNode stop: stopSet){
            stops.put(stop.getStopId(), stop);
        }

        Hashtable<String, ArrayList<String>> stopSequence = new Hashtable<String, ArrayList<String>>();
        stopSequence.putAll(GTFSReadIn.readStopTime("stop_times.txt"));
        ArrayList<String> stop_times_key = new ArrayList<String>();
        stop_times_key.add("0");stop_times_key.add("1");stop_times_key.add("2");stop_times_key.add("3");stop_times_key.add("4");
        int shapeIndex = 0;
        for(String k: stop_times_key){
//            System.out.println(k);
            OsmNode shapePt = shapes.get(shapeIndex);
            boolean istrue = true;
            while(!shapePt.getTags().get("gtfs_shape_id").equals(k)){
                istrue = false;
                shapeIndex++;
                shapePt = shapes.get(shapeIndex);
            }
            if(!istrue) shapeIndex++;
            ArrayList<String> sequence = new ArrayList<String>();
            sequence.addAll(stopSequence.get(k));
            for(String stopId: sequence){
                OsmNode stop = stops.get(stopId);
                shapePt = shapes.get(shapeIndex);
                double previousDistance = 0;
                while((previousDistance==0 || (previousDistance>OsmDistance.distVincenty(shapePt.getLat(), shapePt.getLon(), stop.getLat(), stop.getLon()) || previousDistance>100))){
                    previousDistance = OsmDistance.distVincenty(shapePt.getLat(), shapePt.getLon(), stop.getLat(), stop.getLon());
                    shapeIndex++;
                    if(shapeIndex<shapes.size())shapePt = shapes.get(shapeIndex);
                    else System.exit(1);
                }
                OsmNode accepted = shapes.get(shapeIndex-1);
                System.out.println(accepted.getTags().get("gtfs_shape_id")+"    "+stop.getStopId()+"    "+previousDistance+"    "+accepted.getTags().get("gtfs_shape_dist_traveled"));
//                System.out.println(accepted.getTags().get("gtfs_shape_dist_traveled"));
            }
        }

        System.out.println();
        ArrayList<String> shapeIDs = new ArrayList<String>();
        shapeIDs.addAll(newShapes.keySet());
        WriteFile.exportShapeFile(outshapefname, newShapes, shapeIDs);
    }

    public Test(){
//        filterStopsFromShapes("stops.txt", "shapes.txt");
//        displayShapesInKml("OLD_VERSION_shapes.txt","oldShape.kml");
//        displayShapesInKml("newShapes.txt","newShape.kml");
        
        //agency.txt        
        agency.add(new ArrayList<String>(Arrays.asList("agency_name","agency_url","agency_timezone","agency_lang")));
        agency.add(new ArrayList<String>(Arrays.asList("Bull Runner","http://usfweb2.usf.edu/Parking_services/bullrunneroperationhours.asp","America/New_York","en")));

        buildAgency();

        //fare_attributes.txt
        fare_attributes.add(new ArrayList<String>(Arrays.asList("fare_id","price","currency_type","payment_method","transfers")));
        fare_attributes.add(new ArrayList<String>(Arrays.asList("0","0.00","USD","0","0")));

        buildFareAttributes();
        
        //stops.txt
        matchLatLonForStops();

        //shapes.txt
        getAllNodesInRelation();
/*
        //routes.txt
        routes.add(new ArrayList<String>(Arrays.asList("route_id","route_short_name","route_long_name","route_type","route_url")));
        routes.add(new ArrayList<String>(Arrays.asList("A","A","Green Campus Loop","3","http://usfweb2.usf.edu/parking_services/BullRunnerRoutesStops.htm")));
        routes.add(new ArrayList<String>(Arrays.asList("B","B","Blue USF Health","3","http://usfweb2.usf.edu/parking_services/BullRunnerRoutesStops.htm")));
        routes.add(new ArrayList<String>(Arrays.asList("C","C","Purple Off-Campus North","3","http://usfweb2.usf.edu/parking_services/BullRunnerRoutesStops.htm")));
        routes.add(new ArrayList<String>(Arrays.asList("D","D","Red Off-Campus West","3","http://usfweb2.usf.edu/parking_services/BullRunnerRoutesStops.htm")));
        routes.add(new ArrayList<String>(Arrays.asList("E","E","Gold Campus Loop","3","http://usfweb2.usf.edu/parking_services/BullRunnerRoutesStops.htm")));
        routes.add(new ArrayList<String>(Arrays.asList("F","F","Brown Off-Campus South","3","http://usfweb2.usf.edu/parking_services/BullRunnerRoutesStops.htm")));

        buildRoute();

        //calendar.txt
        calendar.add(new ArrayList<String>(Arrays.asList("service_id","monday","tuesday","wednesday","thursday","friday","saturday","sunday","start_date","end_date")));
        calendar.add(new ArrayList<String>(Arrays.asList("Mo","1","1","1","1","0","0","0","20111001","20120531")));
        calendar.add(new ArrayList<String>(Arrays.asList("Fr","0","0","0","0","1","0","0","20111001","20120531")));
        calendar.add(new ArrayList<String>(Arrays.asList("Su","0","0","0","0","0","1","1","20111001","20120531")));

        buildCalendar();

        //trips.txt
        routeToShape.put("A", "0");
        routeToShape.put("B", "1");
        routeToShape.put("C", "2");
        routeToShape.put("D", "3");
        routeToShape.put("E", "4");
        routeToShape.put("F", "5");
        
        routeToService.put("A", new ArrayList<String>(Arrays.asList("Mo","Fr")));
        routeToService.put("B", new ArrayList<String>(Arrays.asList("Mo","Fr")));
        routeToService.put("C", new ArrayList<String>(Arrays.asList("Mo","Fr","Su")));
        routeToService.put("D", new ArrayList<String>(Arrays.asList("Mo","Fr","Su")));
        routeToService.put("E", new ArrayList<String>(Arrays.asList("Mo","Fr")));
        routeToService.put("F", new ArrayList<String>(Arrays.asList("Mo","Fr","Su")));

        routeToNumberOfBus.put("A", new Integer(1));
        routeToNumberOfBus.put("B", new Integer(1));
        routeToNumberOfBus.put("C", new Integer(1));
        routeToNumberOfBus.put("D", new Integer(1));
        routeToNumberOfBus.put("E", new Integer(1));
        routeToNumberOfBus.put("F", new Integer(1));

        buildTrips();


        //frequencies.txt
        serviceIdToStartTime.put("Mo", "07:00:00");
        serviceIdToStartTime.put("Fr", "07:00:00");
        serviceIdToStartTime.put("Su", "14:30:00");

        serviceIdToEndTime.put("Mo", "24:00:00");
        serviceIdToEndTime.put("Fr", "17:30:00");
        serviceIdToEndTime.put("Su", "21:30:00");

        routeToHeadWaySec.put("A", "600");
        routeToHeadWaySec.put("B", "540");
        routeToHeadWaySec.put("C", "720");
        routeToHeadWaySec.put("D", "600");
        routeToHeadWaySec.put("E", "600");
        routeToHeadWaySec.put("F", "600");
        buildFrequencies();

        buildStop_times();

//        displayShapesInKml("shapes.txt", "newShapes.txt");
//        changeDuplicateCoordInARoute("shapes.txt", "newShapes.txt");
/*        displayShapesInKml("routeA.txt", "routeA.kml");
        displayShapesInKml("routeB.txt", "routeB.kml");
        displayShapesInKml("routeC.txt", "routeC.kml");
        displayShapesInKml("routeD.txt", "routeD.kml");
        displayShapesInKml("routeE.txt", "routeE.kml");*/
/*
        addShapeDistanceTraveled("shapes.txt", "newShapes.txt");
        addShapeDistanceTraveledStopTime("stops.txt","shapes.txt", "newShapes.txt");*/
    }
    
    // with format x:y:z and x can be greater than 24
    private String addSec(String time, int addedSec){
        String[] values = time.split(":");
        if(values.length!=3) return null;
        int second = Integer.parseInt(values[2]);
        int minute = Integer.parseInt(values[1]);
        int hour = Integer.parseInt(values[0]);

        second+=addedSec;
        if(second>=60){
            int addedMinute = second/60;
            minute+=addedMinute;
            if(minute>=60) hour+=minute/60;
            minute=minute%60;
        }
        second=second%60;

        if(second<10) values[2] = "0"+Integer.toString(second);
        else values[2] = Integer.toString(second);

        if(minute<10) values[1] = "0"+Integer.toString(minute);
        else values[1] = Integer.toString(minute);

        if(hour<10) values[0] = "0"+Integer.toString(hour);
        else values[0] = Integer.toString(hour);

        return values[0]+":"+values[1]+":"+values[2];
    }

    private Hashtable<String, Stop> getStops(){
        Hashtable<String, Stop> csvStops = readCsvFile(csv_stops_file);

        Hashtable<String, Stop> stops = new Hashtable<String, Stop>();

        for(int i=0; i<existingNodes.size(); i++){
            AttributesImpl att = existingNodes.get(i);
            if(csvStops.keySet().contains(att.getValue("id"))) {
                Stop s = new Stop(csvStops.get(att.getValue("id")));
                s.setLat(att.getValue("lat"));
                s.setLon(att.getValue("lon"));
                stops.put(s.toString(), s);
            }
        }
        
        return stops;
    }

    public Hashtable<String, Stop> readCsvFile(String fname){
        Hashtable<String, Stop> stops = new Hashtable<String, Stop>();
        
        String thisLine;
        String [] elements;
        int stopIdKey=-1, stopNameKey=-1, stopLatKey=-1, stopLonKey=-1;
        try {
            BufferedReader br = new BufferedReader(new FileReader(fname));
            boolean isFirstLine = true;
            Hashtable keysIndex = new Hashtable();
            while ((thisLine = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    OperatorInfo.setGtfsFields(thisLine);
                    thisLine = thisLine.replace("\"", "");
                    String[] keys = thisLine.split(",");
                    for(int i=0; i<keys.length; i++){
                        if(keys[i].equals("OSM Node ID")) stopIdKey = i;
//                        else if(keys[i].equals("stop_name")) stopNameKey = i;
                        else if(keys[i].equals("Latitude")) stopLatKey = i;
                        else if(keys[i].equals("Longitude")) stopLonKey = i;
                        // gtfs stop_url is mapped to source_ref tag in OSM
/*                        else if(keys[i].equals("stop_url")){
                            keysIndex.put("source_ref", i);
                        }*/
                        else {
                            String t = keys[i];//"gtfs_"+keys[i];
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
                    String tempStopId;
                    try {
                        tempStopId = elements[stopIdKey];
                    } catch (ArrayIndexOutOfBoundsException e){
                        tempStopId = "none";
                    }
                    Stop s;
                    try {
                        s = new Stop(tempStopId, "N/A", "N/A",elements[stopLatKey],elements[stopLonKey]);
                    } catch (ArrayIndexOutOfBoundsException e){
                        s = new Stop(tempStopId, "N/A", "N/A","0","0");
                    }
                    HashSet<String> keys = new HashSet<String>();
                    keys.addAll(keysIndex.keySet());
                    Iterator it = keys.iterator();
                    String k;
                    try {
                        while(it.hasNext()){
                            k = (String)it.next();
                            String v = null;
                            try{
                                v = elements[(Integer)keysIndex.get(k)];
                            } catch (ArrayIndexOutOfBoundsException e){
                                v = null;
                            }
                            if ((v!=null) && (!v.equals(""))) s.addTag(k, v);
                        }
                    } catch(Exception e){
                        System.out.println("Error occurred! Please check your GTFS input files");
                        System.out.println(e.toString());
                        System.exit(0);
                    }
                    stops.put(s.toString(), s);
//                    System.out.println(thisLine);
                }
            }
        }
        catch (IOException e) {
            System.err.println("Error: " + e);
        }
        return stops;
    }
    
    public static void main(String[] args){
        new Test();
    }
}
