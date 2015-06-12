package com.edu.buaa.les.replay.process;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.expressions.AndExpression;

import avic.actri.runtime.core.exceptions.CrossException;
import avic.actri.targetserver.TsActivator;
import avic.actri.targetserver.api.TsApi;
import avic.actri.targetserver.core.IEvtDispatchPort;
import avic.actri.targetserver.core.TargetBp;
import avic.actri.targetserver.core.cmdparam.CtxType;
import avic.actri.targetserver.core.cmdparam.DomainId;
import avic.actri.targetserver.core.cmdparam.TargetAddr;
import avic.actri.targetserver.core.evt.Evt;
import avic.actri.targetserver.core.evt.EvtConstants;
import avic.actri.targetserver.core.evt.EvtDispatchPortObject;
import avic.actri.targetserver.core.evtdata.BpInfo;
import avic.actri.targetserver.core.evtdata.EvtData;
import avic.actri.targetserver.core.evtdata.IEvtData;

import com.edu.buaa.les.replay.control.LogManager;
import com.edu.buaa.les.replay.control.TargetReplay;
import com.edu.buaa.les.replay.global.ConstSymbol4Replay;
import com.edu.buaa.les.replay.global.TargetInformation;

public class BreakpointProcess implements IMyEventProc{

	private BreakPointStruct breakPointInfo = new BreakPointStruct();
	
	/* 构造函数 */
	
	@Override
	public void proc(Evt evt) {
		// TODO Auto-generated method stub
		BpInfo bpInfo = null;
		IEvtData data = null;
		EvtData evtData = (EvtData)evt.getEvtData();
		bpInfo = (BpInfo)evtData.getEvtinfo();
	/*	
		if (data instanceof BpInfo) {
			System.err.println("data is type of BpInfo");
			bpInfo = (BpInfo)data;
		}else if(data instanceof EvtData){
			evtData = (EvtData)data;
			System.err.println("data is type of EvtData");
		}else if (data instanceof CallRetInfo) {
			System.err.println("data is type of CallRetInfo");
		}else if (data instanceof MemXfer) {
			System.err.println("data is type of MemXfer");
		}else {
			System.err.println("data is type of unknown");
		}*/
		
		/* 通过TargetReplay.getinstance获取TargetReplay */
		TargetReplay replayControl = TargetReplay.getInstance();
		
		System.out.println("进入事件处理过程中");
		/* 从BpInfo中获取信息 */
		avic.actri.targetserver.core.cmdparam.Context context =
				bpInfo.getContext();
		System.out.println("the address is: " + bpInfo.getAddr());
		
		
		/* Context的名字就是任务的名字 */
		List<TargetInformation.TaskInformation> taskInfos = null;
		try {
			taskInfos = TargetInformation.getTaskInformationsFromTarget(
					replayControl.getTarget());
		} catch (CrossException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("在断点处理函数中获取Context信息失败");
		}
		TargetInformation.ContextInformation contextInformation = null;
		for(TargetInformation.TaskInformation info: taskInfos){
			System.out.println(info);
		}
		
		
		try {
			/* 判断是否到达了切换点  */
			if(LogManager.getInstance().isArraivedPoint(context, bpInfo.getAddr())){
				/* 寻找下一个任务 */
				if(LogManager.getInstance().hasNext()){
					LogManager.getInstance().next();
				}
				
				if(! LogManager.getInstance().isValid(LogManager.getInstance().getSwapOutTaskContext())){
					/* 寻找下一个任务 */
					if(LogManager.getInstance().hasNext()){
						LogManager.getInstance().next();
					}
					System.err.println("find a new contextswitch");
				}
				
				List<TargetBp> breakpoints = null;
				try{
					breakpoints = TargetInformation.getBreakPointInfo(TargetReplay.getInstance().getTarget());
				}catch(CrossException exception){
					System.err.println("获取目标端断点信息错误");
				}	
				
				/* 获取系统中所有断点 */
				for (TargetBp targetBp : breakpoints) {
					System.err.println(targetBp.getAddr());
				}
				/* 删除当前断点 */
				System.err.println("delete the old breakpoint");
				unregiste();
				
				System.err.print("will swap out: " + context.getMainid());
				System.err.println(", and swap in: " + LogManager.getInstance().getSwapOutTaskContext().getMainid());
				/* 设置下一个断点 */
				System.err.println("will add breakpoint at: " + LogManager.getInstance().getBreakPointAddress());
				TargetReplay.getInstance().addBreakPoint(
						LogManager.getInstance().getSwapOutTaskContext(),
						LogManager.getInstance().getBreakPointAddress(), this);
				
				/* 启动新的任务运行 */
				//TargetReplay.getInstance().suspendContext(context);
				TargetReplay.getInstance().continueContext(LogManager.getInstance().getSwapOutTaskContext());
			}else{
				/* 还没有到达切换点，继续执行 */
				System.err.println("has not meet the switch point, go on...");
				TargetReplay.getInstance().continueContext(context);
			}
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		/* 判断是否得到要求 */
		/* 调用日志判断器来判断是否到达了目的点 */
		/* boolean arrived = isArrive(context, pc) */
		
		/* 如果到达了,删除断点, 然后调用日志得到下一个断点地址 */
		/*
		 * getNextBreakPointAddress()
		 */
		
		
		
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				/*
				 * 一般用于更新界面
				 */
			}
		});
//		TsApi.readContextRegs(target, domain, context, regSetType)
		//TsApi.addBp(target, domain, context, addr);
	}

	@Override
	public IEvtDispatchPort getPort() {
		// TODO Auto-generated method stub
		return breakPointInfo.getPort();
	}

	@Override
	public TargetAddr getBreakpointId() {
		// TODO Auto-generated method stub
		return breakPointInfo.getBreakPointIdentity();
	}

	@Override
	public boolean isBind(){
		return breakPointInfo.isHasBind();
	}
	
	@Override
	public boolean registe(avic.actri.targetserver.core.cmdparam.Context context, 
			long addr) throws CrossException {
		// TODO Auto-generated method stub
		TargetAddr breakpointId = TsApi.addBp(
				TargetReplay.getInstance().getTarget(), 
				DomainId.KERNEL_DOMAINID, context, addr);
		breakPointInfo.setBreakPointIdentity(breakpointId);
		IEvtDispatchPort port = new EvtDispatchPortObject(this);
		breakPointInfo.setPort(port);
		boolean ret = TargetReplay.getEventDispatcher().registeEvt(EvtConstants.EVT_BP, port);
		if(ret){
			breakPointInfo.setHasBind(true);
		}else{
			throw new CrossException("注册断点失败");
		}
		return ret;
	}

	@Override
	public boolean unregiste() throws CrossException{
		// TODO Auto-generated method stub
		/* 删除断点的时候需要做两件事
		 * 1. 删除断点
		 * 2. 删除对应的处理过程 */
		boolean ret = TargetReplay.getEventDispatcher().unregisteEvt(
				EvtConstants.EVT_BP, breakPointInfo.getPort());
		if(ret){
			breakPointInfo.setHasBind(false);
		}else{
			throw new CrossException("反注册断点失败");
		}
		TsApi.delBp(TargetReplay.getInstance().getTarget(), 
				breakPointInfo.getBreakPointIdentity());
		return ret;
	}
	
	/*
	 * 定义一个断点相关信息结构体
	 */
	public class BreakPointStruct{
		private boolean hasBind = false;
		/* 该断点绑定的端口处理 */
		private IEvtDispatchPort port = null;
		/* 该断点的标示 */
		private TargetAddr breakPointIdentity;
		
		/* 构造函数 */
		public BreakPointStruct(){
			hasBind = false;
			port = null;
			breakPointIdentity = null;
		}
		
		public BreakPointStruct(TargetAddr addr){
			this.hasBind = false;
			this.breakPointIdentity = addr;
		}
		
		public BreakPointStruct(TargetAddr id, IEvtDispatchPort port){
			this.breakPointIdentity = id;
			this.port = port;
			this.hasBind = true;
		}
		
		
		public boolean isHasBind() {
			return hasBind;
		}
		
		public void setHasBind(boolean hasBind) {
			this.hasBind = hasBind;
		}
		public IEvtDispatchPort getPort() {
			return port;
		}
		public void setPort(IEvtDispatchPort port) {
			this.port = port;
		}
		public TargetAddr getBreakPointIdentity() {
			return breakPointIdentity;
		}
		public void setBreakPointIdentity(TargetAddr breakPointIdentity) {
			this.breakPointIdentity = breakPointIdentity;
		}
		
		
	}
	
}
