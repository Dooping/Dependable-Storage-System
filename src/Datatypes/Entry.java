package Datatypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Entry implements Serializable{
	private static final long serialVersionUID = 1L;

	public List<Object> values;
	
	@Override
	public String toString() {
		return "Entry [values=" + values + "]";
	}

	public Entry(){
		super();
		values = new ArrayList<Object>();
	}
	
	public Entry(Object... valueList) {
		super();
		values = new ArrayList<Object>();
		for(Object o : valueList)
			values.add(o);
	}
	
	public Object getElem(int pos){
		return values.get(pos);
	}
	
	public void addElem(){
		values.add(null);
	}

}
