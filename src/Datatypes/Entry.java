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
	
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof Entry)) {
			//System.out.println(values+"::"+other);
			return false;
		}
		Entry otherEntry = (Entry) other;
		if (otherEntry.values.size() != this.values.size()){
			//System.out.println(values+":::"+otherEntry.values);
			return false;
		}
		for(int i = 0; i < values.size(); i++)
			if(!otherEntry.values.get(i).equals(this.values.get(i))){
				//System.out.println(values+":"+otherEntry.values);
				return false;
			}
		//System.out.println(values+":"+otherEntry.values);
		return true;
	}

}
