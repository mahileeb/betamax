package co.freeside.betamax.util

class Network {

    static Collection<String> getLocalAddresses() {
        try {
            def local = InetAddress.localHost
            [local.hostName, local.hostAddress, 'localhost', '127.0.0.1']
        } catch (Throwable t) {
            return ['localhost', '127.0.0.1']
        }
    }

    private Network() {}

}
