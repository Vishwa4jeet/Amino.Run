package amino.run.policy;

import amino.run.app.Language;
import amino.run.app.NodeSelectorSpec;
import amino.run.app.SapphireObjectSpec;
import amino.run.common.AppObject;
import amino.run.common.AppObjectStub;
import amino.run.common.GraalObject;
import amino.run.common.SapphireObjectID;
import amino.run.common.SapphireObjectNotFoundException;
import amino.run.common.SapphireObjectReplicaNotFoundException;
import amino.run.common.SapphireReplicaID;
import amino.run.common.Utils;
import amino.run.compiler.GlobalStubConstants;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectFactory;
import amino.run.kernel.common.KernelObjectNotCreatedException;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.OMSServer;
import amino.run.policy.Policy.ServerPolicy;
import amino.run.runtime.Sapphire;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.harmony.rmi.common.RMIUtil;

public abstract class Library implements SapphirePolicyUpcalls {
    public abstract static class SapphireClientPolicyLibrary
            implements SapphireClientPolicyUpcalls {

        /*
         * INTERNAL FUNCTIONS (Used by sapphire runtime system)
         */
    }

    public abstract static class SapphireServerPolicyLibrary
            implements SapphireServerPolicyUpcalls {
        protected AppObject appObject;
        protected KernelOID oid;
        protected SapphireReplicaID replicaId;
        protected Policy.GroupPolicy group;
        protected SapphireObjectSpec spec;
        protected Map<String, SapphirePolicyConfig> configMap;
        protected boolean alreadyPinned;

        static Logger logger = Logger.getLogger(SapphireServerPolicyLibrary.class.getName());

        // ServerPolicy that precedes the current policy in the server side chain - this order is
        // reverse in the client side.
        protected Policy.ServerPolicy previousServerPolicy;

        // List of ServerPolicies that should be created in the chain after the current one when
        // creating replicas.
        // These nested part of chain where the last one created will be called by KernelServer
        // (farthest from actual app object).
        // It means these were the last in order in the client side of chain. New groups should be
        // created for this list of chain.
        protected List<SapphirePolicyContainer> nextPolicies =
                new ArrayList<SapphirePolicyContainer>();

        // List of ServerPolicies that were created previously. They are upper level in group
        // hierarchy. Therefore, this list of chain
        // should not create new group policies. When creating replicas, group stub information
        // stored in this chain will be copied over
        // to the new replicas so that they can reference the same group stubs.
        protected List<SapphirePolicyContainer> processedPolicies =
                new ArrayList<SapphirePolicyContainer>();

        private OMSServer oms() {
            return GlobalKernelReferences.nodeServer.oms;
        }

        private KernelServerImpl kernel() {
            return GlobalKernelReferences.nodeServer;
        }

        /*
         * SAPPHIRE API FOR SERVER POLICIES
         */
        public List<SapphirePolicyContainer> getProcessedPolicies() {
            return this.processedPolicies;
        }

        public ServerPolicy getPreviousServerPolicy() {
            return this.previousServerPolicy;
        }

        public void setPreviousServerPolicy(Policy.ServerPolicy serverPolicy) {
            this.previousServerPolicy = serverPolicy;
        }

        public void setNextPolicies(List<SapphirePolicyContainer> nextPolicies) {
            this.nextPolicies = nextPolicies;
        }

        public void setProcessedPolicies(List<SapphirePolicyContainer> processedPolicies) {
            this.processedPolicies = processedPolicies;
        }

        public void setSapphireObjectSpec(SapphireObjectSpec spec) {
            this.spec = spec;
        }

        public SapphireObjectSpec getSapphireObjectSpec() {
            return this.spec;
        }

        @Override
        public void onCreate(Policy.GroupPolicy group, SapphireObjectSpec spec) {
            this.group = group;
            this.spec = spec;
            if (spec != null && spec.getDmList() != null) {
                this.configMap = Utils.fromDMSpecListToFlatConfigMap(spec.getDmList());
            }
        }

        /** Creates a replica of this server and registers it with the group. */
        public ServerPolicy sapphire_replicate(
                List<SapphirePolicyContainer> processedPolicies, String region)
                throws RemoteException {
            KernelObjectStub serverPolicyStub = null;

            // Construct list of policies that will come after this policy on the server side.
            try {
                // Create a new replica chain from already created policies before this policy and
                // this policy.
                List<SapphirePolicyContainer> processedPoliciesReplica =
                        new ArrayList<SapphirePolicyContainer>();
                Sapphire.createPolicy(
                        this.getGroup().sapphireObjId,
                        spec,
                        processedPolicies,
                        processedPoliciesReplica,
                        region,
                        null);

                // Last policy in the returned chain is replica of this policy.
                serverPolicyStub =
                        processedPoliciesReplica
                                .get(processedPoliciesReplica.size() - 1)
                                .getServerPolicyStub();

                // Complete the chain by creating new instances of server policies and stub that
                // should be created after this policy.
                Sapphire.createPolicy(
                        this.getGroup().sapphireObjId,
                        spec,
                        this.nextPolicies,
                        processedPoliciesReplica,
                        region,
                        null);

                getGroup().addServer((Policy.ServerPolicy) serverPolicyStub);
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                logger.severe(e.getMessage());
                throw new Error("Could not find the class for replication!", e);
            } catch (KernelObjectNotCreatedException e) {
                // TODO Auto-generated catch block
                logger.severe(e.getMessage());
                throw new Error("Could not create a replica!", e);
            } catch (KernelObjectNotFoundException e) {
                logger.severe(e.getMessage());
                throw new Error("Could not find object to replicate!", e);
            } catch (SapphireObjectNotFoundException e) {
                KernelObjectFactory.delete(serverPolicyStub.$__getKernelOID());
                logger.severe(e.getMessage());
                throw new Error("Could not find sapphire object on OMS", e);
            } catch (SapphireObjectReplicaNotFoundException e) {
                KernelObjectFactory.delete(serverPolicyStub.$__getKernelOID());
                logger.severe(e.getMessage());
                throw new Error("Could not find sapphire object replica on OMS", e);
            } catch (RemoteException e) {
                sapphire_remove_replica(processedPolicies);
                logger.severe(e.getMessage());
                throw new Error("Could not create a replica of " + appObject.getObject(), e);
            } catch (Exception e) {
                logger.severe(e.getMessage());
                throw new Error("Unknown exception occurred!", e);
            }

            return (ServerPolicy) serverPolicyStub;
        }

        public AppObject sapphire_getAppObject() {
            return appObject;
        }

        /**
         * Pin server policy chain to a given server. 1) Checks if there is server policy to pin to
         * the new host. 2) Obtain the first server policy (farthest from app object) by moving the
         * pointer in the chain. 3) Navigate through the chain to find all server policy information
         * that need to be removed after move. 4) Copy the chain of server policy to the new host.
         * 5) Remove the server policies in the local chain that were moved.
         *
         * @param serverPolicyStub
         * @param server
         * @throws RemoteException
         * @throws SapphireObjectNotFoundException
         * @throws SapphireObjectReplicaNotFoundException
         */
        public void sapphire_pin_to_server(ServerPolicy serverPolicyStub, InetSocketAddress server)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            KernelOID serverOID = serverPolicyStub.$__getKernelOID();
            Policy.ServerPolicy serverPolicy;
            try {
                serverPolicy =
                        (Policy.ServerPolicy)
                                GlobalKernelReferences.nodeServer.getObject(serverOID);
            } catch (Exception e) {
                logger.severe(e.getMessage());
                throw new RemoteException("No server policy to pin to the server: " + server, e);
            }

            // Ensure that we start from the first Server Policy.
            while (serverPolicy.getPreviousServerPolicy() != null) {
                serverPolicy = serverPolicy.getPreviousServerPolicy();
            }

            // Before pinning the Sapphire Object replica to the provided KernelServer, need to
            // update the Hostname.
            List<SapphirePolicyContainer> processedPolicyList = serverPolicy.getProcessedPolicies();
            Iterator<SapphirePolicyContainer> itr = processedPolicyList.iterator();
            while (itr.hasNext()) {
                SapphirePolicyContainer container = itr.next();
                ServerPolicy tempServerPolicy = container.getServerPolicy();
                container.getServerPolicyStub().$__updateHostname(server);
                /* AppObject holds the previous DM's server policy stub(instead of So stub) in case of DM chain on the
                server side. Update host name in the server stub within AppObject */
                if (tempServerPolicy.sapphire_getAppObject().getObject()
                        instanceof KernelObjectStub) {
                    ((KernelObjectStub) tempServerPolicy.sapphire_getAppObject().getObject())
                            .$__updateHostname(server);
                }
            }

            logger.info(
                    "(Starting) Pinning Sapphire object "
                            + serverPolicyStub
                            + " "
                            + serverPolicy.$__getKernelOID()
                            + " to "
                            + server);
            try {
                kernel().moveKernelObjectToServer(serverPolicy, server);
            } catch (KernelObjectNotFoundException e) {
                String msg = "Could not find myself on this server!";
                logger.severe(msg);
                throw new Error(msg, e);
            } catch (SapphireObjectNotFoundException e) {
                String msg = "Could not find Sapphire object on this server!";
                logger.severe(msg);
                throw new Error(msg, e);
            } catch (SapphireObjectReplicaNotFoundException e) {
                String msg = "Could not find Sapphire replica on this server!";
                logger.severe(msg);
                throw new Error(msg, e);
            }

            serverPolicy.setAlreadyPinned(true);

            logger.info(
                    "(Complete) Pinning Sapphire object "
                            + serverPolicyStub
                            + " "
                            + serverPolicy.$__getKernelOID()
                            + " to "
                            + server);
        }

        // TODO (2018-9-26, Sungwook) Remove after verification.
        public void sapphire_remove_replica() throws RemoteException {
            try {
                GlobalKernelReferences.nodeServer.oms.unRegisterSapphireReplica(getReplicaId());
            } catch (SapphireObjectNotFoundException e) {
                /* Sapphire object not found */
                logger.severe(e.getMessage());
                // TODO (Sungwook, 2018-10-2): Investigate whether exception should be thrown.
            }
            KernelObjectFactory.delete($__getKernelOID());
        }

        public void sapphire_remove_replica(List<SapphirePolicyContainer> processedPolicies)
                throws RemoteException {
            try {
                for (SapphirePolicyContainer policyContainer : processedPolicies) {
                    ServerPolicy sp = policyContainer.getServerPolicy();
                    oms().unRegisterSapphireReplica(sp.getReplicaId());
                }
            } catch (SapphireObjectNotFoundException e) {
                /* Sapphire object not found */
                logger.severe(e.getMessage());
            }
            KernelObjectFactory.delete($__getKernelOID());
        }

        /**
         * Internal function used to initialize the App Object
         *
         * @param spec
         * @param params
         */
        // TODO: not final (stub overrides it)
        public AppObjectStub $__initialize(SapphireObjectSpec spec, Object[] params) {
            logger.info(String.format("Creating app object '%s' with parameters %s", spec, params));

            AppObjectStub actualAppObject = null;
            try {
                if (spec.getLang() == Language.java) {
                    Class<?> appObjectClass = Class.forName(spec.getJavaClassName());
                    String appStubClassName =
                            GlobalStubConstants.getAppPackageName(
                                            RMIUtil.getPackageName(appObjectClass))
                                    + "."
                                    + RMIUtil.getShortName(appObjectClass)
                                    + GlobalStubConstants.STUB_SUFFIX;

                    Class<?> appObjectStubClass = Class.forName(appStubClassName);
                    // Construct the list of classes of the arguments as Class[]
                    if (params != null) {
                        Class<?>[] argClasses = Sapphire.getParamsClasses(params);
                        actualAppObject =
                                (AppObjectStub)
                                        appObjectStubClass
                                                .getConstructor(argClasses)
                                                .newInstance(params);

                    } else {
                        actualAppObject = (AppObjectStub) appObjectStubClass.newInstance();
                    }

                    actualAppObject.$__initialize(true);
                    appObject = new AppObject(actualAppObject);
                } else {
                    String stubClassName = spec.getJavaClassName();
                    if (stubClassName.isEmpty()) {
                        throw new RuntimeException("stub class name missing for application");
                    }

                    Class<?> appObjectStubClass = Class.forName(spec.getJavaClassName());
                    // Construct the list of classes of the arguments as Class[]
                    // TODO: Currently all polyglot application stub should have default
                    // constructor. Fix it
                    Object appStubObject = appObjectStubClass.newInstance();
                    ((GraalObject) appStubObject).$__initializeGraal(spec, params);
                    actualAppObject = (AppObjectStub) appStubObject;
                    actualAppObject.$__initialize(true);

                    appObject = new AppObject(actualAppObject);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to initialize server policy", e);
            }
            return actualAppObject;
        }

        public String sapphire_getRegion() {
            return kernel().getRegion();
        }

        public void $__initialize(AppObject appObject) {
            this.appObject = appObject;
        }

        public void $__setKernelOID(KernelOID oid) {
            this.oid = oid;
        }

        public KernelOID $__getKernelOID() {
            return oid;
        }

        public void setReplicaId(SapphireReplicaID rid) {
            replicaId = rid;
        }

        public SapphireReplicaID getReplicaId() {
            return replicaId;
        }

        public InetSocketAddress sapphire_locate_kernel_object(KernelOID oid)
                throws RemoteException {
            InetSocketAddress addr;
            try {
                addr = oms().lookupKernelObject(oid);
            } catch (RemoteException e) {
                throw new RemoteException("Could not contact oms.");
            } catch (KernelObjectNotFoundException e) {
                e.printStackTrace();
                throw new Error("Could not find myself on this server!");
            }
            return addr;
        }

        public boolean isAlreadyPinned() {
            return this.alreadyPinned;
        }

        public void setAlreadyPinned(boolean alreadyPinned) {
            this.alreadyPinned = alreadyPinned;
        }
    }

    public abstract static class SapphireGroupPolicyLibrary implements SapphireGroupPolicyUpcalls {
        protected String appObjectClassName;
        protected ArrayList<Object> params;
        protected KernelOID oid;
        protected SapphireObjectID sapphireObjId;

        static Logger logger = Logger.getLogger(SapphireGroupPolicyLibrary.class.getName());

        protected OMSServer oms() {
            return GlobalKernelReferences.nodeServer.oms;
        }

        /*
         * SAPPHIRE API FOR GROUP POLICIES
         */

        public ArrayList<String> sapphire_getRegions() throws RemoteException {
            return oms().getRegions();
        }

        /**
         * Gets the list of servers in from nodeSelector or region.
         *
         * @param nodeSelector
         * @param region
         * @return list of server addresses
         * @throws RemoteException
         */
        // TODO: Remove region parameter after spec is applied to all DMs and scripts.
        public List<InetSocketAddress> sapphire_getAddressList(
                NodeSelectorSpec nodeSelector, String region) throws RemoteException {
            List<InetSocketAddress> serversInRegion;

            if (null != nodeSelector) { // spec takes priority over region
                serversInRegion = oms().getServers(nodeSelector);
            } else {
                if (region != null && !region.isEmpty()) {
                    nodeSelector = new NodeSelectorSpec();
                    nodeSelector.addAndLabel(region);
                    serversInRegion = oms().getServers(nodeSelector);
                } else {
                    serversInRegion = oms().getServers(null);
                }
            }
            return serversInRegion;
        }

        public void $__setKernelOID(KernelOID oid) {
            this.oid = oid;
        }

        public KernelOID $__getKernelOID() {
            return this.oid;
        }

        public void setSapphireObjId(SapphireObjectID sapphireId) {
            sapphireObjId = sapphireId;
        }

        public SapphireObjectID getSapphireObjId() {
            return sapphireObjId;
        }

        /**
         * Creates a replica based on replicaSource and pin to the dest host if not pinned before.
         *
         * @param replicaSource server policy where the replica creation operation will be
         *     performed.
         * @param dest address for KernelServer that will host this replica.
         * @param region region where this replica needs to be located within.
         * @param pinned whether the policy chain was already pinned by downstream policy.
         * @return newly created replica.
         * @throws RemoteException
         * @throws SapphireObjectNotFoundException
         * @throws SapphireObjectReplicaNotFoundException
         */
        protected ServerPolicy addReplica(
                Policy.ServerPolicy replicaSource,
                InetSocketAddress dest,
                String region,
                boolean pinned)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            ServerPolicy replica =
                    replicaSource.sapphire_replicate(replicaSource.getProcessedPolicies(), region);
            if (pinned) {
                // This chain was already pinned by the downstream policy; hence, skips pinning.
                return replica;
            }

            try {
                replicaSource.sapphire_pin_to_server(replica, dest);
                updateReplicaHostName(replica, dest);
            } catch (Exception e) {
                String msgDetail =
                        String.format(
                                "Region:%s Dest:%s ReplicaSrc:%s", region, dest, replicaSource);
                logger.log(Level.SEVERE, "Replica pinning failed. " + msgDetail, e);
                try {
                    removeReplica(replica);
                } catch (Exception innerException) {
                    logger.log(
                            Level.WARNING,
                            "Replica removal failed after pinning has failed. " + msgDetail,
                            e);
                }
                throw e;
            }
            return replica;
        }

        protected void removeReplica(Policy.ServerPolicy server)
                throws RemoteException, SapphireObjectReplicaNotFoundException,
                        SapphireObjectNotFoundException {
            server.sapphire_remove_replica();
            removeServer(server);
        }

        protected void updateReplicaHostName(ServerPolicy serverPolicy, InetSocketAddress host)
                throws RemoteException {
            ArrayList<ServerPolicy> servers = getServers();
            if (servers == null) {
                return;
            }

            for (Iterator<ServerPolicy> itr = servers.iterator(); itr.hasNext(); ) {
                ServerPolicy server = itr.next();
                if (server.$__getKernelOID().equals(serverPolicy.$__getKernelOID())) {
                    ((KernelObjectStub) server).$__updateHostname(host);
                    ((KernelObjectStub) serverPolicy).$__updateHostname(host);
                    break;
                }
            }
        }

        /**
         * Notifies server policies to exit. Each server policy should do three tasks: 1) remove
         * itself from {@code KernelObjectManager} on local kernel server, 2) remove itself of OMS's
         * {@code KernelObjectManager}, and 3) remove replica ID from OMS.
         *
         * <p><strong>Warning:</strong> Do not try to call OMS to unregister the sapphire object.
         * {@link OMSServer#deleteSapphireObject(SapphireObjectID)} is the public entry point to
         * delete a sapphire object. OMS will take care of deleting sapphire object at {@link
         * amino.run.oms.OMSServerImpl#deleteSapphireObject(SapphireObjectID)}.
         *
         * @throws RemoteException
         */
        public void onDestroy() throws RemoteException {
            /* Delete all the servers */
            ArrayList<ServerPolicy> servers = getServers();
            if (servers == null) {
                return;
            }

            for (Iterator<ServerPolicy> itr = servers.iterator(); itr.hasNext(); ) {
                Policy.ServerPolicy server = itr.next();
                try {
                    server.sapphire_remove_replica();
                    itr.remove();
                } catch (Exception e) {

                }
            }

            KernelObjectFactory.delete($__getKernelOID());
        }
    }
}