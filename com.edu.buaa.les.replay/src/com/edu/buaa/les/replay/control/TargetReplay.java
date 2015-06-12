package com.edu.buaa.les.replay.control;
import com.edu.buaa.les.replay.process.IMyEventProc;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import com.edu.buaa.les.replay.global.ConstSymbol4Replay;
import com.edu.buaa.les.replay.global.TransactionData;
import com.edu.buaa.les.replay.uniquemarker.IUniqueMarker;

import avic.actri.runtime.core.exceptions.CrossException;
import avic.actri.targetserver.TsActivator;
import avic.actri.targetserver.api.TsApi;
import avic.actri.targetserver.core.IEvtDispatchPort;
import avic.actri.targetserver.core.IEvtDispatcher;
import avic.actri.targetserver.core.ITarget;
import avic.actri.targetserver.core.cmdparam.AgentMode;
import avic.actri.targetserver.core.cmdparam.CtxType;
import avic.actri.targetserver.core.cmdparam.DomainId;
import avic.actri.targetserver.core.cmdparam.TargetAddr;
import avic.actri.targetserver.core.evt.EvtConstants;
import avic.actri.targetserver.core.evt.EvtDispatchPortObject;

public class TargetReplay {
	/* 目标机代理 */
	private ITarget target = null;
	
	private TargetAddr breakpointId = null;
	
	private static TargetReplay instance = null;
	static{
		instance = null;
	}
	
	private TargetReplay() {
		// TODO Auto-generated constructor stub
		target = null;
	}
	
	/* 初始化并设置模式 */
	private TargetReplay(ITarget target) throws CrossException {
		this.target = target;
		/* 目标端服务器模式设置 */
		TsApi.targetModeSet(target, AgentMode.MODE_TASK);
	}
	
	/* 构造一个单例模式 */
	public static TargetReplay getInstance(ITarget target) throws CrossException{
		if(instance == null){
			instance = new TargetReplay(target);
		}
		return instance;
	}
	
	public static TargetReplay getInstance() throws NullPointerException{
		if(instance != null)
			return instance;
		else{
			System.err.println("清确保最少通过参数调用过getInstance函数");
			throw new NullPointerException("清确保最少通过参数调用过getInstance函数");
		}
	}
	
	/* 设置重放的target */
	public void setReplayTarget(ITarget target) throws CrossException{
		this.target = target;
		TsApi.targetModeSet(target, AgentMode.MODE_TASK);
	}
	
	/* 获取管理的target */
	public ITarget getTarget(){
		return target;
	}
	
	/* 创建上下文 */
	public avic.actri.targetserver.core.cmdparam.Context createContext(
			int contextType, long contextId){
		return new avic.actri.targetserver.core.cmdparam.Context(
				target, contextType, contextId, 0);
	}
	
	/* 创建上下文 */
	public long createContext(String taskName, Integer stackBase, Integer stackSize,
			Long entryPoint, long[] args, Integer priority, Integer options)
		throws CoreException{
		/*return TsApi.createContext(target, DomainId.KERNEL_DOMAINID, 
				CtxType.CTX_TASK, stackBase, stackSize, entryPoint, 
				args, taskName, priority, options, 0, 0, 0);*/
		return TsApi.createContext(target, DomainId.KERNEL_DOMAINID, 
				CtxType.CTX_TASK, stackBase, stackSize, entryPoint, 
				args, taskName, priority, options, 0, 0, 0);
	}
	
	/* 获取目标机端事件分发 */
	public static IEvtDispatcher getEventDispatcher() throws CrossException{
		return TsActivator.getDefault().getTargetServer().getEvtDispatcher();
	}
	
	/* 设置断点,并设置断点服务函数 */
	public void addBreakPoint(avic.actri.targetserver.core.cmdparam.Context context, 
			long addr,
			IMyEventProc proc) throws CrossException{
		proc.registe(context, addr);
	}
	
	/* 设置断点，并设置服务函数，与addBreakPoint不同之处在于该函数设置的断点会被所有的上下文命中 */
	public void addBreakPointWithoutContext(long addr, IMyEventProc proc)
		throws CrossException{
		avic.actri.targetserver.core.cmdparam.Context context =
				new avic.actri.targetserver.core.cmdparam.Context(
						target, CtxType.CTX_ANY_TASK, -1, 0);
		addBreakPoint(context, addr, proc);
	}
	
	/* 删除断点 */
	public void removeBreakPoint(IMyEventProc proc) 
		throws CrossException{
		/* 删除断点的时候需要做两件事
		 * 1. 删除断点
		 * 2. 删除对应的处理过程 */
		proc.unregiste();
	}
	
	/* 挂起上下文 */
	public void suspendContext(avic.actri.targetserver.core.cmdparam.Context context)
		throws CrossException{
		TsApi.suspendContext(target, DomainId.KERNEL_DOMAINID, context);
	}
	
	/* 继续执行上下文 */
	public void continueContext(avic.actri.targetserver.core.cmdparam.Context context)
		throws CrossException{
		TsApi.continueContext(target, DomainId.KERNEL_DOMAINID, context);
	}
	
	/* 恢复上下文执行 */
	public void resumeContext(avic.actri.targetserver.core.cmdparam.Context context)
		throws CrossException{
		TsApi.resumeContext(target, DomainId.KERNEL_DOMAINID, context);
	}
	
	/* 
	 * @param： 任务的上下文标示
	 * @return： 寄存器组标示
	 * 获取寄存器的数值 */
	public long[] getContextRegs(avic.actri.targetserver.core.cmdparam.Context context,
			int regSetType) throws CrossException{
		return TsApi.readContextRegs(target, DomainId.KERNEL_DOMAINID,context,
				regSetType);
				//RegSetType.REG_GPR_GROUP);
	}
	
	private List<Long> getAddressBySymbol(String symbol, boolean more)
		throws CrossException{
		List<Long> adds = target.findAddr(symbol);
		if (adds.size() == 0) {
			throw new CrossException("获取 " + symbol + " 的地址失败");
		}
		if(adds.size() > 1 && ! more){
			throw new CrossException("获取 " + symbol + " 的多个地址");
		}
		
		return adds;
	}
	/* 
	 * @param: elf文件中对应的符号表中的符号
	 * @return: 返回符号对应的地址，可能有多个，因为符号给定的不是很完全导致匹配到多个符号 */
	public long getAddressBySymbol(String symbol) throws CrossException{
		return getAddressBySymbol(symbol, false).iterator().next();
	}

	/* 
	 * @param 任务的ID标识
	 * @return 运算得到的唯一表示服 */
	public int getUniqueMarkByContext(int taskId,
			IUniqueMarker marker) throws CrossException{
		long functionCallAddress = getAddressBySymbol(ConstSymbol4Replay.SYMBOL_GET_TASK_STACK);	
		/* 根据找到的函数的入口地址，调用该函数，并取得返回值 */
		/* 构造10个参数 */
		long dataAddress = getAddressBySymbol(ConstSymbol4Replay.SYMBOL_COMMON_REGION);
		
		long[] args = {
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0
		};
		
		args[0] = taskId;
		args[1] = dataAddress;
		/* 调用制定的函数 */
		TsApi.directCall(target, functionCallAddress, args);
		
		/* target数据区域中读取数据 */
		byte[] stackInfo = TsApi.readMem(target, DomainId.KERNEL_DOMAINID, 
				dataAddress, ConstSymbol4Replay.TASK_STACK__INFORMATION_STRUCT_SIZE);
		/* 从获取的数据中得到一个 int */
		long stackBase = TransactionData.toInt(stackInfo, true);
		stackInfo = Arrays.copyOfRange(stackInfo, 4, stackInfo.length);
		
		/* 从获取的数据中得到一个 int */
		int stackLen = TransactionData.toInt(stackInfo, true);
		
		/* 获取堆栈数据 */
		byte[] stackDatas = TsApi.readMem(target, DomainId.KERNEL_DOMAINID,
				stackBase, stackLen);
		/* 对堆栈进行校验和 */
		return marker.transaction(stackDatas);
	}
}
