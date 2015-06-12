package com.edu.buaa.les.replay.uniquemarker;

import avic.actri.runtime.core.exceptions.CrossException;

public class Crc32Marker implements IUniqueMarker {

	@Override
	public int transaction(byte[] data) throws CrossException {
		// TODO Auto-generated method stub
		int idx = -1;
		int retResult = 0;
		while(++ idx < data.length){
			retResult ^= data[idx];
		}
		return retResult;
	}

}
