package com.edu.buaa.les.replay.control;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import avic.actri.runtime.core.exceptions.CrossException;
import avic.actri.targetserver.core.cmdparam.CtxType;
import avic.actri.targetserver.core.cmdparam.DomainId;
import avic.actri.targetserver.core.cmdparam.TargetAddr;

import com.edu.buaa.les.log.core.*;
import com.edu.buaa.les.log.core.BaseLogStructInfo.LogCategory;
import com.edu.buaa.les.log.parser.TaskDelayLogParser;
import com.edu.buaa.les.log.struct.ContextSwitchLogStruct;
import com.edu.buaa.les.log.struct.CpuIndexLogStruct;
import com.edu.buaa.les.log.struct.InterruptExitRescheduleLogStruct;
import com.edu.buaa.les.log.struct.MessageQueueReceiveLogStruct;
import com.edu.buaa.les.log.struct.MessageQueueSendLogStruct;
import com.edu.buaa.les.log.struct.SemaphoreObtainLogStruct;
import com.edu.buaa.les.log.struct.SynchronousBaseLogStruct;
import com.edu.buaa.les.log.struct.SynchronousRescheduleLogStruct;
import com.edu.buaa.les.log.struct.TaskDelayLogStruct;
import com.edu.buaa.les.log.struct.TaskStartLogStruct;
import com.edu.buaa.les.replay.global.ConstSymbol4Replay;
import com.edu.buaa.les.replay.global.TargetInformation;
import com.edu.buaa.les.replay.global.TaskInformationManager;
import com.edu.buaa.les.replay.global.TaskInformationManager.TaskInformation4Repay;

public class LogManager {
	List<BaseLogStructInfo> logs = null;
	Map<String, Integer> mapTaskName2TaskId = null;
	
	
	/* TaskInformationManager对象 */
	private TaskInformationManager taskInformationManager = 
			TaskInformationManager.getInstance();
	/* Target端重放控制 */
	private TargetReplay targetReplay = TargetReplay.getInstance();
	
	/* 当前CPU的索引 */
	private Integer currentCpuIndex = null;
	/* 从上次到这次CPU是否发生了变化 */
	private boolean cpuIndexChanged = false;
	
	private Integer cpuCoreNums = 2;
	/* 每个内核上等待的事件 */
	
	/* 保存事件遍历中的迭代器 */
	/* 对于多CPU而言，应该每一个CPU维护一个iterator */
	ListIterator<BaseLogStructInfo> iterator4ReplayLogs = null;
	
	/* 指示是不是第一次获取重放的任务，任务重放任务的第一个获取比较特殊 */
	private boolean isFirstGetContext = true;
	
	/* 定义一个无效的context用来指示切换到了不关心的任务上了  */
	avic.actri.targetserver.core.cmdparam.Context  invalidContext= 
			targetReplay.createContext(CtxType.CTX_TASK, -1);
	
	public boolean isValid(avic.actri.targetserver.core.cmdparam.Context context){
		return context.getMainid() != invalidContext.getMainid();
	}
	
	/* 保存正在运行的任务context */
	avic.actri.targetserver.core.cmdparam.Context currentRunningContext = null;
	
	/* 保存将要切换出去的context */
	avic.actri.targetserver.core.cmdparam.Context willBeSwarpOutContext = null;
	
	/* 保存将要切换进来的context */
	avic.actri.targetserver.core.cmdparam.Context willBeSwarpInContext = null;
	
	/* 保存引起切换的事件  */
	private BaseLogStructInfo cause2ContextSwitch = null;
	
	/* 切换出去的任务的ContextSwitch事件 */
	private BaseLogStructInfo willBeSwapOutContextSwitch = null;
	
	/* 切换进来的任务的ContextSwitch事件  */
	private BaseLogStructInfo willBeSwapInContextSwitch = null;
	
	private LogManager(){
		
	}
	
	/* 考虑到多个地方使用，故实例化为单例模式 */
	static LogManager instance = null;
	static public LogManager getInstance(){
		if(instance == null){
			instance = new LogManager();
		}
		return instance;
	}
	
	public LogManager(List<BaseLogStructInfo> infos,
			Map<String, Integer> mapTaskName2Id){
		logs = infos;
		mapTaskName2TaskId = mapTaskName2Id;
		iterator4ReplayLogs = logs.listIterator();
	}
	
	public void setLogs(List<BaseLogStructInfo> infos){
		this.logs = infos;
		iterator4ReplayLogs = logs.listIterator();
	}
	
	public void setTaskName2IdMaps(Map<String, Integer> maps){
		mapTaskName2TaskId = maps;
	}
	
	public List<BaseLogStructInfo> getLogs(){
		return this.logs;
	}
	
	public Map<String, Integer> getTaskName2IdMaps(){
		return mapTaskName2TaskId;
	}
	
	private long createContext(TaskInformation4Repay information)
			throws CoreException{
		return targetReplay.createContext(information.getTaskName(), 
				information.getStackBase(), 
				information.getStackSize(), 
				information.getEntryPoint(), 
				information.getArgs(),
				information.getPriority(), 
				information.getOptions());
	}
	
	/* 根据记录阶段的任务Id来创建上下文 */
	private void createContext(Integer taskId) throws CoreException {
		TaskInformation4Repay taskInformation4Repay = null;
		try{
			taskInformation4Repay = taskInformationManager.mapOldTaskId2TaskInformation4Replay(taskId);
		}catch(CrossException e){
			throw new CrossException(e.getMessage());
		}
		
		long contextId = createContext(taskInformation4Repay);
		/* 根据ContextId来创建Context */
		avic.actri.targetserver.core.cmdparam.Context context =
				new avic.actri.targetserver.core.cmdparam.Context(
						targetReplay.getTarget(), 
						CtxType.CTX_TASK,
						contextId, 0);
		/* 修补任务的Context 和ContextId信息 */
		taskInformationManager.setTaskInformationByOldId(taskId, context);
		taskInformationManager.setTaskCurrentStatusByContext(context, 
				TaskInformationManager.TaskCurrentStatus.TASKSTATUS_READY);
		/* 一旦任务修补了这个信息之后,信息基本已经完整 */
	}
	
	/* 判断是否到达日志中的某个点 */
	public boolean isArraivedPoint (
			avic.actri.targetserver.core.cmdparam.Context context,
			TargetAddr pc) throws CrossException{
		/* 查阅表，将该context映射为记录阶段执行的context Id */
		/* 因为目前仅有TaskDelay和Interrupt切换，故直接返回True */
		//return true;
		
		/* 进行断点命中次数判断 */
		TaskInformation4Repay taskInfo = taskInformationManager.mapContextId2TaskInformation4Repay(context);
		taskInfo.setBreakPointHasHitTimes(taskInfo.getBreakPointHasHitTimes() + 1);
		
		Integer hasHit = taskInfo.getBreakPointHasHitTimes();
		Integer mustHit = taskInfo.getBreakPointHitTimes();
		
		if(hasHit.equals(mustHit)){
			return true;
		}
		return false;
		/*if(name == null){
			throw new CrossException("目标机中不存在该上下文，上下文Id(" + context.getMainid() + ")");
		}
		
		if(name.equals("这里填入日志中的名字")){
			 如果名字相同，说明刚好满足要求 
			return true;
		}
		return false;*/
	}
	
	/* 获取第一个断点地址, 这个接口只会在第一次设置断点时候调用,
	 * 目的是为了能够启动任务且设置第一个断点 */
	public avic.actri.targetserver.core.cmdparam.Context 
		getFirstToBeRunning() throws CrossException, CoreException{
		ListIterator<BaseLogStructInfo> iterator = logs.listIterator();
		while(iterator.hasNext()){
			BaseLogStructInfo info = iterator.next();
			if(info instanceof CpuIndexLogStruct){
				/* 找到第一个CPUIndex事件 */
				CpuIndexLogStruct cpuIndexLogStruct = (CpuIndexLogStruct)info;
				if(! cpuIndexLogStruct.getIndex().equals(currentCpuIndex)){
					cpuIndexChanged = true;
				}else{
					cpuIndexChanged = false;
				}
				currentCpuIndex = cpuIndexLogStruct.getIndex();
				
			}else if(info instanceof TaskStartLogStruct){
				/* 找到任务的就绪信息 */
				TaskStartLogStruct taskStartLogStruct = (TaskStartLogStruct)info;
				/* 设置任务的启动信息(entryPoint和args) */
				long[] args = new long[10];
				args[0] = taskStartLogStruct.getArgs();
				taskInformationManager.setTaskInformationByOldId(
						taskStartLogStruct.getTaskIdentity(), 
						(long)taskStartLogStruct.getEntryPoint().intValue(), args);
				/* 设置任务的状态为就绪态 */
				taskInformationManager.setTaskCurrentStatusByOldId(
						taskStartLogStruct.getTaskIdentity(), 
						TaskInformationManager.TaskCurrentStatus.TASKSTATUS_READY);
				/*****************************************************************/
				/*XXX 需要调用TargetReplay来创建对应的就绪任务,让其在Target端也处于就绪状态 */
				createContext(taskStartLogStruct.getTaskIdentity());
				
			}else if(info instanceof ContextSwitchLogStruct){
				/* 找到第一个任务切换信息,那么就可以开始进行任务的运行了 */
				ContextSwitchLogStruct contextSwitchLogStruct = 
						(ContextSwitchLogStruct)info;
				/* 应该返回任务的具体信息,供上层查看{
				 * 		任务context,
				 * 		断点位置
				 * } */
				iterator4ReplayLogs = iterator;
				TargetInformation.printTaskInfo(targetReplay.getTarget());
				/* 那个任务将会运行起来 */
				currentRunningContext =
						taskInformationManager.mapOldTaskId2TaskInformation4Replay(
								contextSwitchLogStruct.getTaskIdentity())
								.getContext();
				/* 这里只需要找到第一个就可以了 */
				/* 找到后续这个任务因为什么切换出去了,还有就是下面还是否有新的任务可以运行了 */
				/* 设置这个Context对应的任务的状态为running */
				taskInformationManager.setTaskCurrentStatusByContext(
						currentRunningContext, 
						TaskInformationManager.TaskCurrentStatus.TASKSTATUS_RUNNING);
				return currentRunningContext;
			}
		}
		return null;
	}
	
	
	/* 采用类似迭代器的形式向后移动,这个函数返回后续是否还有任务切换事件 */
	public boolean hasNext(){
		boolean ret = false;
		Integer iteratorCounts = 0;
		Integer contextSwitchPair = 0;
		while(iterator4ReplayLogs.hasNext()){
			BaseLogStructInfo info = iterator4ReplayLogs.next();
			iteratorCounts ++;
			if(info instanceof ContextSwitchLogStruct){
				/* 找到一个任务切换信息,并判断这个ContextSwitch是我们关注任务的切换信息 */
				contextSwitchPair ++;
				if(contextSwitchPair % 2 == 1){
					/* 表明是切换的一对ContextSwitch的第一个 */
					contextSwitchPair = 1;
					try{
						taskInformationManager.mapOldTaskId2TaskInformation4Replay(((ContextSwitchLogStruct)info).getTaskIdentity());
					}catch(CrossException exception){
						/*异常说明没有该任务*/
						/*if(isFirstGetContext){
							ret = true;
							break;
						}*/
						continue;
					}
					/* 是一个有效的 */
					ret = true;
					break;
				}else{
					continue;
				}
			}
		}
		
		/* 恢复迭代器原始位置 */
		while(iteratorCounts -- > 0 && iterator4ReplayLogs.hasPrevious()){
			iterator4ReplayLogs.previous();
		}
		return ret;
	}
	
	/* 移动到下一个任务切换位置，并且对相应的变量赋值 */
	/* 先考虑的是单核情况 */
	public void next() throws CrossException, CoreException{
		Integer cpuIndex = -1;
		Integer contextSwitchPair = 0;
		while(iterator4ReplayLogs.hasNext()){
			BaseLogStructInfo info = iterator4ReplayLogs.next();
			if(info instanceof CpuIndexLogStruct){
				/* 如果是CPUIndex，那么跟踪这个CPU */
				cpuIndex = ((CpuIndexLogStruct)info).getIndex();
			}else if(info instanceof ContextSwitchLogStruct){
				contextSwitchPair ++;
				if(contextSwitchPair % 2 == 1){
					contextSwitchPair = 1;
					try{
						taskInformationManager.mapOldTaskId2TaskInformation4Replay(((ContextSwitchLogStruct)info).getTaskIdentity());
					}catch(CrossException exception){
						/*异常说明没有该任务*/
						/*if(isFirstGetContext){
							
							isFirstGetContext = true;
							break;
						}*/
						continue;
					}
					/* 是一个有效的 */
				}else{
					continue;
				}
				
				/* 第一次找到一个任务切换，及时是多核上，也是串行进行切换 */
				ContextSwitchLogStruct contextSwitchLogStruct =
						(ContextSwitchLogStruct)info;
				willBeSwarpOutContext = invalidContext;
				try{
					willBeSwarpOutContext = taskInformationManager.mapOldTaskId2TaskInformation4Replay(
						contextSwitchLogStruct.getTaskIdentity()).getContext();
				}catch(CrossException exception){
					/* 说明没有找到对应Id的任务信息，也说明这个任务不是我们关注的，但是参入了切换 */					
				}
				/* 设置将要切换出去的ContextSwitch */
				willBeSwapOutContextSwitch = info;
				
				/* 尝试找切换进来的任务的信息 */
				Integer currentCpu = cpuIndex;
				willBeSwarpInContext = invalidContext;
				while(iterator4ReplayLogs.hasNext()){
					BaseLogStructInfo anotherInfo = iterator4ReplayLogs.next();
					if(anotherInfo instanceof CpuIndexLogStruct){
						currentCpu = ((CpuIndexLogStruct)anotherInfo).getIndex();
					}else if(currentCpu.equals(cpuIndex)){
						/* 找到相同CPU上的事件，那么这个紧跟的事件一定就是另外一个ContextSwitch */
						if(! (anotherInfo instanceof ContextSwitchLogStruct)){
							throw new CrossException("找到一个context事件不匹配，事件timestamp是(" + 
									anotherInfo.getType().getTimestamp() + ")");
						}
						try{
							willBeSwarpInContext = taskInformationManager.mapOldTaskId2TaskInformation4Replay(
									((ContextSwitchLogStruct)anotherInfo).
									getTaskIdentity()).
									getContext();
						}catch(CrossException exception){
							/* 表明没有找到对应Id的任务信息 */
						}
						/* 设置将要切换进来的ContextSwitch事件 */
						willBeSwapInContextSwitch = anotherInfo;
						
						if(willBeSwarpInContext.getMainid() == invalidContext.getMainid() &&
								willBeSwarpOutContext.getMainid() == invalidContext.getMainid()){
							/* 如果两个切换的都是无效的context，那么说明有问题 */
							throw new CrossException("将要运行的和切出去的都是无效的context");
						}
						/* 接下来寻找引起此次切换的原因 */
						cause2ContextSwitch = findCause(iterator4ReplayLogs, cpuIndex);
						break;
						/* 运行到这里后，iterator4ReplayLogs指向任务ContextSwitch信息的下一个 */
					}
				}
				/* 跳出第二层循环 */
				break;
			}else if(info instanceof TaskStartLogStruct){
				/* 找到任务的就绪信息 */
				TaskStartLogStruct taskStartLogStruct = (TaskStartLogStruct)info;
				/* 设置任务的启动信息(entryPoint和args) */
				long[] args = new long[10];
				Arrays.fill(args, 0);
				args[0] = taskStartLogStruct.getArgs();
				
				System.err.println("arg0 address is:" + args[0]);
				
				taskInformationManager.setTaskInformationByOldId(
						taskStartLogStruct.getTaskIdentity(), 
						(long)taskStartLogStruct.getEntryPoint().intValue(), args);
				/* 设置任务的状态为就绪态 */
				taskInformationManager.setTaskCurrentStatusByOldId(
						taskStartLogStruct.getTaskIdentity(), 
						TaskInformationManager.TaskCurrentStatus.TASKSTATUS_READY);
				/*****************************************************************/
				/*XXX 需要调用TargetReplay来创建对应的就绪任务,让其在Target端也处于就绪状态 */
				try{
					createContext(taskStartLogStruct.getTaskIdentity());
				}catch(CoreException e){
					continue;
				}
			}
		}
	}
	
	
	/* 向上负责找到引起切换的事件,注意这个函数调用后，会保证传入的iterator位置不变 */
	private BaseLogStructInfo findCause(ListIterator<BaseLogStructInfo> iterator, Integer cpuIndex)
		throws CrossException{
		Integer iteratorCounts = 0;
		BaseLogStructInfo ret = null;
		Integer currentCpu = cpuIndex;
		Integer passedContextSwitchNums = 0;
		/* 这里需要注意跳过前两条的ContextSwitch，最后面相应的也要这要操作 */
		while(iterator.hasPrevious()){
			BaseLogStructInfo info = iterator.previous();
			iteratorCounts ++;
			if(info instanceof CpuIndexLogStruct){
				currentCpu = ((CpuIndexLogStruct)info).getIndex();
			}else if(currentCpu.equals(cpuIndex)){
				/* 如果是关注的CPU的事件 */
				if(info instanceof ContextSwitchLogStruct){
					passedContextSwitchNums ++;
					if(passedContextSwitchNums == 2){
						/* 如果已经跳过了两个ContextSwitch */
						break;
					}
				}
			}
		}
		
		while(iterator.hasPrevious()){
			BaseLogStructInfo info = iterator.previous();
			iteratorCounts ++;
			if(info instanceof CpuIndexLogStruct){
				currentCpu = ((CpuIndexLogStruct)info).getIndex();
			}else if(currentCpu.equals(cpuIndex)){
				/* 如果是关注的CPU的事件 */
				if(info instanceof InterruptExitRescheduleLogStruct){
					/* 如果是一个中断事件，那么找到 */
					ret = info;
					break;
				}else if(info instanceof SynchronousRescheduleLogStruct){
					/* 如果是一个同步事件，那么这个事件之前的事件就是原因  */
					/* 继续向前找事件 */
					while(iterator.hasPrevious()){
						/* 注意，之前的info对象已经没有作用，故这里重用  */
						info = iterator.previous();
						iteratorCounts ++;
						if(info instanceof CpuIndexLogStruct){
							currentCpu = ((CpuIndexLogStruct)info).getIndex();
						}else if(currentCpu.equals(cpuIndex)){
							if(! info.getLogCategory().equals(LogCategory.LOG_CATEGORY_SYNCHRONOUS)){
								throw new CrossException("引起切换的不是同步事件，但是分析应该是同步事件，事件timestamp是(" + 
										info.getType().getTimestamp() + ")");
							}
							ret = info;
							break;
						}
					}
					break;
				}else if(info instanceof ContextSwitchLogStruct){
					/* 如果之前还是一个ContextSwitch，那么认为是由于中断引发的切换 */
					ret = new InterruptExitRescheduleLogStruct();
					break;
				}
			}
		}
		
		/* 调整iterator */
		while(iterator.hasNext() && iteratorCounts -- > 0){
			iterator.next();
		}
		
		/* 这里需要注意跳过两条的ContextSwitch，参看前面相应的也要这要操作 */
		/* 不过已经包含在了iteratorCounts中 */
		return ret;
	}
	
	
	/* 获取下一个断点设置点 */
	public int getBreakPointAddress() throws CrossException{
		int ret = 0;
		/* 判断引发任务切换的事件的类型 */
		System.err.println("time stamp is: " + cause2ContextSwitch.getType().getTimestamp());
		if(cause2ContextSwitch.getLogCategory().equals(LogCategory.LOG_CATEGORY_SYNCHRONOUS)){
			if(cause2ContextSwitch instanceof TaskDelayLogStruct){
				/* 如果是任务延时，基本山就会引起切换 */
				ret = (int)targetReplay.getAddressBySymbol(ConstSymbol4Replay.SYMBOL_TASK_DELAY);
				/* 设置对应断点的个数 */
				setBreakPointHitTimes(1, 0);
				
			}else if(cause2ContextSwitch instanceof SemaphoreObtainLogStruct){
				ret = (int)targetReplay.getAddressBySymbol(ConstSymbol4Replay.SYMBOL_SEMAPHORE_OBTAIN);
				setBreakPointHitTimes(
						((SemaphoreObtainLogStruct)cause2ContextSwitch).getSynchronousCounter() + 1, 
						0);
			}else if(cause2ContextSwitch instanceof MessageQueueSendLogStruct){
				ret = (int)targetReplay.getAddressBySymbol(ConstSymbol4Replay.SYMBOL_MESSAGEQUEUE_SEND);
				setBreakPointHitTimes(
						((MessageQueueSendLogStruct)cause2ContextSwitch).getSynchronousCounter() + 1, 
						0);
			}else if(cause2ContextSwitch instanceof MessageQueueReceiveLogStruct){
				SynchronousBaseLogStruct logStruct = (SynchronousBaseLogStruct)cause2ContextSwitch;
				ret = logStruct.getProgramCounter();
				setBreakPointHitTimes(
						((MessageQueueReceiveLogStruct)cause2ContextSwitch).getSynchronousCounter() + 1, 
						0);
			}else{
				SynchronousBaseLogStruct logStruct = (SynchronousBaseLogStruct)cause2ContextSwitch;
				System.err.println("something is wrong: timestamp is :" + logStruct.getType().getTimestamp());
			}
		}else{
			/* 说明任务切换是由于中断引发，断点设置在任务切换处 */
			ret = ((ContextSwitchLogStruct)willBeSwapInContextSwitch).getProgramCounter();
			setBreakPointHitTimes(1, 0);
		}
		return ret;
	}	
	
	
	/* 获取下一个将要运行的任务Context */
	public avic.actri.targetserver.core.cmdparam.Context getSwapInTaskContext(){
		return willBeSwarpInContext;
	}
	
	/* 获取下一个将要切出去的任务Context */
	public avic.actri.targetserver.core.cmdparam.Context getSwapOutTaskContext(){
		return willBeSwarpOutContext;
	}
	
	/* 设置对应断点应该命中的次数  */
	public void setBreakPointHitTimes(avic.actri.targetserver.core.cmdparam.Context context,
			Integer mustHitTimes, Integer hasHitTimes) throws CrossException{
		TaskInformation4Repay taskInformation = 
				taskInformationManager.mapContextId2TaskInformation4Repay(context);
		taskInformation.setBreakPointHitTimes(mustHitTimes);
		taskInformation.setBreakPointHasHitTimes(hasHitTimes);
	}
	
	/* 设置当前任务对应的断点命中次数 */
	public void setBreakPointHitTimes(Integer mustHitTimes, Integer hasHitTimes)
		throws CrossException{
		setBreakPointHitTimes(getSwapOutTaskContext(), mustHitTimes, hasHitTimes);
	}
	/* 给出如果没有到达给定位置的处理过程 */
	/* 直觉上应该给出两个不同的处理过程，1. 开始的时候没有找到开始正确运行的任务
	 * 2. 执行过程中，没有到达准确的位置 */
}
