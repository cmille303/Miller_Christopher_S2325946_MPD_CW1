package com.example.miller_christopher_s2325946.model;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * pull-parser for traffic scotland DATEX II XML feeds, turns raw XML into a list of situations and reports progress
 */
public class TrafficParser
{
    public interface ParserListener {
        void onParsingProgress(int percent);
    }

    private ParserListener listener;

    public void setParserListener(ParserListener listener) {
        this.listener = listener;
    }

    // counts <situation> blocks for the progress bar (trailing space skips <situationRecord>)
    private int countSituations(String data) {
        int count = 0;
        int index = data.indexOf("<situation ");
        while (index >= 0) {
            count++;
            index = data.indexOf("<situation ", index + 1);
        }
        return count;
    }

    // parses a full DATEX II XML feed into a list of situations
    public ArrayList<Situation> parseData(String dataToParse, boolean roadworks)
    {
        ArrayList<Situation> allSituations = new ArrayList<>();
        Situation aSituation = null;
        LocationPoint aPoint = null;

        // track which XML block we're inside since <value> is reused in a bunch of places
        boolean insideSource = false;
        boolean insideComment = false;
        boolean insideLocations = false;
        boolean insideName = false;
        String nameValue = "";

        int total = countSituations(dataToParse);
        int completed = 0;
        int lastPercent = -1;

        try
        {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput( new StringReader( dataToParse ) );
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT)
            {
                if(eventType == XmlPullParser.START_TAG)
                {
                    String tag = xpp.getName();

                    if (tag.equalsIgnoreCase("situation"))
                    {
                        aSituation = new Situation();
                        aSituation.setRoadworks(roadworks);
                    }
                    else if (aSituation == null)
                    {
                        // ignore header tags outside a situation block
                    }
                    else if (tag.equalsIgnoreCase("situationRecord"))
                    {
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            if (xpp.getAttributeName(i).equalsIgnoreCase("type")) {
                                aSituation.setRecordType(xpp.getAttributeValue(i));
                            } else if (xpp.getAttributeName(i).equalsIgnoreCase("id")) {
                                aSituation.setId(xpp.getAttributeValue(i));
                            }
                        }
                    }
                    else if (tag.equalsIgnoreCase("situationRecordCreationTime"))
                    {
                        aSituation.setCreationTime(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("situationRecordVersionTime"))
                    {
                        aSituation.setVersionTime(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("probabilityOfOccurrence"))
                    {
                        aSituation.setProbability(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("validityStatus"))
                    {
                        aSituation.setValidityStatus(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("overallStartTime"))
                    {
                        aSituation.setStartTime(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("overallEndTime"))
                    {
                        aSituation.setEndTime(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("numberOfOperationalLanes"))
                    {
                        aSituation.getImpact().setOperationalLanes(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("numberOfLanesRestricted"))
                    {
                        aSituation.getImpact().setLanesRestricted(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("delaysType"))
                    {
                        aSituation.getImpact().setDelaysType(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("source"))
                    {
                        insideSource = true;
                    }
                    else if (tag.equalsIgnoreCase("generalPublicComment"))
                    {
                        insideComment = true;
                    }
                    else if (tag.equalsIgnoreCase("groupOfLocations"))
                    {
                        insideLocations = true;
                    }
                    else if (insideLocations && (tag.equalsIgnoreCase("to") || tag.equalsIgnoreCase("from")))
                    {
                        aPoint = new LocationPoint();
                        aPoint.setRole(tag.toLowerCase());
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            if (xpp.getAttributeName(i).equalsIgnoreCase("type")) {
                                aPoint.setPointType(xpp.getAttributeValue(i));
                            }
                        }
                    }
                    else if (tag.equalsIgnoreCase("tpegDirection"))
                    {
                        aSituation.setDirection(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("latitude"))
                    {
                        String temp = xpp.nextText();
                        if (aPoint != null) {
                            aPoint.setLatitude(temp);
                        }
                    }
                    else if (tag.equalsIgnoreCase("longitude"))
                    {
                        String temp = xpp.nextText();
                        if (aPoint != null) {
                            aPoint.setLongitude(temp);
                        }
                    }
                    else if (aPoint != null && (tag.equalsIgnoreCase("name")
                            || tag.equalsIgnoreCase("ilc") || tag.equalsIgnoreCase("otherName")))
                    {
                        insideName = true;
                        nameValue = "";
                    }
                    else if (insideName && tag.toLowerCase().endsWith("descriptortype"))
                    {
                        // descriptor type comes after <value>, so pair it with the saved nameValue
                        aPoint.addName(nameValue, xpp.nextText());
                        nameValue = "";
                    }
                    else if (tag.equalsIgnoreCase("value"))
                    {
                        String temp = xpp.nextText();
                        if (insideComment) {
                            if (aSituation.getComment().isEmpty()) {
                                aSituation.setComment(temp);
                            } else {
                                aSituation.setComment(aSituation.getComment() + " " + temp);
                            }
                        }
                        else if (insideName) {
                            nameValue = temp;
                        }
                        else if (insideSource) {
                            aSituation.setSourceName(temp);
                        }
                    }
                    else if (tag.equalsIgnoreCase("roadOrCarriagewayOrLaneManagementType")
                            || tag.equalsIgnoreCase("abnormalTrafficType")
                            || tag.equalsIgnoreCase("roadMaintenanceType"))
                    {
                        aSituation.setSubType(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("complianceOption"))
                    {
                        aSituation.setComplianceOption(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("urgentRoadworks"))
                    {
                        aSituation.setUrgentRoadworks(xpp.nextText());
                    }
                }
                else if(eventType == XmlPullParser.END_TAG)
                {
                    String tag = xpp.getName();

                    if (tag.equalsIgnoreCase("situation"))
                    {
                        if (aSituation != null) {
                            aSituation.finishParsing();
                            allSituations.add(aSituation);
                            Log.d("MyTag","Situation parsing completed: " + aSituation.getId());
                        }
                        aSituation = null;
                        aPoint = null;
                        insideSource = false;
                        insideComment = false;
                        insideLocations = false;
                        insideName = false;

                        completed++;
                        if (listener != null && total > 0) {
                            int percent = (completed * 100) / total;
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                listener.onParsingProgress(percent);
                            }
                        }
                    }
                    else if (tag.equalsIgnoreCase("source"))
                    {
                        insideSource = false;
                    }
                    else if (tag.equalsIgnoreCase("generalPublicComment"))
                    {
                        insideComment = false;
                    }
                    else if (tag.equalsIgnoreCase("groupOfLocations"))
                    {
                        insideLocations = false;
                    }
                    else if (aPoint != null && (tag.equalsIgnoreCase("to") || tag.equalsIgnoreCase("from")))
                    {
                        if (aSituation != null) {
                            aSituation.addLocation(aPoint);
                        }
                        aPoint = null;
                    }
                    else if (insideName && (tag.equalsIgnoreCase("name")
                            || tag.equalsIgnoreCase("ilc") || tag.equalsIgnoreCase("otherName")))
                    {
                        if (aPoint != null && !nameValue.isEmpty()) {
                            // fallback if there wasn't a descriptor type tag
                            aPoint.addName(nameValue, "");
                        }
                        insideName = false;
                        nameValue = "";
                    }
                }
                eventType = xpp.next();
            }
        }
        catch (XmlPullParserException ae1)
        {
            Log.e("MyTag","Parsing error" + ae1.toString());
        }
        catch (IOException ae1)
        {
            Log.e("MyTag","IO error during parsing");
        }
        catch (RuntimeException ae1)
        {
            // don't crash on weird feed data, just return whatever we already parsed
            Log.e("MyTag","Unexpected data during parsing" + ae1.toString());
        }

        Log.d("MyTag","End of document reached. Situations found: " + allSituations.size());
        return allSituations;
    }

}
