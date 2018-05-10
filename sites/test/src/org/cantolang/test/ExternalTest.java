/* Canto Test Suite: ExternalTest.java
 *
 * Copyright (c) 2018 by cantolang.org
 */

package org.cantolang.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


/** ExternalTest is designed to help the Canto test suite (test.can) test
 *  the external object interface in a Canyo implementation.  Tests include
 *  access of both fields and methods, both static and instance.
 *
 */
public class ExternalTest {

    public static String a = "A";
    public static String b() {
        return "B";
    }

    public String c = "C";
    private String str;

    public ExternalTest() {
        this("D");
    }

    public ExternalTest(String str) {
        this.str = str;
    }

    public String d() {
        return str;
    }

    public String e() {
        return str;
    }

    public String f() {
        return str;
    }

    public String g() {
        return "G";
    }

    public String h() {
        return str;
    }

    public String i() {
        return str;
    }

    public String j() {
        return str;
    }

    public String k() {
        return "K";
    }

    public String r() {
        return str;
    }
    
    public String show(String x) {
        return x;
    }
    
    public String show() {
        return "x";
    }
    
    public String show_value() {
        return str;
    }
    
    public String toString() {
        return str;
    }
    
    public Object[] bigObjects() {
        Object[] bo = new Object[1500];
        for (int i=0; i < 1500; i++) {
        	bo[i] = new byte[1048576];
        }
        return bo;
    }
    
    public OtherExternalObject other(String y) {
    	return new OtherExternalObject(y);
    }
    
    public OtherExternalObject other() {
        return new OtherExternalObject("x");
    }

    public class OtherExternalObject {
	    String z;
	    public OtherExternalObject(String z) {
		    this.z = z;
	    }
	
	    public String methodZ() {
	    	return z;
	    }
	}
    
    public Map[] mapArray(String a, String b) {
       Map<String, String> map1 = new HashMap<String, String>(1);
       map1.put("item", a);
            
       Map<String, String> map2 = new HashMap<String, String>(1);
       map2.put("item", b);
       
       Map[] maps = new Map[2];
       maps[0] = map1;
       maps[1] = map2;
        
       return maps;
    }

    public String concatElements(Object[] elements) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < elements.length; i++) {
            sb.append(elements[i].toString());
        }
        return sb.toString();
    }
    
    public List<String> extList() {
        List<String> list = new ArrayList<String>();
        list.add("first in list  ");
        list.add("second in list  ");
        list.add("third in list  ");
        return list;
    }
    
    public Map<String, String> extMap() {
        Map<String, String> map = new HashMap<String, String>();
        
        map.put("1", "first in map  ");
        map.put("2", "second in map  ");
        map.put("3", "third in map  ");
        return map;
    }
}
