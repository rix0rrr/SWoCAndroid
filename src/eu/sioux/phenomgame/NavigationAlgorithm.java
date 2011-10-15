package eu.sioux.phenomgame;

import java.io.IOException;

import org.ksoap2.serialization.Marshal;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public enum NavigationAlgorithm {
	Auto,
	BacklashOnly,
	Raw;
	
	public static class Marshaller implements Marshal 
	{
	    public Object readInstance(XmlPullParser parser, String namespace, String name, 
	            PropertyInfo expected) throws IOException, XmlPullParserException {
	        
	    	String txt = parser.nextText().intern();
	    	if (txt == "NAVIGATION-AUTO") return NavigationAlgorithm.Auto;
	    	if (txt == "NAVIGATION-BACKLASH-ONLY") return NavigationAlgorithm.BacklashOnly;
	    	if (txt == "NAVIGATION-RAW") return NavigationAlgorithm.Raw;
	    	return null;
	    }


	    public void register(SoapSerializationEnvelope cm) {
	         cm.addMapping("http://tempuri.org/om.xsd", "navigationAlgorithm", NavigationAlgorithm.class, this);
	        
	    }


	    public void writeInstance(XmlSerializer writer, Object obj) throws IOException {
	    	
	    	switch ((NavigationAlgorithm)obj) {
		    	case Auto: writer.text("NAVIGATION-AUTO"); break;
		    	case BacklashOnly: writer.text("NAVIGATION-BACKLASH-ONLY"); break;
		    	case Raw: writer.text("NAVIGATION-RAW"); break;
	    	}
	    }
	}
}
