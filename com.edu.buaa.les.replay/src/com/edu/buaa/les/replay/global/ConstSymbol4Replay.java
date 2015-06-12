package com.edu.buaa.les.replay.global;

public class ConstSymbol4Replay {
	private static final String SYMBOL_PREFIX = "";
	private static final String addPrefix(String str){
		return SYMBOL_PREFIX + str;
	}
	
	/* 数据符号常量定义 */
	public static final String SYMBOL_COMMON_REGION =
			addPrefix("LES_CommonDataRegion");
	public static final Integer SYMBOL_COMMON_REGION_SIZE=
			100;
	
	/* 函数常量定义 */
	/* 任务获取符号定义 */
	public static final String SYMBOL_GET_TASK_STACK =
			addPrefix("ACoreOs_task_get_stack");
	public static final int TASK_STACK__INFORMATION_STRUCT_SIZE = 12;
	
	/* 任务启动符号定义 */
	public static final String SYMBOL_TASK_START =
			addPrefix("ACoreOs_task_start");
	
	/* 任务延时符号定义 */
	public static final String SYMBOL_TASK_DELAY =
			addPrefix("ACoreOs_task_wake_after");
	
	/* 信号量获取符号定义 */
	public static final String SYMBOL_SEMAPHORE_OBTAIN = 
			addPrefix("ACoreOs_semaphore_obtain");
	
	/* 消息队列发送符号定义 */
	public static final String SYMBOL_MESSAGEQUEUE_SEND =
			addPrefix("ACoreOs_msgqueue_send");
	/* 任务调度符号定义 */
	public static final String SYMBOL_THREAD_DISPATCH =
			addPrefix("_Thread_Dispatch");
	
	
}
