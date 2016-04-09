当前模拟一个很简单的用例

client 1, client 2，client 3 可以任意通讯

1. 实现在线用户的两两即时通讯。
   
   逻辑，消息的发送过程 client 1 -> server -> client 2，这样 server 可以保存所有的聊天记录 
   
   1.1 注册用户
       当用户连接到 server 的时候，server 注册 client，包括 channel
       
   1.2 发送消息
       
       client 1 -> client 2
       
       1.2.1 client 1 将消息发送到 server，消息格式 [send message: xxx send to: xxx] (注：目前为了跑通核心流程，用了最简单的字符格式，开发项目需要改为 JSON)
       
       1.2.2 server 获取 client 2 的注册信息，取得当前的 channel
       
       1.2.3 server 通过 client 2 的 channel 将消息发送给 client 2
       
   演示过程需注意，让 client 1 和 client 2 同时和 server 保持连接，既在线状态，否则进入 #2 的场景    

2. 实现离线用户消息发送和推送。

3. 通过 Nginx 建立 NIO 集群 

