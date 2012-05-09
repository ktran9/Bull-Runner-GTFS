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

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.swing.JTextArea;
import edu.usf.cutr.gtfs_builder.object.OperatorInfo;
import edu.usf.cutr.gtfs_builder.object.OsmNode;
import edu.usf.cutr.gtfs_builder.object.Stop;

/**
 *
 * @author Khoa Tran
 */

public class WriteFile {
//    List<Stop> stops = new ArrayList<Stop>();
    public WriteFile(String fname, HashSet<Stop> s){
        ArrayList<Stop> ls = new ArrayList<Stop>(s.size());
        ls.addAll(s);
        writeStopToFile(fname, ls);
    }

    public WriteFile(String fname, List<Stop> st){
        writeStopToFile(fname, st);
    }

    public WriteFile(String fname, String contents) {
        Writer output = null;
        File file = new File(fname);
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write(contents);
            output.close();
            System.out.println("Your file: "+fname+" has been written");
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

    public WriteFile(String fname, Hashtable r) {
        Hashtable report = new Hashtable();
        report.putAll(r);

        HashSet<Stop> reportKeys = new HashSet<Stop>();
        Writer output = null;
        File file = new File(fname);
        try {
            output = new BufferedWriter(new FileWriter(file));
            reportKeys.addAll(report.keySet());
            Iterator it = reportKeys.iterator();
            int count = 1;
            // Print new gtfs stop with problems first
            while (it.hasNext()){
                Stop st = new Stop((Stop)it.next());
                // if arraylist
                if (!report.get(st).equals("none")){
                    output.write(count+". "+st.getStopID()+","+st.getStopName()+","+
                            st.getLat()+","+st.getLon()+"\n");
                    ArrayList<Stop> ls = new ArrayList<Stop>();
                    ls.addAll((ArrayList<Stop>)report.get(st));
                    for(int i=0; i<ls.size(); i++){
                        output.write("      "+(i+1)+". "+ls.get(i).printOSMStop()+"\n");
                        output.write(" REPORT: "+ls.get(i).getReportText()+"\n\n");
                    }
                    output.write("\n\n");
                    count++;
                }
            }
            // Print new gtfs stop with no issues
            Iterator iter = reportKeys.iterator();
            while (iter.hasNext()){
                Stop st = new Stop((Stop)iter.next());
                // if no arraylist, meaning successfully added or nothing needs to be changed from last upload
                if (report.get(st).equals("none")){
                    output.write(count+". "+st.getStopID()+","+st.getStopName()+","+
                            st.getLat()+","+st.getLon()+"\n");
                    output.write(" REPORT: "+st.getReportText()+"\n\n");
                    count++;
                }
            }
            output.close();
            System.out.println("Your file: "+fname+" has been written");
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
/*
    public static void orderingStop(ArrayList<Stop> reportKeys){
        //ordering by hashcode
        for (int i=0; i<reportKeys.size()-1; i++) {
            int k=i;
            for (int j=i+1; j<reportKeys.size(); j++) {
                if (reportKeys.get(k).getStopID().hashCode() > reportKeys.get(j).getStopID().hashCode()) {
                    k = j;
                }
            }
            Stop temp = new Stop(reportKeys.get(i));
            reportKeys.set(i, reportKeys.get(k));
            reportKeys.set(k, temp);
        }
    }
*/
    public static void orderingStop(ArrayList<String> reportKeys){
        //ordering by hashcode
        for (int i=0; i<reportKeys.size()-1; i++) {
            int k=i;
            for (int j=i+1; j<reportKeys.size(); j++) {
                if (reportKeys.get(k).hashCode() > reportKeys.get(j).hashCode()) {
                    k = j;
                }
            }
            String temp = reportKeys.get(i);
            reportKeys.set(i, reportKeys.get(k));
            reportKeys.set(k, temp);
        }
    }
/*
    public static void exportStops(String fname, Hashtable r, boolean isGtfsFormat, JTextArea taskOutput){
        Hashtable report = new Hashtable();
        report.putAll(r);

        ArrayList<Stop> reportKeys = new ArrayList<Stop>();
        //convert to arrayList for ordering
        reportKeys.addAll(report.keySet());
        orderingStop(reportKeys);

        Writer output = null;
        File file = new File(fname);
        try {
            output = new BufferedWriter(new FileWriter(file));
            //print key (first line)
            output.write(OperatorInfo.getGtfsFields());
            if(!isGtfsFormat) output.write(",OSM_TAGs");
            output.write("\n");

            // print content
            for(int i=0; i<reportKeys.size(); i++){
                Stop st = new Stop(reportKeys.get(i));
                String[] keys = OperatorInfo.getGtfsFields().split(",");
                for(int j=0; j<keys.length; j++){
                    String content="";
                    if(keys[j].equals("stop_id")) content = st.getStopID();
                    else if(keys[j].equals("stop_name")) content = st.getStopName();
                    else if(keys[j].equals("stop_lat")) content = st.getLat();
                    else if(keys[j].equals("stop_lon")) content = st.getLon();
                    // gtfs stop_url is mapped to source_ref tag in OSM
                    else if(keys[j].equals("stop_url")){
                        content = st.getTag("source_ref");
                        st.removeTag("source_ref");
                    }
                    else {
                        content = st.getTag("gtfs_"+keys[j]);
                        st.removeTag("gtfs_"+keys[j]);
                    }

                    if(content!=null && !content.equals("none") && !content.equals("")){
                        output.write(content);
                    }
                    //if not the last key
                    if(j<keys.length-1){
                        output.write(",");
                    }
                    //if last key
                    else {
                        if(!isGtfsFormat){
                            st.removeTag("operator");
                            st.removeTag("name");
                            st.removeTag("source");
                            st.removeTag("highway");
                            st.removeTag("gtfs_id");

                            output.write(",");
                            ArrayList<String> tagKeys = new ArrayList<String>();
                            tagKeys.addAll(st.keySet());
                            for(int t=0; t<tagKeys.size(); t++){
                                String k = tagKeys.get(t);
                                if(k!=null && !k.equals("none") && !k.equals("")) {
                                    if(t!=0) output.write("|"+k+"="+st.getTag(k));
                                    else output.write(k+"="+st.getTag(k));
                                }
                            }
                        }
                        output.write("\n");
                    }
                }
            }
            output.close();
            System.out.println("Your file: "+fname+" has been written");
            taskOutput.append("Your file: "+fname+" has been written"+"\n");
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
    */
    public static void exportStops(String fname, Hashtable<String, Stop> r, boolean isGtfsFormat){//, JTextArea taskOutput){
        Hashtable<String, Stop> report = new Hashtable<String, Stop>();
        report.putAll(r);

        ArrayList<String> reportKeys = new ArrayList<String>();
        //convert to arrayList for ordering
        reportKeys.addAll(report.keySet());
        orderingStop(reportKeys);

        Writer output = null;
        File file = new File(fname);
        try {
            output = new BufferedWriter(new FileWriter(file));
            //print key (first line)
            output.write(OperatorInfo.getGtfsFields());
            if(!isGtfsFormat) output.write(",OSM_TAGs");
            output.write("\n");

            // print content
            for(int i=0; i<reportKeys.size(); i++){
                Stop st = new Stop(report.get(reportKeys.get(i)));
                String[] keys = OperatorInfo.getGtfsFields().split(",");
                for(int j=0; j<keys.length; j++){
                    String content="";
                    if(keys[j].equals("OSM Node ID")) content = st.getStopID();
//                    else if(keys[j].equals("stop_name")) content = st.getStopName();
                    else if(keys[j].equals("Latitude")) content = st.getLat();
                    else if(keys[j].equals("Longitude")) content = st.getLon();
                    // gtfs stop_url is mapped to source_ref tag in OSM
                    else {
                        content = st.getTag(keys[j]);
                        st.removeTag(keys[j]);
                    }

                    if(content!=null && !content.equals("none") && !content.equals("")){
                        output.write(content);
                    }
                    //if not the last key
                    if(j<keys.length-1){
                        output.write(",");
                    }
                    //if last key
                    else {
                        if(!isGtfsFormat){
                            st.removeTag("operator");
                            st.removeTag("name");
                            st.removeTag("source");
                            st.removeTag("highway");
                            st.removeTag("gtfs_id");

                            output.write(",");
                            ArrayList<String> tagKeys = new ArrayList<String>();
                            tagKeys.addAll(st.keySet());
                            for(int t=0; t<tagKeys.size(); t++){
                                String k = tagKeys.get(t);
                                if(k!=null && !k.equals("none") && !k.equals("")) {
                                    if(t!=0) output.write("|"+k+"="+st.getTag(k));
                                    else output.write(k+"="+st.getTag(k));
                                }
                            }
                        }
                        output.write("\n");
                    }
                }
            }
            output.close();
            System.out.println("Your file: "+fname+" has been written");
//            taskOutput.append("Your file: "+fname+" has been written"+"\n");
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

    public static void exportShapeFile(String fname, Hashtable<String, ArrayList<OsmNode>> shape, ArrayList<String> shapeIDs){
        Writer output = null;
        File file = new File(fname);
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write("shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence,shape_dist_traveled\n");
            ArrayList<String> shapeKeys = new ArrayList<String>();
            shapeKeys.addAll(shape.keySet());
            for(int i=0; i<shapeKeys.size(); i++){
                ArrayList<OsmNode> points = new ArrayList<OsmNode>();
                points.addAll(shape.get(shapeKeys.get(i)));
                for(int j=0; j<points.size(); j++){
                    output.write(shapeIDs.get(i)+","+points.get(j).getLat()+","+points.get(j).getLon()+","+j+","+points.get(j).getDistTraveled()+"\n");
                }
            }
            output.close();
            System.out.println("Your file: "+fname+" has been written");
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

    public static void exportArrayList2File(String fname, ArrayList<ArrayList<String>> d){
        ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
        data.addAll(d);
        Writer output = null;
        File file = new File(fname);
        try {
            output = new BufferedWriter(new FileWriter(file));
            for(int i=0; i<data.size(); i++){
                ArrayList<String> r = data.get(i);
                for(int j=0; j<r.size(); j++){
                    output.write(r.get(j));
                    if(j==r.size()-1) output.write("\n");
                    else output.write(",");
                }
            }
            output.close();
            System.out.println("Your file: "+fname+" has been written");
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

    public void writeStopToFile(String fname, List<Stop> st){
        List<Stop> stops = new ArrayList<Stop>();
        stops.addAll(st);
        Writer output = null;
        File file = new File(fname);
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write("stop_id,stop_name,stop_lat,stop_lon\n");
            for(int i=0; i<stops.size(); i++){
                output.write(stops.get(i).getStopID()+","+stops.get(i).getStopName()+","+
                             stops.get(i).getLat()+","+stops.get(i).getLon()+"\n");
            }
            output.close();
            System.out.println("Your file: "+fname+" has been written");
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

    public static void writeNodesToKmlFile(String fname, List<OsmNode> st){
        List<OsmNode> nodes = new ArrayList<OsmNode>();
        nodes.addAll(st);
        Writer output = null;
        File file = new File(fname);
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
                    "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"+
                    "<Document>\n"+
                    "<Folder>\n");
            output.write("<name>Placemarks</name>");
            for(int i=0; i<nodes.size(); i++){
                OsmNode n = nodes.get(i);
                output.write("<Placemark id=\""+i+"\">\n");
                output.write("<name>"+n.getTags().get("gtfs_shape_id")+"-"+n.getTags().get("gtfs_shape_pt_sequence")+"</name>");
                output.write("<description>\n");
                output.write("("+n.getLon()+", "+n.getLat()+") "+n.getTags().get("gtfs_shape_pt_sequence"));
                output.write("</description>\n");
                output.write("<Point>");
                output.write("<coordinates>"+nodes.get(i).getLon()+","+nodes.get(i).getLat()+",0</coordinates>\n");
                output.write("</Point>\n");
                output.write("</Placemark>\n");
            }
            output.write("</Folder>\n"+
                    "</Document>\n"+
                    "</kml>");
            output.close();
            System.out.println("Your file: "+fname+" has been written");
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}
