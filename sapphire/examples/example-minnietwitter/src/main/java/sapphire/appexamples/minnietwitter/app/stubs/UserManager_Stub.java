/*
 * Stub for class sapphire.appexamples.minnietwitter.app.UserManager
 * Generated by Sapphire Compiler (sc).
 */
package sapphire.appexamples.minnietwitter.app.stubs;


public final class UserManager_Stub extends sapphire.appexamples.minnietwitter.app.UserManager implements sapphire.common.AppObjectStub {

    sapphire.policy.SapphirePolicy.SapphireClientPolicy $__client = null;
    boolean $__directInvocation = false;

    public UserManager_Stub (sapphire.appexamples.minnietwitter.app.TagManager $param_TagManager_1) {
        super($param_TagManager_1);
    }


    public void $__initialize(sapphire.policy.SapphirePolicy.SapphireClientPolicy client) {
        $__client = client;
    }

    public void $__initialize(boolean directInvocation) {
        $__directInvocation = directInvocation;
    }

    public sapphire.policy.SapphirePolicy.SapphireClientPolicy $__getSapphireClientPolicy() {
        return $__client;
    }

    public Object $__clone() throws CloneNotSupportedException {
        return super.clone();
    }



    // Implementation of login(String, String)
    public sapphire.appexamples.minnietwitter.app.User login(java.lang.String $param_String_1, java.lang.String $param_String_2) {
        java.lang.Object $__result = null;
        try {
            if ($__directInvocation)
                $__result = super.login( $param_String_1,  $param_String_2);
            else {
                java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
                String $__method = "public sapphire.appexamples.minnietwitter.app.User sapphire.appexamples.minnietwitter.app.UserManager.login(java.lang.String,java.lang.String)";
                $__params.add($param_String_1);
                $__params.add($param_String_2);
                $__result = $__client.onRPC($__method, $__params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ((sapphire.appexamples.minnietwitter.app.User) $__result);
    }

    // Implementation of getUser(String)
    public sapphire.appexamples.minnietwitter.app.User getUser(java.lang.String $param_String_1) {
        java.lang.Object $__result = null;
        try {
            if ($__directInvocation)
                $__result = super.getUser( $param_String_1);
            else {
                java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
                String $__method = "public sapphire.appexamples.minnietwitter.app.User sapphire.appexamples.minnietwitter.app.UserManager.getUser(java.lang.String)";
                $__params.add($param_String_1);
                $__result = $__client.onRPC($__method, $__params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ((sapphire.appexamples.minnietwitter.app.User) $__result);
    }

    // Implementation of dhtGetData()
    public java.util.Map dhtGetData() {
        java.lang.Object $__result = null;
        try {
            if ($__directInvocation)
                $__result = super.dhtGetData();
            else {
                java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
                String $__method = "public java.util.Map<sapphire.policy.interfaces.dht.DHTKey, ?> sapphire.appexamples.minnietwitter.app.UserManager.dhtGetData()";
                $__result = $__client.onRPC($__method, $__params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ((java.util.Map) $__result);
    }

    // Implementation of addUser(String, String)
    public sapphire.appexamples.minnietwitter.app.User addUser(java.lang.String $param_String_1, java.lang.String $param_String_2)
            throws sapphire.app.AppObjectNotCreatedException {
        java.lang.Object $__result = null;
        try {
            if ($__directInvocation)
                $__result = super.addUser( $param_String_1,  $param_String_2);
            else {
                java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
                String $__method = "public sapphire.appexamples.minnietwitter.app.User sapphire.appexamples.minnietwitter.app.UserManager.addUser(java.lang.String,java.lang.String) throws sapphire.app.AppObjectNotCreatedException";
                $__params.add($param_String_1);
                $__params.add($param_String_2);
                $__result = $__client.onRPC($__method, $__params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ((sapphire.appexamples.minnietwitter.app.User) $__result);
    }
}
