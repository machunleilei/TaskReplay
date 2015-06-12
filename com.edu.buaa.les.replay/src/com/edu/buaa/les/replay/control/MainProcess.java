package com.edu.buaa.les.replay.control;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import com.edu.buaa.les.replay.global.ConstSymbol4Replay;
import com.edu.buaa.les.replay.global.TargetInformation;
import com.edu.buaa.les.replay.global.TaskInformationManager;
import com.edu.buaa.les.replay.global.TaskInformationManager.TaskInformation4Repay;
import com.edu.buaa.les.replay.process.BreakpointProcess;

import avic.actri.runtime.core.exceptions.CrossException;
import avic.actri.targetserver.api.TsApi;
import avic.actri.targetserver.core.ITarget;
import avic.actri.targetserver.core.cmdparam.AgentMode;
import avic.actri.targetserver.core.cmdparam.CtxType;

import com.edu.buaa.les.log.core.BaseLogStructInfo;
import com.edu.buaa.les.log.main.*;
import com.edu.buaa.les.log.struct.TaskCreateLogStruct;

public class MainProcess {
	
	public static void ControlReplay()
		throws CrossException{
		TargetReplay replayControl = null;
		ITarget target = null;
		try {
			target = TargetInformation.getTargetByName("Target001");
		} catch (CrossException e) {
			// TODO: handle exception
			e.printStackTrace();
			throw new CrossException("连接目标服务器失败");
		}
		try{
			/* 创建TargetReplay对象并初始化target */
			replayControl = TargetReplay.getInstance(target);
		}catch(CrossException e){
			e.printStackTrace();
			throw new CrossException("创建TargetReplay对象失败");
		}
		
		/* 载入过滤后的日志 */
		String logFilePath = "D:\\eclipse4.2.2\\workspace\\com.edu.buaa.les.log\\logfile\\target.svr";
		/* 构造日志分析和过滤主任务 */
		Main logMain = new Main(logFilePath);
		/* 设置过滤任务名,这里采用默认的任务名,是UserTask01 ~ UserTask12 */
		logMain.defaultSetTaskName();
		/* 自己可手动添加任务名 */
//		logMain.addTaskName("UserTaskxx");
		logMain.start();
		/* 获取过滤之后的日志 */
		List<BaseLogStructInfo> filterLogRet = logMain.getFilteredLogs();
		/* 获取任务Id和任务创建日志对应关系 */
		Map<Integer, BaseLogStructInfo> mapId2TaskCreateLog = 
				logMain.getMapId2TaskCreateInfo();
		
		/* 获取任务名字和Id对应关系 */
		Map<String, Integer> mapName2Id = logMain.getMapName2Id();

		/******************************************************************/
		/* 构造任务信息管理对象,并开始对其进行初始化工作 */
		TaskInformationManager taskInformationManager =
				TaskInformationManager.getInstance();
		
		/* 初始化任务管理对象中对基本的任务信息 */
		Iterator<Integer> iterator4TaskId = mapId2TaskCreateLog.keySet().iterator();
		while(iterator4TaskId.hasNext()){
			Integer which = iterator4TaskId.next();
			TaskCreateLogStruct taskLogStruct = 
					(TaskCreateLogStruct)mapId2TaskCreateLog.get(which);
			taskInformationManager.setTaskInformationByOldId(which, taskLogStruct.getTaskName(), 
					0, taskLogStruct.getStackSize(), 
					taskLogStruct.getPriority(), taskLogStruct.getOption());
			/* 设置任务的状态为创建状态,表明任务已经创建 */
			taskInformationManager.setTaskCurrentStatusByOldId(which, 
					TaskInformationManager.TaskCurrentStatus.TASKSTATUS_CREATE);
		}
		
		/* 关于任务入口地址以及入口参数的初始化工作交给LogManager模块进行管理和初始化 */
		
		/* 下面开始进行LogManager的第一次尝试找到运行的任务并尝试设置断点 */
		/* 为了跟TaskInformationManager对应起来,这里LogManager也采用单身模式 */
		LogManager logManager = LogManager.getInstance();
		/* 需要将要管理的日志告诉给LogManger对象,并且需要讲管理的任务的Id和任务名字也告诉给它*/
		logManager.setLogs(filterLogRet);
		logManager.setTaskName2IdMaps(mapName2Id);
		
		try {
			if(logManager.hasNext()){
				logManager.next();
			}
		}catch(CrossException exception){
			System.err.println("LogManager.next fisrt call failed with CrossException");
			exception.printStackTrace();
			System.exit(0);
		}
		catch (CoreException e2) {
			// TODO Auto-generated catch block
			System.err.println("LogManager.next fisrt call failed with CoreException");
			e2.printStackTrace();
			System.exit(0);
		}
		
		/* 先运行Init任务，为其他任务的执行做好铺垫 */
		startInitTask();
		
		try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		/*  */
//		TsApi.targetModeSet(target, AgentMode.);
		/* 获取第一个任务的断点 */
		int address = logManager.getBreakPointAddress();
		avic.actri.targetserver.core.cmdparam.Context context = logManager.getSwapOutTaskContext();
		TaskInformation4Repay infos = taskInformationManager.mapContextId2TaskInformation4Repay(context);
			
		

		/* 目标机在运行过程中，不能够提供动态的信息，要获取相关信息，最好通过断点的回调来获取信息 */
		
		/* 在任务开始处，设定断点，这样有机会调度任务开始运行的顺序和记录阶段一样 */
		
		System.err.println("the address is: " + address);
		/* 设置断点, 并绑定断点处理过程 */
		BreakpointProcess process = new BreakpointProcess();
		try {
			//replayControl.addBreakPointWithoutContext(address, process);
			replayControl.addBreakPoint(logManager.getSwapInTaskContext(), address, process);
		} catch (CrossException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new CrossException("CrossException为任务设置断点<" + address +  ">失败");
		}
		/* 启动重放任务 */
		replayControl.resumeContext(context);
		
	}
	
	
	
	/* 启动Init任务 */
	private static void startInitTask() throws CrossException{
		TargetReplay replayControl = TargetReplay.getInstance();
		/* 获取任务列表信息 */
		List<TargetInformation.TaskInformation> taskInfos = null;
		try {
			taskInfos = TargetInformation.getTaskInformationsFromTarget(replayControl.getTarget());
		} catch (CrossException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new CrossException("从目标端获取任务相关信息错误");
		}
		
		/* 获取目标机context信息 */
		List<TargetInformation.ContextInformation> contextInfos = null;
		try {
			contextInfos = TargetInformation.getContextInformationsFromTarget(replayControl.getTarget());
		} catch (CrossException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new CrossException("从目标端获取Context相关信息错误");
		}
		
		/* 找到名字是Init的任务的信息 */
		avic.actri.targetserver.core.cmdparam.Context context = null;
		for (TargetInformation.TaskInformation contextInformation : contextInfos) {
			String taskName = contextInformation.getName();
			if (contextInformation.getName().equals(new String("Init"))) {
				/* 创建任务上下文 */
				context = replayControl.createContext(CtxType.CTX_TASK, 
						contextInformation.getId());
				break;
			}
		}
		
		/* 继续Init任务的执行 */
		try {
			replayControl.resumeContext(context);
		} catch (CrossException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new CrossException("继续Init任务失败");
		}
	}
}
