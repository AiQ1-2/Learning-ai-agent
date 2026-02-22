package com.zpark.learningagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.val;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 作用：基于文件存储的会话内存
 */
public class FileBasedChatMemory implements ChatMemory {
    private final String BASE_PATH;
    private static final Kryo kryo = new Kryo();
    static {
        // 允许未注册的类
        kryo.setRegistrationRequired(false);
        //设置实例化策略
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }
    //构造对象时，指定文件保存路径
    public FileBasedChatMemory(String dir){
        this.BASE_PATH = dir;
        File baseDir = new File(dir);
        if(!baseDir.exists()){
            baseDir.mkdirs();
        }
    }
    @Override
    public void add(String conversationId, Message message) {
        saveConversation(conversationId, List.of());
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message>  messagesList  = getOrCreateConversation(conversationId);
        messagesList.addAll(messages);
        saveConversation(conversationId, messagesList);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> messages = getOrCreateConversation(conversationId);
        return messages.stream()
                .skip(Math.max(0, messages.size() - lastN))
                .toList();
    }

    @Override
    public void clear(String conversationId) {

    }
    /**
     * 获取会话
     */
    private List< Message> getOrCreateConversation(String conversationId){
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if(file.exists()){
            try(Input input = new Input(new FileInputStream(file))){
                messages = kryo.readObject(input, ArrayList.class);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return messages;
    }
    /**
     * 保存对话信息
     */
    private void saveConversation(String conversationId, List<Message> messages){
        File file = getConversationFile(conversationId);
        try(Output output = new Output(new FileOutputStream(file))){
            kryo.writeObject(output, messages);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * 每个不同的会话文件单独保存
     */
    private File getConversationFile(String conversationId){
        return new File(BASE_PATH, conversationId + ".kryo");
    }
}
