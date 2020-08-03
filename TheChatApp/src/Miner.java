
import java.io.IOException;
import java.net.DatagramPacket;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.*;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.PublicKey;

public class Miner extends MyUser {
    
    public Miner(String username, String myIP, String otherIP) throws NoSuchAlgorithmException, SocketException {
        super(username, myIP, otherIP);
    }
    @Override
    void receive(){
        try {
            byte[] receivedData=new byte[2048];
            receivedPacket=new DatagramPacket(receivedData,receivedData.length);
            mySocket.receive(receivedPacket);
            receivedData=receivedPacket.getData();
            String str=new String(receivedData);
            String[] arr=str.split(",",2);
            if(arr[0].equals("Message")){
                Message m=(Message)SerializeObject.deserializeObject(arr[1]);
                String message=decrypt(this.privateKey,m.cipher);
                System.out.println("Message received from "+otherUser.userName+":"+message);    
                allText=allText+otherUser.userName+": "+message+"\n";
                chatBox.setText(allText);
                Block b;
                String lastHash="";
                if(blockChain.size()==0){
                    lastHash="0";
                }
                else{
                    lastHash=blockChain.get(blockChain.size()-1).currHash;
                }
                b=Block.createBlock(lastHash,m);
                b.mineBlock();
                if(b.verifyTransaction(this)){
                    broadcastBlock(b);
                }
            }
            else{
                Block k=(Block)SerializeObject.deserializeObject(arr[1]);
                if(blockChain.size()!=0){
                    if(k.previousHash.equals(blockChain.get(blockChain.size()-1).currHash))
                        blockChain.add(k);
                }
                else{
                    blockChain.add(k);
                }
            }            
            //String allText=chatBox.getText();  
        } catch (IOException ex) {
            Logger.getLogger(MyUser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MyUser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Miner.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    @Override
    void send(String msg) throws Exception{
        byte[] toBeSent;
        Message m=new Message(this.userName,otherUser.userName);
        String str;
        allText=allText+userName+": "+msg+"\n";
        chatBox.setText(allText);
        m.cipher=encrypt(otherUser.publicKey,msg);
        m.vCipher=encryptPrivate(privateKey,"VERIFY");
        str="Message,"+SerializeObject.serializeObject(m);
        toBeSent=str.getBytes();
        InetAddress ia=InetAddress.getByName(otherUser.IP);
        sentPacket=new DatagramPacket(toBeSent,toBeSent.length,ia,port);
        mySocket.send(sentPacket);
        /*miner creates a block, this part is added
        to the receive and send functions of MyUser*/
        Block b;
        String lastHash="";
        if(blockChain.size()==0){
            lastHash="0";
        }
        else{
            lastHash=blockChain.get(blockChain.size()-1).currHash;
        }
        b=Block.createBlock(lastHash,m);
        b.mineBlock();
        if(b.verifyTransaction(this));
            broadcastBlock(b);
    }
    private void broadcastBlock(Block b) throws IOException, Exception {
        String str="Block,"+SerializeObject.serializeObject(b);
        byte[] toBeSent;
        toBeSent=str.getBytes();
        InetAddress ia=InetAddress.getByName(otherUser.IP);
        sentPacket=new DatagramPacket(toBeSent,toBeSent.length,ia,port);
        mySocket.send(sentPacket);  
        ia=InetAddress.getByName(InetAddress.getLocalHost().getHostAddress());
        sentPacket=new DatagramPacket(toBeSent,toBeSent.length,ia,port);
        mySocket.send(sentPacket);
    }
}
