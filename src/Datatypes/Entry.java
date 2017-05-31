package Datatypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import hlib.hj.mlib.HomoSearch;

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
	
	public int size(){
		return values.size();
	}
	
	public Object getElem(int pos){
		return values.get(pos);
	}
	
	public void addCustomElem(Object val){
		values.add(val);
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
	
	public boolean search(Entry other, boolean encrypted){
		for(int i = 0; i < values.size() && i< other.size(); i++)
			if(values.get(i) != null)
				if(encrypted){
					if(HomoSearch.searchAll((String)values.get(i), (String)other.getElem(i)))
						return true;
				}
				else
					if(((String)values.get(i)).contains((String)other.getElem(i)))
						return true;
		return false;
	}
	
	public static Entry randomEntry(Object[] config, String[] strings, int maxInt){
		Entry res = new Entry();
		Random r = new Random();
		int l = config.length;
		for(Object o: config)
			if(o instanceof String)
				res.addCustomElem(strings[r.nextInt(l)]);
			else
				res.addCustomElem(1+r.nextInt(maxInt));
		return res;
	}

}
