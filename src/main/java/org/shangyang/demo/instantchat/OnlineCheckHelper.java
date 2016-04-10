package org.shangyang.demo.instantchat;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.netty.channel.Channel;

/**
 * 
 * Simple mock Helper class to check if the user is online.
 * 
 * @author shangyang
 *
 */
public class OnlineCheckHelper {

	static Set<String> users = new LinkedHashSet<String>(5);
	
	static Map<String, Channel> connections = new LinkedHashMap<String, Channel>(5);
	
	/**
	 * 
	 * 这里加载在线用户必须使用覆盖更新的方法，因为如果浏览器刷新，重新建立连接，这个时候，服务器还来不及判断浏览器端的关闭事件。
	 * 
	 * @param username
	 * 
	 * @param the unique communication channel with user
	 */
	// FIXME: to use the real use instead
	public static void addOnlineUser(String username, Channel channel){
		
		users.add( username );
		
		// 如果已经注册，同样更新 channel 信息，用户刷新页面的时候，会重连。
		connections.put(username, channel);
	
	}

	// FIXME: to use the real use instead
	public static void delOnlineUser(String username){
		
		users.remove( username );
		
		connections.remove( username );
		
	}
	
	// FIXME: to use the real use instead
	public static boolean isUserOnline( String username ){
		
		return users.contains( username );
		
	}
	
	// find the user by channel
	public static String getOnlineUser( Channel channel ){
		
		for( Iterator<String> iter = connections.keySet().iterator(); iter.hasNext(); ){
			
			String currentUser = iter.next();
			
			if( connections.get( currentUser ).equals(channel) ){
				
				System.out.println("curent user get find," + currentUser);
				
				return currentUser;	
			}
		}
		
		return null;
		
	}

	// FIXME: to use the real use instead
	public static Channel getUserChannel( String username ){
		
		return connections.get( username );
		
	}
	
}
