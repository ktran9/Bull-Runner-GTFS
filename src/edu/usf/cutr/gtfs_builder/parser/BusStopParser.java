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

import java.util.ArrayList;
import java.util.Hashtable;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Khoa Tran
 */
public class BusStopParser extends DefaultHandler{
    private Hashtable tempTag;
    private AttributesImpl attImplNode;
    private ArrayList<AttributesImpl> xmlNodes;
    private ArrayList<Hashtable> xmlTags;
    private boolean shouldCollect = false;
    public BusStopParser(){
        xmlNodes = new ArrayList<AttributesImpl>();
        xmlTags = new ArrayList<Hashtable>();
    }
    
    @Override public void startElement(String namespaceURI, String localName, String qname, Attributes attributes) throws SAXException {
        if (qname.equals("node") || qname.equals("changeset")) {
            attImplNode = new AttributesImpl(attributes);
            tempTag = new Hashtable();      // start to collect tags of that node
        }
        if (qname.equals("tag")) {
            AttributesImpl attImpl = new AttributesImpl(attributes);
            //                System.out.println(attImpl.getValue("k") + attImpl.getValue("v"));
            String key = attImpl.getValue("k");
            String value = attImpl.getValue("v");
            if(key.equals("highway") && value.equals("bus_stop")){
                tempTag.put(key, value);         // insert key and value of that tag into Hashtable
                xmlNodes.add(attImplNode);       // add the node attribute to list
                shouldCollect = true;
            }
        }
    }

    @Override public void endElement (String uri, String localName, String qName) throws SAXException {
        if (qName.equals("node") && shouldCollect) {
            xmlTags.add(tempTag);
            shouldCollect = false;      // reset the flag
        }
    }

    public AttributesImpl getOneNode(){
        return attImplNode;
    }

    public Hashtable getTagsOneNode(){
        return tempTag;
    }

    public ArrayList<AttributesImpl> getNodes(){
        return xmlNodes;
    }

    public ArrayList<Hashtable> getTags(){
        return xmlTags;
    }
}