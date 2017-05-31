package auxiliary;

import java.util.ArrayList;


/*Some encrypt operands may require more than 1 key, so create a KeyStorage for each entry's value/column/operand */
public class KeyStorage {

	private ArrayList<Object> keys;
	public KeyStorage(){
		keys = new ArrayList<Object>();
	}

	public void addKey(Object key){
		keys.add(key);
	}
	
	public Object getKey(int i){
		return keys.get(i);
	}
	
	public int size(){
		return keys.size();
	}
	
	public boolean hasPos(int pos){
		return pos<keys.size();
	}
	
}
