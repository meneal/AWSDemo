package base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;


/**
 * DemoRunner is a tool class in java that creates ec2 instances, lists
 * the running instances, and terminates ec2 instances.  Demo Runner 
 * generated as an executable .jar class to be run on the command line.
 * 
 * Use: 
 * java -jar DemoRunner.jar <options>
 * Options: -l list instances, -g generate key, -i new instance, -t terminate instances
 * 
 * On first use generate a key, then use the same key name to create an instance. 
 * Follow instructions to connect. 
 * Then when finished terminate instances.
 * 
 * Prerequisites:
 * Set up to compile within eclipse, must have Eclipse AWS SDK plugin.
 * 
 * Program assumes that a security group named bitcrusher already exists.
 * 
 * To set up the bitcrusher group in EC2 on the aws dashboard under Network & Security 
 * select "Security Groups", click "Create Security Group" set Security Group Name to "bitcrusher"
 * under the inbound tab select "Custom TCP Rule" under type, select "TCP" under Protocol, enter
 * "22" in the port range, and set source to "My IP" then click "Create".
 * 
 * Program assumes that credentials are set up as explained at this link:
 * http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html
 * 
 * Extension:
 * Adding a method to set up a new security group would be trivial and would
 * make the tool more portable.
 * 
 * @author Matthew Neal
 *
 */
public class DemoRunner {
	private static final Integer SHUTTING_DOWN = 32;
	private static final String IMAGE_ID = "ami-b5a7ea85";
	private static final int NUM_INSTANCES = 1;
	private static final String INSTANCE_TYPE = "t2.micro";
	private static final String GROUP = "bitcrusher";
	private static AmazonEC2 ec2 = null;
	private static String keyname = null;

	public static void main(String[] args) {
		AWSCredentials credentials = null;
	    try {
	        credentials = new ProfileCredentialsProvider("default").getCredentials();
	    } catch (Exception e) {
	        throw new AmazonClientException(
	                "Credentials could not be loaded.",
	                e);
	    }
	    
	    ec2 = new AmazonEC2Client(credentials);
	    ec2.setEndpoint("ec2.us-west-2.amazonaws.com");
	    
	    
	    if(args.length < 1){
	    	System.out.println("Usage: java -jar DemoRunner.jar <options>");
	    	System.out.println("Options: -l list instances, -g generate key, -i new instance, -t terminate instances, -s generate security group");
	    	System.exit(0);
	    }
	    if(args[0].equals("-g")){
	    	genKey();
	    }
	    if(args[0].equals("-i")){
	    	spawnInstance();
	    	try {
	    		System.out.println("Just a moment while the instance is spun up...");
				TimeUnit.SECONDS.sleep(30);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	String ip = listLiveInstances();
	    	System.out.println("");
	    	System.out.println("To ssh into server follow these steps:");
	    	System.out.println("Set permissions on pem file with 'chmod 400 " + keyname + ".pem'.");
	    	System.out.println("Then connect with 'ssh -i " + keyname + ".pem ec2-user@" + ip +"'");
	    }
	    
	    if(args[0].equals("-t")){
	    	terminateInstances();
	    }
	    if(args[0].equals("-l")){
	    	listLiveInstances();
	    }
	}
	
	private static String listLiveInstances(){
		DescribeInstancesResult descrInst = ec2.describeInstances();
		   
		List<Reservation> reservations = descrInst.getReservations();
		String ip = null;
		String ip2 = null;
		for(int i = 0; i < reservations.size(); i++){
		   Reservation cur = reservations.get(i);
		   String resId = cur.getReservationId();
		   List<Instance> instances = cur.getInstances();
		   for(int j = 0; j < instances.size(); j++){
			   Instance curInst = instances.get(j);
			   ip = curInst.getPublicIpAddress();
			   if(ip == null){
				   //Do nothing
			   }else{
				   System.out.println("Live ip on reservation id " + resId + " is: " + ip);
				   //Added so that only a non null ip will be returned
				   ip2 = ip;
			   }
		   }
		}
		return ip2;
	}
	
	private static void terminateInstances(){
	   DescribeInstancesResult descrInst = ec2.describeInstances();
	   List<Reservation> reservations = descrInst.getReservations();
	   for(int i = 0; i < reservations.size(); i++){
		   Reservation cur = reservations.get(i);
		   List<Instance> instances = cur.getInstances();
		   for(int j = 0; j < instances.size(); j++){
			   Instance curInst = instances.get(j);
			   String ip = curInst.getPublicIpAddress();
			   if(ip == null){
				   //Do nothing
			   }else{
				   String instId = curInst.getInstanceId();
				   System.out.println(ip + " about to be terminated!");
				   TerminateInstancesRequest term = new TerminateInstancesRequest();
				   term.withInstanceIds(instId);
				   TerminateInstancesResult termRes = ec2.terminateInstances(term);
				   List<InstanceStateChange> stateList = termRes.getTerminatingInstances();
				   //Hardcoded to zero since there should only be one instance
				   if(stateList.get(0).getCurrentState().getCode() == SHUTTING_DOWN){
					   System.out.println("Terminated");
				   }
			   }
		   }
	   }
	}
	
	/**
	 * Creates a new instance 
	 */
	private static void spawnInstance(){
		System.out.println("Name of key to use?");
		Scanner in = new Scanner(System.in);
		keyname = in.next();
		in.close();
		
		RunInstancesRequest req = new RunInstancesRequest();

		int numInstances = NUM_INSTANCES;
	    String imageId = IMAGE_ID;
	    String instanceType = INSTANCE_TYPE;
	    String group = GROUP;
	        
	    req.withImageId(imageId)
	       .withInstanceType(instanceType)
	       .withMinCount(numInstances)
	       .withMaxCount(numInstances)
	       .withKeyName(keyname)
	       .withSecurityGroups(group);
	        
	   RunInstancesResult reqRes = ec2.runInstances(req);
	   Reservation res = reqRes.getReservation();
	   System.out.println("Reservation number is " + res.getReservationId());
	}
	
	
	/**
	 * The genKey method creates a pem file in the directory that the 
	 * program is executed in, and generates a key on amazon's servers.
	 * 
	 */
	private static void genKey(){
		System.out.println("Name to use for key?");
		Scanner in = new Scanner(System.in);
		keyname = in.next();
		in.close();
		
		CreateKeyPairRequest createKPReq = new CreateKeyPairRequest();
		createKPReq.withKeyName(keyname);
		CreateKeyPairResult resultPair = ec2.createKeyPair(createKPReq);
		KeyPair keyPair = new KeyPair();
		keyPair = resultPair.getKeyPair();
		String privateKey = keyPair.getKeyMaterial();
		FileOutputStream out = null;
		
		
		try{
			File f = new File(keyname + ".pem");
			out = new FileOutputStream(f);
			byte[] privateKeyByte = privateKey.getBytes();
			out.write(privateKeyByte);
			out.flush();
			out.close();
			
			
		}catch (IOException e){
			System.out.println("IO failed!");
		
		}finally{
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {
					System.out.println("IO failed!");
				}
			}
		}
	
		System.out.println("Key generated: " + keyname + ".pem");
	}
}
