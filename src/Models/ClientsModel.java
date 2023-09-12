package Models;

import Assets.StringAssets;

public class ClientsModel {
    private String serverName;
    private String clientServer;
    private String clientType;
    private String clientId;


    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public ClientsModel(String clientId) {
        this.clientId = clientId;
        this.clientType = findClientType();
        this.clientServer = findClientServer();
    }

    private String findClientType() {
        return clientId.charAt(3)=='A'? StringAssets.ADMIN_USER:StringAssets.CUSTOMER_USER;
    }

    private String findClientServer() {
        String serverSubstring = clientId.substring(0,3);
        if(serverSubstring.equals("ATW")){
            return StringAssets.ATWATER_SERVER;
        }else if(serverSubstring.equals("VER")){
            return StringAssets.VERDUN_SERVER;
        }else{
            return StringAssets.OUTREMONT_SERVER;
        }
    }

    @Override
    public String toString() {
        return "Accessing: "+clientServer+" Server By Client:"+clientType+" with ID:"+clientId;
    }
}
