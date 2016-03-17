package org.jbpm.more;

import java.util.Map;
import java.util.Map.Entry;

public class ExceptionService {

    public void handleException(Map<String, Object> inputMap) { 
        System.out.println("HANDLING EXCEPTION: " );
        if( inputMap != null ) { 
            for( Entry<String, Object> entry : inputMap.entrySet() ) { 
                System.out.println( "> " + entry.getKey() + ": " + entry.getValue() );
            }
        } else { 
            System.out.println("EMPTY input map.");
        }
    }
}
