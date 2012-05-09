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

package edu.usf.cutr.gtfs_builder.parser;

import edu.usf.cutr.gtfs_builder.object.OsmNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import edu.usf.cutr.gtfs_builder.object.RelationMember;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Khoa Tran
 */
public class RelationParser extends DefaultHandler {
    private Hashtable tempTag;
    private HashSet<RelationMember> tempMembers;
    private ArrayList<AttributesImpl> xmlRelations;
    //xmlTags<String, String> ----------- xmlMembers<String(refID), AttributesImpl>
    private ArrayList<Hashtable> xmlTags;
    private ArrayList<HashSet<RelationMember>> xmlMembers;
    private AttributesImpl attImplRel;
    private boolean shouldCollected;
    private Hashtable<String, OsmNode> nodeCollection = new Hashtable<String, OsmNode>();
    private Hashtable<String, ArrayList<String>> wayCollection = new Hashtable<String, ArrayList<String>>();
    private ArrayList<String> tempNodeInWay;
    private String tempWayId = "", tempRelationId = "";
    private ArrayList<OsmNode> nodeInAShape;
    private Hashtable<String, ArrayList<OsmNode>> shape = new Hashtable<String, ArrayList<OsmNode>>();
    private String tempRoute = null;
    
    public RelationParser(){
        xmlRelations = new ArrayList<AttributesImpl>();
        xmlTags = new ArrayList<Hashtable>();
        xmlMembers = new ArrayList<HashSet<RelationMember>>();
        shouldCollected = false;
        tempRelationId = "";
        tempWayId = "";
    }
    
    @Override public void startElement(String namespaceURI, String localName, String qname, Attributes attributes) throws SAXException {
        if (qname.equals("relation")) {
            nodeInAShape = new ArrayList<OsmNode>();
            shouldCollected = false;
            attImplRel = new AttributesImpl(attributes);
            tempRelationId = attImplRel.getValue("id");
            tempTag = new Hashtable();      // start to collect tags of that relation
            tempMembers = new HashSet<RelationMember>();
        } else if(qname.equals("node")) {
            AttributesImpl attImpl = new AttributesImpl(attributes);
            nodeCollection.put(attImpl.getValue("id"), new OsmNode(attImpl.getValue("lat"), attImpl.getValue("lon")));
        } else if(qname.equals("way")) {
            AttributesImpl attImpl = new AttributesImpl(attributes);
            tempNodeInWay = new ArrayList<String>();
            tempWayId = attImpl.getValue("id");
        }

        if (qname.equals("nd") && !tempWayId.equals("")) {
            AttributesImpl attImpl = new AttributesImpl(attributes);
            tempNodeInWay.add(attImpl.getValue("ref"));
        }
        
        if (tempMembers!=null && qname.equals("member") && !tempRelationId.equals("")) {
            AttributesImpl attImpl = new AttributesImpl(attributes);
            if(attImpl.getValue("type").equals("node")){
                nodeInAShape.add(nodeCollection.get(attImpl.getValue("ref")));
            } else if(attImpl.getValue("type").equals("way")){
                ArrayList<String> nodesInWay = wayCollection.get(attImpl.getValue("ref"));
                if(nodesInWay==null) {
                    System.out.println(attImpl.getValue("ref")+" doesn't find any match");
                    return;
                }
                for(int i=0; i<nodesInWay.size(); i++){
                    nodeInAShape.add(nodeCollection.get(nodesInWay.get(i)));
                }
            }
        }

        if(qname.equals("tag") && !tempRelationId.equals("")){
            AttributesImpl attImpl = new AttributesImpl(attributes);
            if(attImpl.getValue("v")!=null && attImpl.getValue("v").equals("USF Bull Runner")) shouldCollected = true;
            
            if(attImpl.getValue("k")!=null && attImpl.getValue("k").equals("ref")) tempRoute = attImpl.getValue("v");
        }
    }

    @Override public void endElement (String uri, String localName, String qName) throws SAXException {
        if (qName.equals("relation") && shouldCollected) {
            if(tempRoute!=null){
                for(int i=0; i<nodeInAShape.size(); i++){
                    OsmNode n = nodeInAShape.get(i);
                    n.setRoute(tempRoute);
                }
            }
            shape.put(tempRelationId, nodeInAShape);
            shouldCollected = false;
            tempRelationId = "";
            xmlRelations.add(attImplRel);
            xmlTags.add(tempTag);
            xmlMembers.add(tempMembers);
            tempTag = null;
            tempMembers = null;
            tempRoute = null;
        } else if(qName.equals("way") && !tempWayId.equals("")) {
            wayCollection.put(tempWayId, tempNodeInWay);
            tempWayId = "";
        }
    }

    public Hashtable<String, ArrayList<OsmNode>> getShape(){
        return shape;
    }
}