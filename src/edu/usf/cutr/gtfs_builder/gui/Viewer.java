/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * Viewer.java
 *
 * Created on Jan 7, 2011, 1:02:34 PM
 */

package edu.usf.cutr.gtfs_builder.gui;

import edu.usf.cutr.gtfs_builder.io.WriteFile;
import edu.usf.cutr.gtfs_builder.object.OsmNode;
import edu.usf.cutr.gtfs_builder.object.Stop;
import edu.usf.cutr.gtfs_builder.tools.OsmDistance;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.mapviewer.Waypoint;
import org.jdesktop.swingx.mapviewer.WaypointPainter;
import org.jdesktop.swingx.mapviewer.WaypointRenderer;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;

/**
 *
 * @author ktran
 */
public class Viewer extends javax.swing.JFrame {

    private DefaultTileFactory osmTf;

    private Hashtable<String, ArrayList<OsmNode>> shape = new Hashtable<String, ArrayList<OsmNode>>();

    public JXMapViewer mainMap;

    private HashSet<GeoPosition> allNodesGeo;

    public CompoundPainter mainPainter = new CompoundPainter();

    private WaypointPainter stopsPainter = new WaypointPainter();

    private WaypointPainter otherStopsPainter = new WaypointPainter();

    private Hashtable<GeoPosition, OsmNode> newNodesByGeoPos = new Hashtable<GeoPosition, OsmNode>();

    private Painter<JXMapViewer> selectedNodeOverlayPainter=null;

    private ArrayList<ArrayList<OsmNode>> buildingShapes = new ArrayList<ArrayList<OsmNode>>();

    private Hashtable<String, ArrayList<OsmNode>> finalShapes = new Hashtable<String, ArrayList<OsmNode>>();

    private OsmNode selectedNodeFromMap = null;

    public boolean hasStart = false;

    private HashSet<String> routeToBuild = new HashSet<String>();

    private ArrayList<Stop> csvStops = new ArrayList<Stop>();

    private ArrayList<ArrayList<Stop>> finalTrips = new ArrayList<ArrayList<Stop>>();

    private Stop selectedStop = null;

    private ArrayList<String> shapeIDs = new ArrayList<String>();

    /** Creates new form Viewer */
    public Viewer(Hashtable<String, ArrayList<OsmNode>> s, ArrayList<Stop> csvSt) {
        csvStops.addAll(csvSt);
        shape.putAll(s);
        initComponents();

        shapeIDs.addAll(shape.keySet());

        mainMap = mapJXMapKit.getMainMap();

        String[] items = new String[shape.size()];
        ArrayList<String> keys = new ArrayList<String>();
        keys.addAll(shape.keySet());
        for(int i=0; i<keys.size(); i++) {
            items[i] = keys.get(i);
            routeToBuild.add(keys.get(i));
        }

        for(int i=0; i<keys.size(); i++) {
            buildingShapes.add(new ArrayList<OsmNode>());
            finalTrips.add(new ArrayList<Stop>());
        }

        ComboBoxModel cbModel = new DefaultComboBoxModel(items);
        cbRoute.setModel(cbModel);

        cbRoute.setSelectedIndex(0);

        addNewRouteToMap(shape.get((String)cbRoute.getSelectedItem()));
        addOtherRouteToMap(shape, (String)cbRoute.getSelectedItem());

        addMapListener(mainMap);
    }

    private Stop findNextClosestStopFromNode(OsmNode origin, ArrayList<Stop> stopSet){
        if(origin==null || stopSet==null || stopSet.isEmpty()) return null;
        Stop dest = stopSet.get(0);
        double minDistance = OsmDistance.distVincenty(dest.getLat(), dest.getLon(), origin.getLat(), origin.getLon());
        for(int i=1; i<stopSet.size(); i++){
            Stop tempNode = stopSet.get(i);
            double distance = OsmDistance.distVincenty(tempNode.getLat(), tempNode.getLon(), origin.getLat(), origin.getLon());
            if((minDistance>distance )){
                minDistance = distance;
                dest = stopSet.get(i);
            }
        }

        return dest;
    }

    private OsmNode findNextClosestNode(OsmNode origin, OsmNode previousDest, ArrayList<OsmNode> nodeSet){
        if(origin==null || nodeSet==null || nodeSet.isEmpty()) return null;
        OsmNode dest = nodeSet.get(0);
        double minDistance = OsmDistance.distVincenty(dest.getLat(), dest.getLon(), origin.getLat(), origin.getLon());
        for(int i=1; i<nodeSet.size(); i++){
            OsmNode tempNode = nodeSet.get(i);
            double distance = OsmDistance.distVincenty(tempNode.getLat(), tempNode.getLon(), origin.getLat(), origin.getLon());
            if((minDistance==0) || (minDistance>distance && distance>0.5 && (previousDest==null || !tempNode.equals(previousDest)))){
                minDistance = distance;
                dest = nodeSet.get(i);
            }
        }

        final OsmNode tempDest = dest;

        selectedNodeOverlayPainter = new Painter<JXMapViewer>() {
            public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
                g = (Graphics2D) g.create();
                //convert from viewport to world bitmap
                Rectangle rect = map.getViewportBounds();
                //                g.translate(-rect.x, -rect.y);

                g.setColor(new Color(0,0,127,150));

                JXMapViewer mainMap = mapJXMapKit.getMainMap();

                GeoPosition st_gp = new GeoPosition(Double.parseDouble(tempDest.getLat()), Double.parseDouble(tempDest.getLon()));
                //convert to pixel
                Point2D st_gp_pt2D = mainMap.getTileFactory().geoToPixel(st_gp, mainMap.getZoom());
                //convert to screen AND left 5, up 5 to have a nice square
                Point st_gp_pt_screen = new Point((int)st_gp_pt2D.getX()-rect.x-9, (int)st_gp_pt2D.getY()-rect.y-9);
                //draw mask
                Rectangle blue_mask = new Rectangle(st_gp_pt_screen, new Dimension(25,25));
                g.fill(blue_mask);
                g.setColor(Color.BLACK);
                g.draw(blue_mask);
                g.dispose();
            }
        };
        mainPainter.setPainters(stopsPainter, selectedNodeOverlayPainter);
        mapJXMapKit.getMainMap().setOverlayPainter(mainPainter);
        mapJXMapKit.getMainMap().setZoom(1);
        HashSet<GeoPosition> temp = new HashSet<GeoPosition>();
        temp.add(new GeoPosition(Double.parseDouble(tempDest.getLat()), Double.parseDouble(tempDest.getLon())));
        mapJXMapKit.getMainMap().calculateZoomFrom(temp);

        selectedNodeFromMap = dest;

        selectedStop = findNextClosestStopFromNode(origin, csvStops);
        lblStopInfo.setText(selectedStop.getTag("Number")+" "+selectedStop.getTag("Name")+" "+selectedStop.getTag("A")+
                                                                                          ", "+selectedStop.getTag("B")+
                                                                                          ", "+selectedStop.getTag("C")+
                                                                                          ", "+selectedStop.getTag("D")+
                                                                                          ", "+selectedStop.getTag("E")+
                                                                                          ", "+Double.toString(OsmDistance.distVincenty(origin.getLat(), origin.getLon(), selectedStop.getLat(), selectedStop.getLon())));

        return dest;
    }

    private void addMapListener(JXMapViewer mainMap){
        mainMap.addMouseListener(new MouseListener() {
            private OsmNode findNode(Point mousePt){
                JXMapViewer mainMap = mapJXMapKit.getMainMap();
                Iterator it = allNodesGeo.iterator();
                while(it.hasNext()){
                    GeoPosition st_gp = (GeoPosition)it.next();
                    //convert to pixel
                    Point2D st_gp_pt2D = mainMap.getTileFactory().geoToPixel(st_gp, mainMap.getZoom());
                    //convert to screen
                    Rectangle rect = mainMap.getViewportBounds();
                    Point st_gp_pt_screen = new Point((int)st_gp_pt2D.getX()-rect.x, (int)st_gp_pt2D.getY()-rect.y);
                    //check if near the mouse
                    if(st_gp_pt_screen.distance(mousePt)<10)
                        return newNodesByGeoPos.get(st_gp);
                }
                return null;
            }
            public void mouseClicked(MouseEvent e){
                final OsmNode selected = findNode(e.getPoint());
                System.out.println("selected");
                if(selected!=null){
                    System.out.println(selected);

                    selectedNodeOverlayPainter = new Painter<JXMapViewer>() {
                        public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
                            g = (Graphics2D) g.create();
                            //convert from viewport to world bitmap
                            Rectangle rect = map.getViewportBounds();
                            //                g.translate(-rect.x, -rect.y);

                            g.setColor(new Color(0,0,127,150));

                            JXMapViewer mainMap = mapJXMapKit.getMainMap();

                            GeoPosition st_gp = new GeoPosition(Double.parseDouble(selected.getLat()), Double.parseDouble(selected.getLon()));
                            //convert to pixel
                            Point2D st_gp_pt2D = mainMap.getTileFactory().geoToPixel(st_gp, mainMap.getZoom());
                            //convert to screen AND left 5, up 5 to have a nice square
                            Point st_gp_pt_screen = new Point((int)st_gp_pt2D.getX()-rect.x-9, (int)st_gp_pt2D.getY()-rect.y-9);
                            //draw mask
                            Rectangle blue_mask = new Rectangle(st_gp_pt_screen, new Dimension(25,25));
                            g.fill(blue_mask);
                            g.setColor(Color.BLACK);
                            g.draw(blue_mask);
                            g.dispose();
                        }
                    };

                    selectedNodeFromMap = selected;

                    mainPainter.setPainters(stopsPainter, selectedNodeOverlayPainter);
                    mapJXMapKit.getMainMap().setOverlayPainter(mainPainter);
                    mapJXMapKit.getMainMap().setCenterPosition(new GeoPosition(Double.parseDouble(selected.getLat()), Double.parseDouble(selected.getLon())));
                    mapJXMapKit.getMainMap().setZoom(1);
                    /*
                    HashSet<GeoPosition> temp = new HashSet<GeoPosition>();
                    temp.add();
                    mapJXMapKit.getMainMap().calculateZoomFrom(temp);*/
                }

            }

            public void mousePressed(MouseEvent e){}

            public void mouseReleased(MouseEvent e){}

            public void mouseEntered(MouseEvent e){}

            public void mouseExited(MouseEvent e){}
        });
    }

    private void addAllNewBusStopToMap(Hashtable<String, ArrayList<OsmNode>> shape){
        HashSet<Waypoint> waypoints = new HashSet<Waypoint>();

        //to Calculate Zoom
        allNodesGeo = new HashSet<GeoPosition>();

        ArrayList<String> shapeKeys = new ArrayList<String>();
        shapeKeys.addAll(shape.keySet());
        for(int j=0; j<shapeKeys.size(); j++){
            ArrayList<OsmNode> newNodes = shape.get(shapeKeys.get(j));
            for(int i=0; i<newNodes.size(); i++){
                OsmNode st = newNodes.get(i);
                waypoints.add(new Waypoint(Double.parseDouble(st.getLat()), Double.parseDouble(st.getLon())));
                GeoPosition pos = new GeoPosition(Double.parseDouble(st.getLat()), Double.parseDouble(st.getLon()));
                allNodesGeo.add(pos);
                newNodesByGeoPos.put(pos, st);
            }
        }

        //create a WaypointPainter to draw the points
        stopsPainter.setWaypoints(waypoints);

        stopsPainter.setRenderer(new WaypointRenderer() {
            public boolean paintWaypoint(Graphics2D g, JXMapViewer map, Waypoint wp) {
/*                char[] text = "1".toCharArray();
                g.drawChars(text, 0, text.length, 10, 10);*/
                g.fillOval(0, 0, 10, 10);
                return true;
            }
        });

        mainMap.setZoom(1);
        mainMap.calculateZoomFrom(allNodesGeo);
        mainPainter.setPainters(stopsPainter);
        mainMap.setOverlayPainter(mainPainter);
    }

    private void addOtherRouteToMap(Hashtable<String, ArrayList<OsmNode>> shape, String currentSelectedRoute){
        HashSet<Waypoint> waypoints = new HashSet<Waypoint>();

        //to Calculate Zoom
        HashSet<GeoPosition> otherNodesGeo = new HashSet<GeoPosition>();

        ArrayList<String> shapeKeys = new ArrayList<String>();
        shapeKeys.addAll(shape.keySet());
        for(int j=0; j<shapeKeys.size(); j++){
            if(shapeKeys.get(j).equals(currentSelectedRoute)) continue;
            ArrayList<OsmNode> newNodes = shape.get(shapeKeys.get(j));
            for(int i=0; i<newNodes.size(); i++){
                OsmNode st = newNodes.get(i);
                waypoints.add(new Waypoint(Double.parseDouble(st.getLat()), Double.parseDouble(st.getLon())));
                GeoPosition pos = new GeoPosition(Double.parseDouble(st.getLat()), Double.parseDouble(st.getLon()));
                otherNodesGeo.add(pos);
                newNodesByGeoPos.put(pos, st);
            }
        }

        //create a WaypointPainter to draw the points
        otherStopsPainter.setWaypoints(waypoints);

        otherStopsPainter.setRenderer(new WaypointRenderer() {
            public boolean paintWaypoint(Graphics2D g, JXMapViewer map, Waypoint wp) {
/*                char[] text = "1".toCharArray();
                g.drawChars(text, 0, text.length, 10, 10);*/
                g.setColor(Color.red);
                g.fillOval(0, 0, 3, 3);
                return true;
            }
        });
    }

    private void addNewRouteToMap(ArrayList<OsmNode> route){
        HashSet<Waypoint> waypoints = new HashSet<Waypoint>();

        //to Calculate Zoom
        allNodesGeo = new HashSet<GeoPosition>();

        for(int i=0; i<route.size(); i++){
            OsmNode st = route.get(i);
            waypoints.add(new Waypoint(Double.parseDouble(st.getLat()), Double.parseDouble(st.getLon())));
            GeoPosition pos = new GeoPosition(Double.parseDouble(st.getLat()), Double.parseDouble(st.getLon()));
            allNodesGeo.add(pos);
            newNodesByGeoPos.put(pos, st);
        }

        //create a WaypointPainter to draw the points
        stopsPainter.setWaypoints(waypoints);

        stopsPainter.setRenderer(new WaypointRenderer() {
            public boolean paintWaypoint(Graphics2D g, JXMapViewer map, Waypoint wp) {
/*                char[] text = "1".toCharArray();
                g.drawChars(text, 0, text.length, 10, 10);*/
                g.fillOval(0, 0, 10, 10);
                return true;
            }
        });

        mainPainter.setPainters(stopsPainter, otherStopsPainter);
        mainMap.setOverlayPainter(mainPainter);
        mainMap.setZoom(1);
        mainMap.calculateZoomFrom(allNodesGeo);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mapJXMapKit = new org.jdesktop.swingx.JXMapKit();
        final int osmMaxZoom = 19;
        TileFactoryInfo osmInfo = new TileFactoryInfo(1,osmMaxZoom-2,osmMaxZoom,
            256, true, true, // tile size is 256 and x/y orientation is normal
            "http://tile.openstreetmap.org",//5/15/10.png",
            "x","y","z") {
            public String getTileUrl(int x, int y, int zoom) {
                zoom = osmMaxZoom-zoom;
                String url = this.baseURL +"/"+zoom+"/"+x+"/"+y+".png";
                return url;
            }
        };
        osmTf = new DefaultTileFactory(osmInfo);
        mapJXMapKit.setTileFactory(osmTf);
        btnAccept = new javax.swing.JButton();
        lblNodeInfo = new javax.swing.JLabel();
        cbRoute = new javax.swing.JComboBox();
        btnFinish = new javax.swing.JButton();
        btnAcceptStop = new javax.swing.JButton();
        lblStopInfo = new javax.swing.JLabel();
        shapeIDtf = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        btnAccept.setText("Accept Node");
        btnAccept.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAcceptActionPerformed(evt);
            }
        });

        lblNodeInfo.setFont(new java.awt.Font("Times New Roman", 1, 12));
        lblNodeInfo.setText("Info");

        cbRoute.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbRouteActionPerformed(evt);
            }
        });

        btnFinish.setText("Finish");
        btnFinish.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFinishActionPerformed(evt);
            }
        });

        btnAcceptStop.setText("Accept Stop");
        btnAcceptStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAcceptStopActionPerformed(evt);
            }
        });

        lblStopInfo.setFont(new java.awt.Font("Times New Roman", 1, 12)); // NOI18N
        lblStopInfo.setText("Info");

        jLabel1.setText("Shape ID");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mapJXMapKit, javax.swing.GroupLayout.DEFAULT_SIZE, 779, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGap(53, 53, 53)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(58, 58, 58)
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(shapeIDtf, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(533, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblStopInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 574, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnAcceptStop)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(cbRoute, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(90, 90, 90)
                        .addComponent(lblNodeInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 251, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                        .addComponent(btnAccept)
                        .addGap(56, 56, 56)
                        .addComponent(btnFinish)
                        .addGap(144, 144, 144))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAccept)
                    .addComponent(lblNodeInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnFinish)
                    .addComponent(cbRoute, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStopInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAcceptStop))
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(shapeIDtf, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(mapJXMapKit, javax.swing.GroupLayout.PREFERRED_SIZE, 443, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAcceptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAcceptActionPerformed
        // TODO add your handling code here:
        if(selectedNodeFromMap==null) return;
        ArrayList<OsmNode> selectedRoute = buildingShapes.get(cbRoute.getSelectedIndex());
        if(selectedRoute==null) return;
        double distance = 0;
        if (selectedRoute.get(0)!=null)
            distance = OsmDistance.distVincenty(selectedNodeFromMap.getLat(), selectedNodeFromMap.getLon(), selectedRoute.get(0).getLat(), selectedRoute.get(0).getLon());
        selectedNodeFromMap.setDistTraveled(Double.toString(distance));
        selectedRoute.add(selectedNodeFromMap);
        lblNodeInfo.setText("Sequence # = "+selectedRoute.size()+" "+selectedNodeFromMap.toString());
        if(selectedRoute.size()<2) findNextClosestNode(selectedNodeFromMap, null, shape.get((String)cbRoute.getSelectedItem()));
        else findNextClosestNode(selectedNodeFromMap, selectedRoute.get(selectedRoute.size() - 2), shape.get((String) cbRoute.getSelectedItem()));
        shapeIDs.set(cbRoute.getSelectedIndex(), shapeIDtf.getText());
    }//GEN-LAST:event_btnAcceptActionPerformed

    private void cbRouteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbRouteActionPerformed
        // TODO add your handling code here:
        ArrayList<OsmNode> selectedRoute = buildingShapes.get(cbRoute.getSelectedIndex());
        if(selectedRoute==null) {
            JOptionPane.showMessageDialog(this, "Invalid");
            return;
        }
        if(!selectedRoute.isEmpty()) {
            if(selectedRoute.size()<2) selectedNodeFromMap = findNextClosestNode(selectedRoute.get(selectedRoute.size()-1), null, shape.get((String)cbRoute.getSelectedItem()));
            else selectedNodeFromMap = findNextClosestNode(selectedRoute.get(selectedRoute.size()-1), selectedRoute.get(selectedRoute.size()-2), selectedRoute);
            lblNodeInfo.setText("Sequence # = "+selectedRoute.size()+" "+selectedRoute.get(selectedRoute.size()-1).toString());
        }
        else {
            selectedNodeFromMap = null;
            lblNodeInfo.setText("Sequence # = 0");
        }
        addNewRouteToMap(shape.get((String)cbRoute.getSelectedItem()));
        addOtherRouteToMap(shape, (String)cbRoute.getSelectedItem());
        mainPainter.setPainters(stopsPainter, otherStopsPainter);
        mainMap.setOverlayPainter(mainPainter);
        shapeIDtf.setText(shapeIDs.get(cbRoute.getSelectedIndex()));
    }//GEN-LAST:event_cbRouteActionPerformed

    private void btnFinishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFinishActionPerformed
        // TODO add your handling code here:
        System.out.println(buildingShapes.toString());
        if(buildingShapes.get(cbRoute.getSelectedIndex())==null) {
            JOptionPane.showMessageDialog(this, "Invalid");
            return;
        }
        finalShapes.put(cbRoute.getSelectedItem().toString(), buildingShapes.get(cbRoute.getSelectedIndex()));
        routeToBuild.remove((String)cbRoute.getSelectedItem());
        /*
         * Need to go over all shapes before writing files
         */
        if(routeToBuild.isEmpty()){
            WriteFile.exportShapeFile("shapes_manually_select.txt", finalShapes, shapeIDs);
            JOptionPane.showMessageDialog(this, "All shapes are built!");
        }
    }//GEN-LAST:event_btnFinishActionPerformed

    private void btnAcceptStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAcceptStopActionPerformed
        // TODO add your handling code here:
        ArrayList<Stop> selectedTrip = finalTrips.get(cbRoute.getSelectedIndex());
        selectedTrip.add(selectedStop);
    }//GEN-LAST:event_btnAcceptStopActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAccept;
    private javax.swing.JButton btnAcceptStop;
    private javax.swing.JButton btnFinish;
    private javax.swing.JComboBox cbRoute;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel lblNodeInfo;
    private javax.swing.JLabel lblStopInfo;
    private org.jdesktop.swingx.JXMapKit mapJXMapKit;
    private javax.swing.JTextField shapeIDtf;
    // End of variables declaration//GEN-END:variables

}
