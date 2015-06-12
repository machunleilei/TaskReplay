package com.edu.buaa.les.replay.global;

import avic.actri.runtime.core.exceptions.CrossException;


public class TransactionData {
	
	public static int toInt(byte[] data, boolean sameStyle)
		throws CrossException{
		int ret = 0;
		if(data.length < 4){
			throw new CrossException("toInt: 传入的数据长度<4");
		}
		if(sameStyle){
			ret = data[0];
			ret = (ret << 8) + data[1];
			ret = (ret << 8) + data[2];
			ret = (ret << 8) + data[3];
			return ret;
		}
		
		ret = ((int)data[3] << 24);
		ret = ret | (((int)data[2]) << 16);
		ret = ret | (((int)data[1]) << 8);
		ret = ret | data[0];
		return 0;
	}
	
	public static long toLong(byte[] data, boolean sameStyle)
		throws CrossException{
		long ret = 0;
		if(data.length < 8){
			throw new CrossException("toLong: 传入的数据长度<8");
		}
		if(sameStyle){
			ret = data[0];
			ret = (ret << 8) + data[1];
			ret = (ret << 8) + data[2];
			ret = (ret << 8) + data[3];
			ret = (ret << 8) + data[4];
			ret = (ret << 8) + data[5];
			ret = (ret << 8) + data[6];
			ret = (ret << 8) + data[7];
			return ret;
		}
		
		ret = ((long)data[7]) << 56; 
		ret = ret | (((long)data[6]) << 48);
		ret = ret | (((long)data[5]) << 40);
		ret = ret | (((long)data[4]) << 32);
		ret = ret | (((long)data[3]) << 24);
		ret = ret | (((long)data[2]) << 16);
		ret = ret | (((long)data[1]) << 8);
		ret = ret | data[0];
		return ret;
	}
}
