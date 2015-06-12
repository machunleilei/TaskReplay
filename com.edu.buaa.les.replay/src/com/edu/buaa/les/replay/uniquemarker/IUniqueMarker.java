package com.edu.buaa.les.replay.uniquemarker;

import avic.actri.runtime.core.exceptions.CrossException;

public interface IUniqueMarker {
	public int transaction(final byte[] data)
		throws CrossException;
}
