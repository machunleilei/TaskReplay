package com.edu.buaa.les.replay.process;

import avic.actri.runtime.core.exceptions.CrossException;
import avic.actri.targetserver.core.IEvtDispatchPort;
import avic.actri.targetserver.core.IEvtProc;
import avic.actri.targetserver.core.ITarget;
import avic.actri.targetserver.core.cmdparam.TargetAddr;

public interface IMyEventProc extends IEvtProc {
	public IEvtDispatchPort getPort();
	public TargetAddr getBreakpointId();
	public boolean isBind();
	
	/* 注册和反注册 */
	public boolean registe(avic.actri.targetserver.core.cmdparam.Context context, long addr)
		throws CrossException;
	public boolean unregiste() throws CrossException;
}
