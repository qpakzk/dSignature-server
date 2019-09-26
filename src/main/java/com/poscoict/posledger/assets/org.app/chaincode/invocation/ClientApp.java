/*package com.poscoict.posledger.assets.org.app.chaincode.invocation;

import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Wallet;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class ClientApp {

    public static void main(String[] args) throws Exception {
        // Load a file system based wallet for managing identities.
        Path walletPath = Paths.get("/home/yoongdoo0819/dSignature-server/wallet");
        Wallet wallet = Wallet.createFileSystemWallet(walletPath);
        System.out.println(walletPath.getRoot() + " " + walletPath.getFileName() + " " + walletPath.subpath(0, walletPath.getNameCount()) + " " + walletPath.toString());
        System.out.println(wallet.toString());
        // load a CCP
        Path networkConfigPath = Paths.get("/home/yoongdoo0819/fabric-samples/first-network/connection-org1.yaml");

        Gateway.Builder builder = Gateway.createBuilder();
        builder.identity(wallet, "user1").networkConfig(networkConfigPath).discovery(true);
        System.out.println((builder.connect()).getIdentity().toString());
        // create a gateway connection
        System.out.println("------------");
        System.out.println(builder.connect().getNetwork("ll") + "*****************************");
        try (Gateway gateway = builder.connect()) {
            System.out.println(gateway.getNetwork("mychannel"));
            // get the network and contract
            Network network = gateway.getNetwork("mychannel");
            Contract contract = network.getContract("fabcar");

            byte[] result;

            result = contract.evaluateTransaction("queryAllCars");
            System.out.println(new String(result));

            contract.submitTransaction("createCar", "CAR10", "VW", "Polo", "Grey", "Mary");

            result = contract.evaluateTransaction("queryCar", "CAR10");
            System.out.println(new String(result));

            contract.submitTransaction("changeCarOwner", "CAR10", "Archie");

            result = contract.evaluateTransaction("queryCar", "CAR10");
            System.out.println(new String(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
*/
package com.poscoict.posledger.assets.org.app.chaincode.invocation;

import com.poscoict.posledger.assets.org.app.client.CAClient;
import com.poscoict.posledger.assets.org.app.client.ChannelClient;
import com.poscoict.posledger.assets.org.app.client.FabricClient;
import com.poscoict.posledger.assets.org.app.config.Config;
import com.poscoict.posledger.assets.org.app.user.UserContext;
import com.poscoict.posledger.assets.org.app.util.Util;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hyperledger.fabric.sdk.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ClientApp {

    private static final byte[] EXPECTED_EVENT_DATA = "!".getBytes(UTF_8);
    private static final String EXPECTED_EVENT_NAME = "event";

    public String mint(int tokenId, String owner, String docId, String signers, String path, String pathHash) {

        try {
            Util.cleanUp();
            String caUrl = Config.CA_ORG1_URL;
            CAClient caClient = new CAClient(caUrl, null);
            // Enroll Admin to Org1MSP
            UserContext adminUserContext = new UserContext();
  /*          adminUserContext.setName(Config.ADMIN);
            adminUserContext.setAffiliation(Config.ORG1);
            adminUserContext.setMspId(Config.ORG1_MSP);
            caClient.setAdminUserContext(adminUserContext);
            adminUserContext = caClient.enrollAdminUser(Config.ADMIN, Config.ADMIN_PASSWORD);
*/
            adminUserContext.setName(Config.ADMIN);
            adminUserContext.setAffiliation(Config.ORG1);
            adminUserContext.setMspId(Config.ORG1_MSP);
            caClient.setAdminUserContext(adminUserContext);
            adminUserContext = caClient.enrollAdminUser(Config.ADMIN, Config.ADMIN_PASSWORD);

            FabricClient fabClient = new FabricClient(adminUserContext);

            ChannelClient channelClient = fabClient.createChannelClient(Config.CHANNEL_NAME);
            Channel channel = channelClient.getChannel();
            Peer peer = fabClient.getInstance().newPeer(Config.ORG1_PEER_0, Config.ORG1_PEER_0_URL);
            EventHub eventHub = fabClient.getInstance().newEventHub("eventhub01", "grpc://localhost:7053");
            Orderer orderer = fabClient.getInstance().newOrderer(Config.ORDERER_NAME, Config.ORDERER_URL);
            channel.addPeer(peer);
            channel.addEventHub(eventHub);
            channel.addOrderer(orderer);
            channel.initialize();

            TransactionProposalRequest request = fabClient.getInstance().newTransactionProposalRequest();
            ChaincodeID ccid = ChaincodeID.newBuilder().setName(Config.CHAINCODE_1_NAME).build();
            request.setChaincodeID(ccid);
            request.setFcn("mint");
            //String[] arguments = { valueOf(tokenId), "doc", owner, docId, signers, path, pathHash};
            String[] arguments = { "10", "sig", "alice", "sigId", "path", "pathHash" };
            /*
            String signers = "";
            for(int i=0; i<party.length; i++) {
                    signers += party[i];

                    if(i+1 < party.length)
                        signers += ",";
            }*/
/*
            JSONObject jsonObjectForXatt = new JSONObject();
            jsonObjectForXatt.put("hash", docId);
            jsonObjectForXatt.put("signers", signers);
            jsonObjectForXatt.put("signatures", "");

            JSONObject jsonObjectForUri = new JSONObject();
            jsonObjectForUri.put("path", path);
            jsonObjectForUri.put("hash", "");

            String[] arguments = { valueOf(tokenId), "doc", owner, jsonObjectForXatt.toString(), jsonObjectForUri.toString() };

 */
            //request.setFcn("createCar");
            //String[] arguments = { "CAR1", "Chevy", "Volt", "Red", "Nick" };
            request.setArgs(arguments);
            request.setProposalWaitTime(1000);

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
            tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
            tm2.put("result", ":)".getBytes(UTF_8));
            tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA);
            request.setTransientMap(tm2);
            Collection<ProposalResponse> responses = channelClient.sendTransactionProposal(request);
            for (ProposalResponse res: responses) {
                ChaincodeResponse.Status status = res.getStatus();
                String message = res.getMessage();
                Logger.getLogger(InvokeChaincode.class.getName()).log(Level.INFO,"Invoked createCar on "+Config.CHAINCODE_1_NAME + ". Status - " + status + " Message -" + message);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static void main(String args[]) {
        ClientApp a = new ClientApp();
        a.mint(0, "owner", "docid", "signers", "path", "pathhash");
    /*
        try {
            Util.cleanUp();
            String caUrl = Config.CA_ORG1_URL;
            CAClient caClient = new CAClient(caUrl, null);
            // Enroll Admin to Org1MSP
            UserContext adminUserContext = new UserContext();
            adminUserContext.setName(Config.ADMIN);
            adminUserContext.setAffiliation(Config.ORG1);
            adminUserContext.setMspId(Config.ORG1_MSP);
            caClient.setAdminUserContext(adminUserContext);
            adminUserContext = caClient.enrollAdminUser(Config.ADMIN, Config.ADMIN_PASSWORD);

            FabricClient fabClient = new FabricClient(adminUserContext);

            ChannelClient channelClient = fabClient.createChannelClient(Config.CHANNEL_NAME);
            Channel channel = channelClient.getChannel();
            Peer peer = fabClient.getInstance().newPeer(Config.ORG1_PEER_0, Config.ORG1_PEER_0_URL);
            EventHub eventHub = fabClient.getInstance().newEventHub("eventhub01", "grpc://localhost:7053");
            Orderer orderer = fabClient.getInstance().newOrderer(Config.ORDERER_NAME, Config.ORDERER_URL);
            channel.addPeer(peer);
            channel.addEventHub(eventHub);
            channel.addOrderer(orderer);
            channel.initialize();

            TransactionProposalRequest request = fabClient.getInstance().newTransactionProposalRequest();
            ChaincodeID ccid = ChaincodeID.newBuilder().setName(Config.CHAINCODE_1_NAME).build();
            request.setChaincodeID(ccid);
            request.setFcn("mint");
            //String[] arguments = { "token", "woori", "100", "yoongdoo", "yoongdoo", "sig" };

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("hash", "456");

            String[] arguments = { "2", "sig", "Alice", jsonObject.toString(), "" };
            //request.setFcn("createCar");
            //String[] arguments = { "CAR1", "Chevy", "Volt", "Red", "Nick" };
            request.setArgs(arguments);
            request.setProposalWaitTime(1000);

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
            tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
            tm2.put("result", ":)".getBytes(UTF_8));
            tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA);
            request.setTransientMap(tm2);
            Collection<ProposalResponse> responses = channelClient.sendTransactionProposal(request);
            for (ProposalResponse res: responses) {
                ChaincodeResponse.Status status = res.getStatus();
                String message = res.getMessage();
                Logger.getLogger(InvokeChaincode.class.getName()).log(Level.INFO,"Invoked createCar on "+Config.CHAINCODE_1_NAME + ". Status - " + status + " Message -" + message);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

}