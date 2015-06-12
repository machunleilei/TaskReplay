package com.edu.buaa.les.replay.global;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLEngineResult.Status;
import javax.sound.midi.MidiDevice.Info;

import com.edu.buaa.les.log.core.BaseLogStructInfo;

import avic.actri.runtime.core.exceptions.CrossException;

public class TaskInformationManager {
	/* 映射任务名字到任务信息体 */
	private Map<String, TaskInformation4Repay> mapName2TaskInfo = null;
	/*  */
	private Map<Integer, TaskInformation4Repay> mapOldTaskId2TaskInfo = null;
	
	/* 鉴于Context无法正确实现key的作用，故使用Context.getMainid */
	private Map<Long, TaskInformation4Repay> mapContext2TaskInfo = null;
	
	private static TaskInformationManager instance = null;
	private TaskInformationManager(){
		mapName2TaskInfo = new HashMap<String, TaskInformation4Repay>();
		mapOldTaskId2TaskInfo = new HashMap<Integer, TaskInformation4Repay>();
		mapContext2TaskInfo = 
				new HashMap<Long, TaskInformation4Repay>();
	}
	/* 单身汉模式 */
	public static TaskInformationManager getInstance(){
		if(instance == null)
			instance = new TaskInformationManager();
		return instance;
	}
	
	/* 值得注意的是:
	 * 设置任务相关信息的顺序如下:
	 * 最先根据任务在记录阶段的Id设置任务的创建信息
	 * 	1. 任务Create
	 *  2. 任务Start
	 * 接着根据任务在创建阶段的Context设置任务的相关信息 */
	
	/* 设置相关信息 */
	public void setTaskInformationByOldId(Integer taskId,
			String taskName, Integer stackBase, Integer stackSize, 
			Integer priority, Integer options){
		TaskInformation4Repay info = null;
		/* 判断该任务信息在Map中 */
		if(mapOldTaskId2TaskInfo.containsKey(taskId)){
			/* 这里没有考虑需要将info从mapName2TaskInfo中删除, 因为在实际中不会出现 */
			info = mapOldTaskId2TaskInfo.get(taskId);	
		}else{
			info = new TaskInformation4Repay();	
			info.setOldTaskId(taskId);
		}
		info.setTaskName(taskName);
		info.setStackBase(stackBase);
		info.setStackSize(stackSize);
		info.setOptions(options);
		info.setPriority(priority);
		mapOldTaskId2TaskInfo.put(taskId, info);
		mapName2TaskInfo.put(taskName, info);
	}
	
	public void setTaskInformationByOldId(Integer taskId, Long entryPoint,
			long[] args) throws CrossException{
		TaskInformation4Repay info = null;
		if(mapOldTaskId2TaskInfo.containsKey(taskId)){
			/* 这里没有考虑需要将info从mapName2TaskInfo中删除, 因为在实际中不会出现 */
			info = mapOldTaskId2TaskInfo.get(taskId);	
		}else{
			throw new CrossException("没有找到记录阶段任务id是(" + taskId
					+ ")的任务的信息");
		}
		info.setEntryPoint(entryPoint);
		info.setArgs(args);
	}
	
	/* 根据任务名设置重放阶段任务的Id */
	/*public void setTaskInformationByTaskName(String taskName, Integer taskId){
		TaskInformation4Repay info = null;
		Integer oldTaskId = null;
		if(mapName2TaskInfo.containsKey(taskName)){
			 这里没有考虑需要将info从mapName2TaskInfo中删除, 因为在实际中不会出现 
			info = mapName2TaskInfo.get(taskName);	
			oldTaskId = info.getOldTaskId();
		}else{
			info = new TaskInformation4Repay();	
			info.setTaskName(taskName);
		}
		 修改info对应的重放阶段的id 
		info.setNewTaskId(taskId);
		mapName2TaskInfo.put(taskName, info);
		if(oldTaskId != null)
			mapOldTaskId2TaskInfo.put(oldTaskId, info);
		mapNewTaskId2TaskInfo.put(taskId, info);
	}*/
	
	public void setTaskInformationByOldId(
			Integer taskId, 
			avic.actri.targetserver.core.cmdparam.Context context)
		throws CrossException{
		TaskInformation4Repay taskInformation4Repay =
				mapOldTaskId2TaskInfo.get(taskId);
		if(taskInformation4Repay == null){
			throw new CrossException("没有找到记录阶段任务id是(" + taskId
					+")的任务信息");
		}
		taskInformation4Repay.setContext(context);
		mapContext2TaskInfo.put(context.getMainid(), taskInformation4Repay);
	}
	
	/* 根据任务名设置重放阶段对应的context*/
	public void setTaskInformationByTaskName(String taskName,
			avic.actri.targetserver.core.cmdparam.Context context)
		throws CrossException{
		TaskInformation4Repay info = null;
		if(mapName2TaskInfo.containsKey(taskName)){
			info = mapName2TaskInfo.get(taskName);
		}else{
			throw new CrossException("没有找到记录阶段任务名是(" + taskName
					+ ")的任务的信息");
		}
		info.setContext(context);
		mapContext2TaskInfo.put(context.getMainid(), info);
	}
	
	/* 设置任务的状态信息, 根据任务记录阶段的Id */
	public void setTaskCurrentStatusByOldId(Integer taskId,
			TaskCurrentStatus status) throws CrossException{
		/* 找到这个任务Id对应的任务块信息 */
		TaskInformation4Repay taskInformation4Repay = 
				mapOldTaskId2TaskInfo.get(taskId);
		if(taskInformation4Repay == null){
			throw new CrossException("没有找到记录阶段任务id是(" + taskId
					+ ")的任务的信息");
		}
		/* 设置状态信息 */
		taskInformation4Repay.setTaskStatus(status);
	}
	
	/* 设置任务的状态信息,根据任务重放阶段的Context */
	public void setTaskCurrentStatusByContext(
			avic.actri.targetserver.core.cmdparam.Context context,
			TaskCurrentStatus status)
		throws CrossException{
		TaskInformation4Repay taskInformation4Repay =
				mapContext2TaskInfo.get(context.getMainid());
		if(taskInformation4Repay == null){
			throw new CrossException("没有找到重放阶段任务Context Id是(" + context.getMainid() 
					+ ")的任务的信息");
		}
		taskInformation4Repay.setTaskStatus(status);
	}
	
	/* 供查阅的接口 */
	public TaskInformation4Repay mapOldTaskId2TaskInformation4Replay(
			Integer taskId)
		throws CrossException{
		if(! mapOldTaskId2TaskInfo.containsKey(taskId)){
			throw new CrossException("没有查到记录阶段任务Id是(" +
					taskId + ")的相关信息");
		}
		return mapOldTaskId2TaskInfo.get(taskId);
	}
	
	public TaskInformation4Repay mapContextId2TaskInformation4Repay(
			avic.actri.targetserver.core.cmdparam.Context context)
		throws CrossException{
		if(! mapContext2TaskInfo.containsKey(context.getMainid())){
			throw new CrossException("没有查到重放阶段Context Id是(" +
					context.getMainid() + ")的相关信息");
		}
		return mapContext2TaskInfo.get(context.getMainid());
	}
	
	public TaskInformation4Repay mapTaskName2TaskInformation4Repay(
			String taskName)
		throws CrossException{
		if(! mapName2TaskInfo.containsKey(taskName)){
			throw new CrossException("没有查到任务名是(" +
					taskName + ")的相关信息");
		}
		return mapName2TaskInfo.get(taskName);
	}
	
	/* 该类管理任务的基本信息 */
	public class TaskInformationBase{
		private String taskName = null; /* 任务名 */
		private Long entryPoint = null; /* 任务入口地址 */
		private Integer stackSize = null; /* 任务堆栈大小 */
		private Integer stackBase = 0; /* 任务的堆栈起始地址 */
		private long[] args = null; /* 任务的参数 */
		private Integer priority = null; /* 任务的优先级 */
		private Integer options = null; /* 任务的可选设置 */
		
		public TaskInformationBase() {
			// TODO Auto-generated constructor stub
		}

		public TaskInformationBase(String taskName, Long entryPoint,
				Integer stackSize, Integer stackBase, long[] args,
				Integer priority, Integer options) {
			super();
			this.taskName = taskName;
			this.entryPoint = entryPoint;
			this.stackSize = stackSize;
			this.stackBase = stackBase;
			this.args = args;
			this.priority = priority;
			this.options = options;
		}

		@Override
		public String toString() {
			return "TaskInformationBase [taskName=" + taskName
					+ ", entryPoint=" + entryPoint + ", stackSize="
					+ stackSize + ", stackBase=" + stackBase + ", args="
					+ Arrays.toString(args) + ", priority=" + priority
					+ ", options=" + options + "]";
		}

		public String getTaskName() {
			return taskName;
		}

		public void setTaskName(String taskName) {
			this.taskName = taskName;
		}

		public Long getEntryPoint() {
			return entryPoint;
		}

		public void setEntryPoint(Long entryPoint) {
			this.entryPoint = entryPoint;
		}

		public Integer getStackSize() {
			return stackSize;
		}

		public void setStackSize(Integer stackSize) {
			this.stackSize = stackSize;
		}

		public Integer getStackBase() {
			return stackBase;
		}

		public void setStackBase(Integer stackBase) {
			this.stackBase = stackBase;
		}

		public long[] getArgs() {
			return args;
		}

		public void setArgs(long[] args) {
			this.args = args;
		}

		public Integer getPriority() {
			return priority;
		}

		public void setPriority(Integer priority) {
			this.priority = priority;
		}

		public Integer getOptions() {
			return options;
		}

		public void setOptions(Integer options) {
			this.options = options;
		}
		
	}
	
	public enum TaskCurrentStatus{
		TASKSTATUS_CREATE(0),
		TASKSTATUS_READY(1),
		TASKSTATUS_RUNNING(2),
		TASKSTATUS_SUSPEND(3),
		TASKSTATUS_DESTROY(4),
		TASKSTATUS_UNCREATE(5),
		TASKSTATUS_UNKNOWN(6);
		
		int taskStatus;
		private TaskCurrentStatus(int val){
			taskStatus = val;
		}
	}
	public class TaskInformation4Repay extends TaskInformationBase{
		/* 记录阶段任务的ID */
		private Integer oldTaskId = null;
		/* 重放阶段任务的ID */
		private Integer newTaskId = null;
		/* 重放阶段任务的Context, 因为调试接口都是以Context作为操作对象的 */
		private avic.actri.targetserver.core.cmdparam.Context context = null;
		
		/* 标示该任务当前状态 */
		TaskCurrentStatus taskStatus = TaskCurrentStatus.TASKSTATUS_UNCREATE;
		
		
		/* 任务重放时候因为那种事件会引发切换 */
		BaseLogStructInfo switchTriggerLog = null;
		
		/* 需要命中该断点的次数 */
		Integer breakPointHitTimes = 0;
		
		/* 已经命中该断点的次数 */
		Integer breakPointHasHitTimes = 0;
		
		public TaskInformation4Repay() {
			// TODO Auto-generated constructor stub
		}
		
		
		public Integer getOldTaskId() {
			return oldTaskId;
		}

		public void setOldTaskId(Integer oldTaskId) {
			this.oldTaskId = oldTaskId;
		}

		public Integer getNewTaskId() {
			return newTaskId;
		}

		public void setNewTaskId(Integer newTaskId) {
			this.newTaskId = newTaskId;
		}


		public avic.actri.targetserver.core.cmdparam.Context getContext() {
			return context;
		}

		public void setContext(avic.actri.targetserver.core.cmdparam.Context context) {
			this.context = context;
		}
		
		public TaskCurrentStatus getTaskStatus() {
			return taskStatus;
		}

		public void setTaskStatus(TaskCurrentStatus taskStatus) {
			this.taskStatus = taskStatus;
		}
		
		public BaseLogStructInfo getSwitchTriggerLog() {
			return switchTriggerLog;
		}

		public void setSwitchTriggerLog(BaseLogStructInfo switchTriggerLog) {
			this.switchTriggerLog = switchTriggerLog;
		}

		
		public Integer getBreakPointHitTimes() {
			return breakPointHitTimes;
		}


		public void setBreakPointHitTimes(Integer breakPointHitTimes) {
			this.breakPointHitTimes = breakPointHitTimes;
		}

		
		public Integer getBreakPointHasHitTimes() {
			return breakPointHasHitTimes;
		}


		public void setBreakPointHasHitTimes(Integer breakPointHasHitTimes) {
			this.breakPointHasHitTimes = breakPointHasHitTimes;
		}


		@Override
		public String toString() {
			return "TaskInformation4Repay [oldTaskId=" + oldTaskId
					+ ", newTaskId=" + newTaskId 
					+ ", context=" + context + ", toString()="
					+ super.toString() + "]";
		}	
	}
}
