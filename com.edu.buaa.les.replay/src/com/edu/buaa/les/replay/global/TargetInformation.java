package com.edu.buaa.les.replay.global;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import avic.actri.runtime.core.exceptions.CrossException;
import avic.actri.targetserver.TsActivator;
import avic.actri.targetserver.api.TsApi;
import avic.actri.targetserver.core.ITarget;
import avic.actri.targetserver.core.ITargetSet;
import avic.actri.targetserver.core.TargetBp;

public class TargetInformation {
	
	/* 通过名字获取正在运行的target */
	public static ITarget getTargetByName(final String targetName)
		throws CrossException{
		TsApi.connectTargetServer();
		ITargetSet set = TsActivator.getDefault().getTargetServer()
				.getTsManager().getActiveTargetSet();
		//List<ITarget> targets = set.getTargets();
		return TsActivator.getDefault().getTargetServer()
				.getTsManager().getActiveTargetSet().getTarget(targetName);
	}
	
	/* 获取系统中的断点信息 */
	public static List<TargetBp> getBreakPointInfo(ITarget target)
		throws CrossException{
		return TsApi.getTargetBpList(target);
	}
	
	/*
	 * 从目标端获取任务相关信息
	 */
	public static List<TaskInformation> getTaskInformationsFromTarget(ITarget target) 
		throws CrossException{
		List<String> taskInfoStrings = null;
		List<TaskInformation> taskInformations = new LinkedList<TaskInformation>();
		
		taskInfoStrings = TsApi.getTasks(target);
		Iterator<String> iterator = taskInfoStrings.iterator();
		while(iterator.hasNext()){
			taskInformations.add(getTaskInformationFromString(iterator.next()));
		}
		return taskInformations;
	}
	
	/* 打印任务信息 */
	public static void printTaskInfo(ITarget target){
		List<TaskInformation> taskInfos = null;
		try {
			taskInfos = getTaskInformationsFromTarget(target);
		} catch (CrossException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		Iterator<TaskInformation> iterator = taskInfos.iterator();
		while(iterator.hasNext()){
			System.err.println(iterator.next());
		}
	}
	/*
	 * 从字符串中取任务相关信息
	 */
	public static TaskInformation getTaskInformationFromString(final String info)
		throws CrossException{
		String[] tokens = info.trim().split(" ");
		if(tokens.length != 3){
			/* 如果分割好之后不是3个的话,返回null */
			throw new CrossException("返回数据采用空格分割之后不是三个域");
		}
		TaskInformation infoStruct = new TaskInformation(); 
		infoStruct.setName(tokens[0]);
		infoStruct.setId(Long.parseLong(tokens[1], 16));
		infoStruct.setStatus(tokens[2]);
		return infoStruct;
	}
	
	public static List<ContextInformation> getContextInformationsFromTarget(ITarget target) 
		throws CrossException{
		List<String> taskInfoStrings = null;
		List<ContextInformation> taskInformations = new LinkedList<ContextInformation>();
		try {
			taskInfoStrings = TsApi.getTasks(target);
		} catch (CrossException e) {
			// TODO Auto-generated catch block
			throw new CrossException("从目标端获取任务相关信息错误");
		}
		Iterator<String> iterator = taskInfoStrings.iterator();
		while(iterator.hasNext()){
			taskInformations.add(getContextInformationFromString(iterator.next()));
		}
		return taskInformations;
	}
	
	public static ContextInformation getContextInformationFromString(final String info)
		throws CrossException{
		String[] tokens = info.trim().split(" ");
		if(tokens.length != 3){
			/* 如果分割好之后不是3个的话,返回null */
			throw new CrossException("返回的数据不能采用空格分割得到3个域");
		}
		ContextInformation infoStruct = new ContextInformation(); 
		infoStruct.setName(tokens[0]);
		/* 根据手册，传回来的是16进制数据 */
		infoStruct.setId(Long.parseLong(tokens[1], 16));
		infoStruct.setStatus(tokens[2]);
		return infoStruct;
	}
	
	/* 基本信息体 */
	private static abstract class BaseInformation{
		private String name;
		private Long id;
		private String status;
		
		public BaseInformation() {
			// TODO Auto-generated constructor stub
			name = "";
			id = (long) -1;
			status = "";
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}
		
	}
	
	/* 任务信息内部类 */
	public static class TaskInformation extends BaseInformation{
		public String toString(){
			return "任务名：(" + super.getName() + ")" + "-----" +
					"任务Id：(" + super.getId() + ")" + "-----" +
					"任务状态：(" + super.getStatus() + ")";
		}
	}
	
	/* 上下文信息内部类 */
	public static class ContextInformation extends TaskInformation{
		public String toString(){
			return "Context名：(" + super.getName() + ")" + "-----" +
					"Context Id：(" + super.getId() + ")" + "-----" +
					"Context状态：(" + super.getStatus() + ")";
		}
	}
}
