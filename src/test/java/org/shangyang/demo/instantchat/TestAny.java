package org.shangyang.demo.instantchat;

import org.junit.Test;

/**
 * 
 * @author shangyang
 *
 * @version
 *
 * @createTime：2016年4月9日 上午11:20:44
 *
**/

public class TestAny {

	@Test
	public void regexTest1(){
		
		String message = "send message: hello client 1 send to: client1";
		
		System.out.println( message.replaceFirst("send message:", "").replaceFirst("send to:.*", ""));
		
		System.out.println( message.replaceFirst(".*send to:", "") );
		
	}
	
}
