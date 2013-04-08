package com.test;

import java.util.Map;

public class MyService extends TaskExecutor {

    public Map<String, Object> execute(Object object) { 
        String type = "null";
        if( object != null ) { 
            type = object.getClass().getCanonicalName();
        }
        System.out.println("Input is " + type );
        throw new RuntimeException("Exception thrown!");
    }
    
    public boolean execute(Map<String, Object> input) { 
        System.out.println("My Service is being executed.");
        return false;
    }
    
	public Map<String, Object> runTask(Map<String, Object> input) throws Exception{
		int i=2;
		int j=2;
		
		int total = i + j;
		if( total == 5 ) {  // parallel realities... ;) 
		    input.put("total", total );
		} else { 
		    throw new RuntimeException("2 + 2 should equal 5!");
		}
		
		return input;
	}
	

}
