package amino.run.appexamples.fundmover;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.SapphireObjectServer;
import amino.run.app.SapphireObjectSpec;
import amino.run.common.SapphireObjectID;
import amino.run.kernel.server.KernelServer;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.policy.transaction.TwoPCCoordinatorPolicy;
import amino.run.policy.transaction.TwoPCExtResourceCohortPolicy;

public class FundmoverMain {
    public static void main(String[] args) throws RemoteException {

        if (args.length < 4) {
            System.out.println("Incorrect arguments to the program");
            System.out.println("usage: " +FundmoverMain.class.getSimpleName() + " <host-ip> <host-port> <oms ip> <oms-port>");
            System.exit(1);
        }
        String hostIp = args[0], hostPort = args[1], omsIp = args[2], omsPort = args[3];
        InetSocketAddress hostAddr = new InetSocketAddress(hostIp, Integer.parseInt(hostPort)), omsAddr = new InetSocketAddress(omsIp, Integer.parseInt(omsPort));

        Registry registry;
        try{
            registry = LocateRegistry.getRegistry(args[0],Integer.parseInt(args[1]));
            SapphireObjectServer server = (SapphireObjectServer) registry.lookup("SapphireOMS");
            System.out.println(server);

            KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(args[2], Integer.parseInt(args[3])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));

            SapphireObjectSpec walletSpec,bankAccountSpec,financeSpec ;
            walletSpec= SapphireObjectSpec.newBuilder()
                         .setLang(Language.java)
                         .setJavaClassName(Wallet.class.getName())
                         .addDMSpec(
                                 DMSpec.newBuilder()
                                 .setName(TwoPCExtResourceCohortPolicy.class.getName())
                                 .create())
                         .create();
            bankAccountSpec = SapphireObjectSpec.newBuilder()
                               .setLang(Language.java)
                               .setJavaClassName(BankAccount.class.getName())
                               .addDMSpec(
                                       DMSpec.newBuilder()
                                       .setName(TwoPCExtResourceCohortPolicy.class.getName())
                                       .create())
                               .create();
            financeSpec = SapphireObjectSpec.newBuilder()
                           .setLang(Language.java)
                           .setJavaClassName(Finance.class.getName())
                           .addDMSpec(
                                   DMSpec.newBuilder()
                                   .setName(TwoPCCoordinatorPolicy.class.getName())
                                   .create())
                           .create();

            SapphireObjectID walletSapphireObjectID = server.createSapphireObject(walletSpec.toString());

            Wallet wallet = (Wallet)server.acquireSapphireObjectStub(walletSapphireObjectID);;

            wallet.credit(100);

            SapphireObjectID bankAccountSapphireObjectID = server.createSapphireObject(bankAccountSpec.toString());

            BankAccount bankaccount = (BankAccount)server.acquireSapphireObjectStub(bankAccountSapphireObjectID);

            System.out.println("creating the finance object...");

            SapphireObjectID financeSapphireObjectID = server.createSapphireObject(financeSpec.toString(),wallet,bankaccount);

            Finance finance = (Finance) server.acquireSapphireObjectStub(financeSapphireObjectID);

            System.out.println("transfering fund between 2 entities...");

            finance.transferFromWallet(40);
            System.out.println("checking the current balance after the transaction...");
            System.out.printf("finance details: %s\r\n", finance.getDetails());

            finance.transferFromBank(20);
            System.out.println("checking the current balance after the transaction...");
            System.out.printf("finance details: %s\r\n", finance.getDetails());
            // Verifying the rollback use case
            finance.transferFromWallet(85);

        }catch (Exception e) {
            System.out.println("---------- error occurred -----");
            System.out.println(e.toString());

        }


    }
}